/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import communication.BrokerIB;
import communication.IBroker;
import data.StockIndicatorsForNinety;
import data.TickersToTrade;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import tradingapp.CheckingThread;
import tradingapp.TradeFormatter;
import tradingapp.MailSender;
import tradingapp.Settings;
import tradingapp.TradeLogger;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class NinetyScheduler {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final static LocalTime FIRST_CHECK_TIME = LocalTime.of(10, 0);
    private final static Duration DURATION_BEFORECLOSE_LASTCALL = Duration.ofMinutes(3);
    private final static Duration DURATION_BEFORECLOSE_RUNSTRATEGY = Duration.ofMinutes(1);

    public StockDataForNinety stockData = new StockDataForNinety();
    public StatusDataForNinety statusData = new StatusDataForNinety();

    public final IBroker broker;
    public boolean isStartScheduled = false;
    
    public final Semaphore dataMutex = new Semaphore(1);

    public NinetyScheduler(IBroker broker) {
        statusData.LoadTradingStatus();
        statusData.PrintStatus();
        
        this.broker = broker;
    }
    
    public void NewDayInit() {
        TradeLogger.getInstance().clearLogs(); 
        TradeLogger.getInstance().initializeFiles(LocalDate.now());
        TradeTimer.LoadSpecialTradingDays();
        TickersToTrade.LoadTickers();

        Settings.ReadSettings();

        statusData.UpdateCashSettings();
        statusData.LoadTradingStatus();
        statusData.PrintStatus();
        
        stockData.ClearData();
    }

    public void RunNow() {
        NewDayInit();
        
        isStartScheduled = true;
        
        ScheduleLoadingHistData(TradeTimer.GetNYTimeNow().plusSeconds(2));
        ScheduleTradingRun(TradeTimer.GetNYTimeNow().plusSeconds(20));
    }

    private void ScheduleForTomorrow() {
        logger.info("Scheduling for tomorrow!");
        isStartScheduled = true;
        ZonedDateTime tomorrowCheck = TradeTimer.GetNYTimeNow().plusDays(1).with(FIRST_CHECK_TIME);
        TradeTimer.startTaskAt(tomorrowCheck, this::PrepareForTrading);

        Duration durationToNextRun = Duration.ofSeconds(TradeTimer.computeTimeFromNowTo(tomorrowCheck));
        
        logger.info("Next check is scheduled for " + tomorrowCheck.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + durationToNextRun.toString());

        MailSender.SendErrors();
    }

    public void ScheduleFirstCheck() {
        ZonedDateTime earliestCheckTime = TradeTimer.GetNYTimeNow().with(FIRST_CHECK_TIME);
        
        ZonedDateTime checkTime = TradeTimer.GetNYTimeNow().plusSeconds(1);

        if (checkTime.compareTo(earliestCheckTime) <= 0) {
            checkTime = earliestCheckTime;
        
            Duration durationToNextRun = Duration.ofSeconds(TradeTimer.computeTimeFromNowTo(checkTime));

            logger.info("First check is scheduled for " + checkTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            logger.info("Starting in " + durationToNextRun.toString());
        }

        TradeTimer.startTaskAt(checkTime, this::PrepareForTrading);
    }

    public void PrepareForTrading() {
        boolean isCheckOk = true;
        try {
            NewDayInit();

            ZonedDateTime now = TradeTimer.GetNYTimeNow();
            LocalTime closeTimeLocal = TradeTimer.GetTodayCloseTime();

            if (closeTimeLocal == null) {
                logger.info("No trading today.");
                ScheduleForTomorrow();
                return;
            }

            logger.info("Time in NY now: " + now);
            logger.info("Closing at: " + closeTimeLocal);

            ZonedDateTime lastCall = now.with(closeTimeLocal).minus(DURATION_BEFORECLOSE_LASTCALL);
            logger.fine("LastCall: " + lastCall);

            if (now.compareTo(lastCall) > 0) {
                logger.info("Not enough time today.");
                ScheduleForTomorrow();
                return;
            }

            isStartScheduled = true;

            ZonedDateTime closeTimeZoned = now.with(closeTimeLocal);

            PrepareData();
            
            if (!broker.connect() ) {
                isCheckOk = false;
                return;
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                    throw new IllegalStateException("InterruptedException");
            }
            
            isCheckOk &= NinetyChecker.PerformChecks(statusData, stockData, broker);
            
            logger.fine(broker.GetAccountSummary().toString());
            broker.disconnect();

            if (isCheckOk) {
                ScheduleTradingRun(closeTimeZoned.minus(DURATION_BEFORECLOSE_RUNSTRATEGY));
            }
            
        } finally {
            if (!isCheckOk) {
                logger.severe("Check failed. Scheduling check for next hour.");

                TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plusHours(1), this::PrepareForTrading);
                MailSender.SendErrors();
            } else {
                MailSender.SendErrors();
                MailSender.AddLineToMail("Check complete");
                AddProfitLossToMail();
                MailSender.SendCheckResult();
            }
        }
    }

    public void PrepareData() {
        try {
            CheckingThread checkThread = CheckingThread.StartNewCheckingThread(Duration.ofMinutes(2), "Failed to prepare data");
        
            logger.finer("Acquiring lock for LoadingHistData run.");
            dataMutex.acquire();
            new NinetyDataPreparator(stockData, broker).run();
            stockData.SaveHistDataToFiles();
            dataMutex.release();
            logger.finer("Released lock for LoadingHistData run.");
            
            checkThread.SetChecked();
        } catch (InterruptedException ex) {
            throw new IllegalStateException("InterruptedException");
        }
    }

    public void ScheduleLoadingHistData(ZonedDateTime runTime) {
        Runnable taskWrapper = new Runnable() {
            @Override
            public void run() {
                PrepareData();
            }
        };
        
        TradeTimer.startTaskAt(runTime, taskWrapper);

        Duration timeToHist = Duration.ofSeconds(TradeTimer.computeTimeFromNowTo(runTime));

        logger.info("Reading historic data is scheduled for " + runTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + timeToHist.toString());
    }
    
    public void ScheduleTradingRun(ZonedDateTime runTime) {
        Duration timeToStart = Duration.ofSeconds(TradeTimer.computeTimeFromNowTo(runTime));

        logger.info("Starting Ninety strategy is scheduled for " + runTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + timeToStart.toString());
        
        Runnable taskWrapper = new Runnable() {
            @Override
            public void run() {
                try {
                    CheckingThread checkThread = CheckingThread.StartNewCheckingThread(Duration.ofMinutes(2), "Trade run did not end properly.");
            
                    logger.finer("Acquiring lock for trading run.");
                    dataMutex.acquire();
                    new NinetyRunner(stockData, statusData, broker).run();
                    dataMutex.release();
                    logger.finer("Released lock for trading run.");
                    Thread.sleep(5000);
                    statusData.UpdateEquityFile();
                    ScheduleForTomorrow();
                    stockData.SaveHistDataToFiles();
                    stockData.SaveIndicatorsToCSVFile();
                    stockData.SaveStockIndicatorsToFiles();
                    
                    AddProfitLossToMail();
                    
                    MailSender.AddLineToMail(broker.GetAccountSummary().toString());
                    MailSender.AddLineToMail("Saved current cash: " + TradeFormatter.toString(statusData.currentCash));
                    
                    MailSender.SendTradingLog();
                    MailSender.SendErrors();
                    
                    checkThread.SetChecked();
                } catch (InterruptedException ex) {
                    throw new IllegalStateException("InterruptedException");
                }
            }
        };

        TradeTimer.startTaskAt(runTime, taskWrapper);
    }
    
    public void Stop() {
        logger.info("Stopping execution of Ninety strategy.");
        TradeTimer.stop();
        logger.info("Execution of Ninety strategy is stopped.");
        isStartScheduled = false;
    }
    
    public void AddProfitLossToMail() {
        if (stockData.indicatorsMap.isEmpty()) { // This happens during no trade days
            return;
        }
        
        int heldAboveSMA200 = 0;
        for (StockIndicatorsForNinety inds : stockData.indicatorsMap.values()) {
            if (inds.actValue > inds.sma200) {
                heldAboveSMA200++;
            }
        }
        
        double heldAboveSMA200Percent = (double) heldAboveSMA200 / stockData.indicatorsMap.size() * 100.0;
        
        String strTickersAboveSMA200 = "Tickers with actual value above SMA200: " + heldAboveSMA200 + "/" + stockData.indicatorsMap.size() 
                + " = " + TradeFormatter.toString(heldAboveSMA200Percent) + "%";
        
        MailSender.AddLineToMail(strTickersAboveSMA200);
        logger.info(strTickersAboveSMA200);
        
        MailSender.AddLineToMail("Unrealized profit/loss:");
        logger.info("Unrealized profit/loss:");
        
        double totalPL = 0;
        for (HeldStock held : statusData.heldStocks.values()) {
            StockIndicatorsForNinety indicators = stockData.indicatorsMap.get(held.tickerSymbol);
            if (indicators == null) {
                continue;
            }
            double actValue = indicators.actValue;
            double profit = held.CalculateProfitIfSold(actValue);
            double profitPercent = held.CalculatePercentProfitIfSold(actValue);
            
            totalPL += profit;
            
            String strHeldStats = held.tickerSymbol + 
                    ": " + TradeFormatter.toString(profit) + "$ = " + TradeFormatter.toString(profitPercent) + 
                    "%. Last buy value: " + TradeFormatter.toString(held.GetLastBuyValue()) + 
                    ", actual value: " + TradeFormatter.toString(actValue) + 
                    ", SMA5: " + TradeFormatter.toString(indicators.sma5) +
                    ", portions: " + held.GetPortions();
            
            MailSender.AddLineToMail(strHeldStats);
            logger.info(strHeldStats);
        }
        
        String strTotalPL = "Total unrealized profit/loss: " + TradeFormatter.toString(totalPL) 
                + "$ = " + TradeFormatter.toString(totalPL / Settings.investCash * 100) + "%";
        
        MailSender.AddLineToMail(strTotalPL);
        logger.info(strTotalPL);
    }
}
