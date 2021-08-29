/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyMondayBuyer;

import communication.IBroker;
import data.Utils;
import data.getters.DataGetterActIB;
import data.getters.DataGetterHistIB;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import strategy90.TickersToTrade;
import tradingapp.CheckingThread;
import tradingapp.FilePaths;
import tradingapp.MailSender;
import tradingapp.Report;
import tradingapp.Settings;
import tradingapp.TradeFormatter;
import tradingapp.TradeLogger;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class MBScheduler {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final static LocalTime FIRST_CHECK_TIME = LocalTime.of(6, 0);
    private final static LocalTime TRADE_TIME = LocalTime.of(9, 30);
    private final static Duration DURATION_BEFORE_LASTCALL = Duration.ofMinutes(10);

    public final IBroker broker;
    public boolean isStartScheduled = false;

    public final Semaphore dataMutex = new Semaphore(1);

    public final MBData data;
    public final MBStatus status;
    public String[] tickers;

    public MBScheduler(IBroker broker) {
        this.broker = broker;
        data = new MBData(broker);
        status = new MBStatus();
    }

    public void NewDayInit() {
        String todayString = TradeTimer.GetLocalDateNow().toString();
        String[] attachmentsTradeLog = {FilePaths.dataLogDirectory + todayString + FilePaths.logPathFile,
            FilePaths.dataLogDirectory + todayString + FilePaths.logCommPathFile,
            FilePaths.dataLogDirectory + todayString + FilePaths.logDetailedPathFile,
            FilePaths.equityPathFile,
            FilePaths.tradingStatusPathFileInput,
            FilePaths.tradeLogPathFile,
            //FilePaths.dataLogDirectory + todayString + FilePaths.indicatorsPathFile,
            FilePaths.reportPathFile
        };

        String[] attachmentsError = {FilePaths.dataLogDirectory + todayString + FilePaths.logPathFile,
            FilePaths.dataLogDirectory + todayString + FilePaths.logCommPathFile,
            FilePaths.dataLogDirectory + todayString + FilePaths.logDetailedPathFile,
            FilePaths.equityPathFile,
            FilePaths.tradingStatusPathFileInput
        };

        MailSender.SetTradeLogAttachments(attachmentsTradeLog);
        MailSender.SetErrorAttachments(attachmentsError);

        MailSender.SetTradeLogSubject("AOS Trade log MB");
        MailSender.SetCheckSubject("AOS Check MB");
        MailSender.SetErrorSubject("AOS Errors MB");

        TradeLogger.getInstance().clearLogs();
        TradeLogger.getInstance().initializeFiles(LocalDate.now());
        TradeTimer.LoadSpecialTradingDays();
        TickersToTrade.LoadTickers();

        Settings.ReadSettings();
        tickers = Utils.LoadTickers();

        status.LoadTradingStatus();
        status.removeOldRecSold();
        status.PrintStatus();
    }

    public void ScheduleForNow() {
        NewDayInit();

        if (!broker.connect()) {
            return;
        }
        
        TradeTimer.SetToday(TradeTimer.GetLastTradingDay());

        PrepareForTrading();

        new MBRunner(data, status, broker).run();
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
            logger.info("Starting at: " + TRADE_TIME);
            logger.info("Closing at: " + closeTimeLocal);

            ZonedDateTime lastCall = now.with(TRADE_TIME).minus(DURATION_BEFORE_LASTCALL);
            logger.fine("LastCall: " + lastCall);

            if (now.compareTo(lastCall) > 0) {
                logger.info("Not enough time today.");
                ScheduleForTomorrow();
                return;
            }

            isStartScheduled = true;

            //ZonedDateTime closeTimeZoned = now.with(closeTimeLocal);
            if (!broker.connect()) {
                isCheckOk = false;
                return;
            }
            TradeTimer.wait(1000);

            PrepareData();

            isCheckOk &= MBChecker.PerformChecks(status, data, broker);

            logger.fine(broker.GetAccountSummary().toString());
            broker.disconnect();

            if (isCheckOk) {
                ZonedDateTime startTimeZoned = now.with(TRADE_TIME);
                ScheduleTradingRun(startTimeZoned);
            }
        } catch (Exception e) {
            logger.severe("Failed prepare for trading: " + e.toString());
        } finally {
            if (!isCheckOk) {
                logger.severe("Check failed. Scheduling check for next minute.");

                TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plusMinutes(1), this::PrepareForTrading);
                MailSender.SendErrors();
            } else {
                MailSender.SendErrors();
                MailSender.AddLineToMail("Check complete");
                AddProfitLossToMail();
                MailSender.SendCheckResult();
            }
        }
    }

    public void AddProfitLossToMail() {
        if (data.indicatorsMap.isEmpty()) { // This happens during no trade days
            return;
        }

        MailSender.AddLineToMail("Unrealized profit/loss:");
        logger.info("Unrealized profit/loss:");

        double totalPL = 0;
        for (MBHeldTicker held : status.heldTickers.values()) {
            MBIndicators indicators = data.indicatorsMap.get(held.ticker);
            if (indicators == null) {
                continue;
            }
            double lastValue = data.getLastKnownPrice(held.ticker);
            double profit = held.CalculateProfitIfSold(lastValue);
            double profitPercent = held.CalculatePercentProfitIfSold(lastValue);

            totalPL += profit;

            String strHeldStats = held.ticker
                    + ": " + TradeFormatter.toString(profit) + "$ = " + TradeFormatter.toString(profitPercent)
                    + ", last value: " + TradeFormatter.toString(lastValue)
                    + ", price: " + TradeFormatter.toString(held.price);

            MailSender.AddLineToMail(strHeldStats);
            logger.info(strHeldStats);
        }

        String strTotalPL = "Total unrealized profit/loss: " + TradeFormatter.toString(totalPL)
                + "$ = " + TradeFormatter.toString(totalPL / Settings.investCash * 100) + "%";

        MailSender.AddLineToMail(strTotalPL);
        logger.info(strTotalPL);
    }

    public void ScheduleTradingRun(ZonedDateTime runTime) {
        Duration timeToStart = Duration.ofSeconds(TradeTimer.computeTimeFromNowTo(runTime));

        logger.info("Starting MB strategy is scheduled for " + runTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + timeToStart.toString());

        Runnable taskWrapper = new Runnable() {
            @Override
            public void run() {
                try {
                    CheckingThread checkThread = CheckingThread.StartNewCheckingThread(Duration.ofMinutes(5), "Trade run did not end properly.");

                    int connectTries = 3;

                    while (connectTries-- > 0) {
                        if (broker.connect()) {
                            break;
                        } else {
                            logger.warning("Cannot connect to IB. Trying again.");
                        }
                    }

                    if (connectTries < 0) {
                        logger.warning("Failed to connect to IB. Exiting trading!");
                        return;
                    }

                    logger.finer("Acquiring lock for trading run.");
                    dataMutex.acquire();
                    new MBRunner(data, status, broker).run();
                    dataMutex.release();
                    logger.finer("Released lock for trading run.");

                    status.UpdateEquityFile();

                    broker.RequestHistoricalData("SPY", Report.GetNrOfDaysInEquity());
                    broker.SubscribeRealtimeData("SPY");

                    Thread.sleep(5000);
                    ScheduleForTomorrow();
                    //data.SaveHistDataToFiles();
                    //data.SaveIndicatorsToCSVFile();
                    //data.SaveStockIndicatorsToFiles();

                    AddProfitLossToMail();
                    MailSender.AddLineToMail(broker.GetAccountSummary().toString());
                    MailSender.AddLineToMail("Saved current equity: " + TradeFormatter.toString(status.equity));

                    Report.Generate(new DataGetterHistIB(broker), new DataGetterActIB(broker), "SPY", false);

                    MailSender.SendTradingLog();
                    MailSender.SendErrors();

                    checkThread.SetChecked();
                } catch (InterruptedException ex) {
                    throw new IllegalStateException("InterruptedException");
                } catch (Exception e) {
                    logger.severe(e.getMessage());
                } finally {
                    broker.disconnect();
                }
            }
        };

        TradeTimer.startTaskAt(runTime, taskWrapper);
    }

    public void PrepareData() {
        try {
            CheckingThread checkThread = CheckingThread.StartNewCheckingThread(Duration.ofMinutes(60), "Failed to prepare data");

            logger.finer("Acquiring lock for LoadingHistData run.");
            dataMutex.acquire();
            data.PrepareData(tickers);
            //data.SaveHistDataToFiles();
            dataMutex.release();
            logger.finer("Released lock for LoadingHistData run.");

            checkThread.SetChecked();
        } catch (InterruptedException ex) {
            throw new IllegalStateException("InterruptedException");
        }
    }

    public void Stop() {
    }
}
