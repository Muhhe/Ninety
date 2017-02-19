/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import communication.BrokerIB;
import communication.IBroker;
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
import tradingapp.TradingTimer;

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
        statusData.ReadHeldPositions();
        statusData.PrintStatus();
        
        this.broker = broker;
    }
    
    public void NewDayInit() {
        TradeLogger.getInstance().clearLogs(); 
        TradeLogger.getInstance().initializeFiles(LocalDate.now());
        TradingTimer.LoadSpecialTradingDays();
        TickersToTrade.LoadTickers();

        Settings.ReadSettings();

        statusData.UpdateCashSettings();
        statusData.ReadHeldPositions();
        statusData.PrintStatus();
    }

    public void RunNow() {
        NewDayInit();
        
        isStartScheduled = true;
        
        ScheduleLoadingHistData(ZonedDateTime.now().plusSeconds(3));
        ScheduleTradingRun(ZonedDateTime.now().plusMinutes(1));
    }

    private void ScheduleForTomorrow() {
        logger.info("Scheduling for tomorrow!");
        isStartScheduled = true;
        ZonedDateTime tomorrowCheck = TradingTimer.GetNYTimeNow().plusDays(1).with(FIRST_CHECK_TIME);
        TradingTimer.startTaskAt(tomorrowCheck, this::DoInitialization);

        Duration durationToNextRun = Duration.ofSeconds(TradingTimer.computeTimeFromNowTo(tomorrowCheck));
        
        logger.info("Next check is scheduled for " + tomorrowCheck.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + durationToNextRun.toString());

        MailSender.SendErrors();
    }

    public void ScheduleFirstCheck() {
        ZonedDateTime earliestCheckTime = TradingTimer.GetNYTimeNow().with(FIRST_CHECK_TIME);
        
        ZonedDateTime checkTime = TradingTimer.GetNYTimeNow().plusSeconds(1);

        if (checkTime.compareTo(earliestCheckTime) <= 0) {
            checkTime = earliestCheckTime;
        
            Duration durationToNextRun = Duration.ofSeconds(TradingTimer.computeTimeFromNowTo(checkTime));

            logger.info("First check is scheduled for " + checkTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            logger.info("Starting in " + durationToNextRun.toString());
        }

        TradingTimer.startTaskAt(checkTime, this::DoInitialization);
    }

    public void DoInitialization() {
        boolean isCheckOk = true;
        try {
            NewDayInit();

            ZonedDateTime now = TradingTimer.GetNYTimeNow();
            LocalTime closeTimeLocal = TradingTimer.GetTodayCloseTime();

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

                TradingTimer.startTaskAt(ZonedDateTime.now().plusHours(1), this::DoInitialization);

            }
            
            if (!MailSender.SendErrors()) {
                MailSender.AddLineToMail("Check complete");
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
        
        TradingTimer.startTaskAt(runTime, taskWrapper);

        Duration timeToHist = Duration.ofSeconds(TradingTimer.computeTimeFromNowTo(runTime));

        logger.info("Reading historic data is scheduled for " + runTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + timeToHist.toString());
    }
    
    public void ScheduleTradingRun(ZonedDateTime runTime) {
        Duration timeToStart = Duration.ofSeconds(TradingTimer.computeTimeFromNowTo(runTime));

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

        TradingTimer.startTaskAt(runTime, taskWrapper);
    }
    
    public void Stop() {
        logger.info("Stopping execution of Ninety strategy.");
        TradingTimer.stop();
        logger.info("Execution of Ninety strategy is stopped.");
        isStartScheduled = false;
    }
}
