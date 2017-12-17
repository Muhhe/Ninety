/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyVXVMT;

import communication.IBroker;
import data.getters.DataGetterActIB;
import data.getters.DataGetterHistIB;
import data.getters.IDataGetterAct;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
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
public class VXVMTScheduler {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final static LocalTime FIRST_CHECK_TIME = LocalTime.of(10, 0);
    private final static Duration DURATION_BEFORECLOSE_RUNSTRATEGY = Duration.ofMinutes(2);

    public final VXVMTStatus status = new VXVMTStatus();
    public VXVMTData data = null;
    public final IBroker broker;

    public boolean isStartScheduled = false;

    public VXVMTScheduler(IBroker broker) {
        Settings.ReadSettings();
        status.LoadTradingStatus();
        this.broker = broker;
    }

    private void RunNow() {
        logger.info("Starting run!");
        new VXVMTRunner(status, broker).Run(data);
        logger.info("Run finished!");
        status.UpdateEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue, data.indicators.actGLDvalue, "?");
        status.SaveTradingStatus();
        logger.info(broker.GetAccountSummary().toString());
    }

    private void ScheduleForTomorrow() {
        logger.info("Scheduling for tomorrow!");
        ZonedDateTime tomorrowCheck = TradeTimer.GetNYTimeNow().plusDays(1).with(FIRST_CHECK_TIME);
        TradeTimer.startTaskAt(tomorrowCheck, this::ScheduleRun);

        Duration durationToNextRun = Duration.ofSeconds(TradeTimer.computeTimeFromNowTo(tomorrowCheck));

        logger.info("Next check is scheduled for " + tomorrowCheck.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + durationToNextRun.toString());

        MailSender.SendErrors();
    }

    private void NewDayInit() {
        String todayString = TradeTimer.GetLocalDateNow().toString();
        String[] attachmentsTradeLog = {FilePaths.dataLogDirectory + todayString + FilePaths.logPathFile,
            FilePaths.dataLogDirectory + todayString + FilePaths.logCommPathFile,
            FilePaths.dataLogDirectory + todayString + FilePaths.logDetailedPathFile,
            FilePaths.equityPathFile,
            FilePaths.tradingStatusPathFileInput,
            FilePaths.reportPathFile
        };

        String[] attachmentsError = {FilePaths.dataLogDirectory + todayString + FilePaths.logPathFile,
            FilePaths.dataLogDirectory + todayString + FilePaths.logCommPathFile,
            FilePaths.dataLogDirectory + todayString + FilePaths.logDetailedPathFile,
            FilePaths.equityPathFile,
            FilePaths.tradingStatusPathFileInput};

        MailSender.SetTradeLogAttachments(attachmentsTradeLog);
        MailSender.SetErrorAttachments(attachmentsError);

        MailSender.SetTradeLogSubject("AOS Trade log POMER");
        MailSender.SetCheckSubject("AOS Check POMER");
        MailSender.SetErrorSubject("AOS Errors POMER");

        TradeLogger.getInstance().clearLogs();
        TradeLogger.getInstance().initializeFiles(LocalDate.now());
        TradeTimer.LoadSpecialTradingDays();

        Settings.ReadSettings();
        status.LoadTradingStatus();
    }

    public void ScheduleForNow() {
        isStartScheduled = true;

        NewDayInit();
        broker.connect();

        if (!PrepareData()) {
            logger.severe("Data check failed!");
            isStartScheduled = false;
            return;
        }

        TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plusSeconds(2), this::RunNow);
    }

    public void ScheduleRun() {
        isStartScheduled = true;
        NewDayInit();

        if (!broker.connect()) {
            logger.severe("Failed to connect to IB! Scheduling check for next hour.");
            TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plusHours(1), this::ScheduleRun);
            MailSender.SendErrors();
            return;
        }

        if (!VXVMTChecker.CheckHeldPositions(status, broker)) {
            logger.severe("Held position check failed! Scheduling check for next hour.");
            TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plusHours(1), this::ScheduleRun);
            MailSender.SendErrors();
            return;
        }
        if (!VXVMTChecker.CheckCash(status, broker)) {
            logger.severe("Cash position check failed! Scheduling check for next hour.");
            TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plusHours(1), this::ScheduleRun);
            MailSender.SendErrors();
        }

        ZonedDateTime now = TradeTimer.GetNYTimeNow();
        LocalTime closeTimeLocal = TradeTimer.GetTodayCloseTime();
        if (closeTimeLocal == null) {
            logger.info("No trading today.");
            ScheduleForTomorrow();

            broker.disconnect();
            return;
        }

        ZonedDateTime closeTimeZoned = now.with(closeTimeLocal);
        ZonedDateTime runTime = closeTimeZoned.minus(DURATION_BEFORECLOSE_RUNSTRATEGY);
        Duration timeToStart = Duration.ofSeconds(TradeTimer.computeTimeFromNowTo(runTime));

        if (now.compareTo(runTime) > 0) {
            logger.info("Not enough time today.");
            ScheduleForTomorrow();
            broker.disconnect();
            return;
        }

        if (!PrepareData()) {
            logger.severe("Data load failed! Scheduling check for next hour.");
            TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plusHours(1), this::ScheduleRun);
            MailSender.SendErrors();
            return;
        }

        status.PrintStatus(data.indicators.actXIVvalue, data.indicators.actVXXvalue, data.indicators.actGLDvalue);

        logger.info("Starting VXVMT strategy is scheduled for " + runTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + timeToStart.toString());

        Runnable taskWrapper = new Runnable() {
            @Override
            public void run() {
                CheckingThread checkThread = CheckingThread.StartNewCheckingThread(Duration.ofMinutes(5), "Trade run did not end properly.");
                broker.connect();
                if (!VXVMTChecker.CheckHeldPositions(status, broker)) {
                    logger.severe("Failed check position! Ending run.");
                    ScheduleForTomorrow();

                    checkThread.SetChecked();
                    return;
                }

                logger.info("Starting run!");
                VXVMTSignal signal = new VXVMTRunner(status, broker).Run(data);
                logger.info("Run finished!");

                String signalInfo = new String();
                if (signal == null) {
                    logger.severe("Run failed!");
                } else {
                    signalInfo = signal.typeToString() + "-" + TradeFormatter.toString(signal.exposure);
                }

                VXVMTChecker.CheckCash(status, broker);
                VXVMTChecker.CheckHeldPositions(status, broker);

                AddSignalInfoToMail(signal);

                status.UpdateEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue, data.indicators.actGLDvalue, signalInfo);
                status.SaveTradingStatus();

                broker.RequestHistoricalData("XIV", Report.GetNrOfDaysInEquity());

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                }

                Report.Generate(new DataGetterHistIB(broker), "XIV", true);

                broker.disconnect();

                ScheduleForTomorrow();
                MailSender.SendTradingLog();
                checkThread.SetChecked();
            }
        };

        //logger.severe("Delete this!");
        //TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plusSeconds(2), taskWrapper);
        TradeTimer.startTaskAt(runTime, taskWrapper);

        MailSender.AddLineToMail("Check done");
        MailSender.AddLineToMail("Held '" + status.heldType + "', position: " + status.heldPosition);
        MailSender.AddLineToMail("Equity: " + TradeFormatter.toString(status.GetEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue, data.indicators.actGLDvalue)));

        if (status.heldType != VXVMTSignal.Type.None) {
            double equity = status.GetEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue, data.indicators.actGLDvalue);
            double diff = (equity - status.closingEquity);
            double prc = diff / status.closingEquity * 100.0;

            MailSender.AddLineToMail("Today's profit/loss: " + TradeFormatter.toString(diff) + "$ = " + TradeFormatter.toString(prc) + "%");

            double unrDiff = GetUnrealizedProfit();
            double unrPrc = unrDiff / status.closingEquity * 100.0;

            MailSender.AddLineToMail("Total unrealized profit/loss: " + TradeFormatter.toString(unrDiff) + "$ = " + TradeFormatter.toString(unrPrc) + "%");
        }

        MailSender.SendErrors();
        MailSender.SendCheckResult();
    }

    public boolean PrepareData() {
        CheckingThread checkThread = CheckingThread.StartNewCheckingThread(Duration.ofMinutes(5), "Failed to prepare data");
        logger.info("Subscribing data (1 min wait).");
        broker.SubscribeRealtimeData("XIV");
        broker.SubscribeRealtimeData("VXX");
        broker.SubscribeRealtimeData("GLD");
        broker.SubscribeRealtimeData("VIX3M", IBroker.SecType.IND);
        broker.SubscribeRealtimeData("VXMT", IBroker.SecType.IND);

        //logger.warning("REMOVE!");
        try {
            Thread.sleep(30000);
        } catch (InterruptedException ex) {
        }
        broker.SubscribeRealtimeData("VIX3M", IBroker.SecType.IND);
        broker.SubscribeRealtimeData("VXMT", IBroker.SecType.IND);
        try {
            Thread.sleep(30000);
        } catch (InterruptedException ex) {
        }

        data = VXVMTDataPreparator.LoadData(broker);
        if (data == null) {
            checkThread.SetChecked();
            return false;
        }

        VXVMTDataPreparator.UpdateIndicators(broker, data);

        if (!VXVMTChecker.CheckDataIndicators(data)) {
            return false;
        }

        checkThread.SetChecked();
        return true;
    }

    public void CheckHeldPositions() {
        boolean connected = broker.isConnected();
        if (!connected) {
            broker.connect();
        }

        broker.SubscribeRealtimeData("VXX");
        broker.SubscribeRealtimeData("XIV");
        broker.SubscribeRealtimeData("GLD");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }

        IDataGetterAct actGetter = new DataGetterActIB(broker);
        double vxx = actGetter.readActualData("VXX");
        double xiv = actGetter.readActualData("XIV");
        double gld = actGetter.readActualData("GLD");

        status.PrintStatus(xiv, vxx, gld);

        VXVMTChecker.CheckHeldPositions(status, broker);

        if (!connected) {
            broker.disconnect();
        }
    }

    public void Stop() {
        logger.info("Stopping execution of VXVMT strategy.");
        TradeTimer.stop();
        logger.info("Execution of VXVMT strategy is stopped.");
        isStartScheduled = false;
    }

    public double GetUnrealizedProfit() {
        double value = 0;
        if (status.heldType == VXVMTSignal.Type.VXX) {
            value = data.indicators.actVXXvalue;
        } else if (status.heldType == VXVMTSignal.Type.XIV) {
            value = data.indicators.actXIVvalue;
        } else if (status.heldType == VXVMTSignal.Type.GLD) {
            value = data.indicators.actGLDvalue;
        }

        return (value - status.avgPrice) * status.heldPosition;
    }

    public void AddSignalInfoToMail(VXVMTSignal signal) {

        String str1 = "Today's signal - type: " + signal.type + ", exposure: " + TradeFormatter.toString(signal.exposure);

        String str2 = "Positive signal for: ";
        if (signal.XIVSignals[0]) {
            str2 += "XIV-60day ";
        }
        if (signal.XIVSignals[1]) {
            str2 += "XIV-125day ";
        }
        if (signal.XIVSignals[2]) {
            str2 += "XIV-150day ";
        }

        if (signal.VXXSignals[0]) {
            str2 += "VXX-60day ";
        }
        if (signal.VXXSignals[1]) {
            str2 += "VXX-125day ";
        }
        if (signal.VXXSignals[2]) {
            str2 += "VXX-150day ";
        }

        String str3 = "Yesterday act ratio: " + TradeFormatter.toString(data.indicators.actRatioLagged)
                + ", SMA60: " + TradeFormatter.toString(data.indicators.ratiosLagged[0])
                + ", SMA125: " + TradeFormatter.toString(data.indicators.ratiosLagged[1])
                + ", SMA150: " + TradeFormatter.toString(data.indicators.ratiosLagged[2]);

        String str4 = "Today act ratio: " + TradeFormatter.toString(data.indicators.actRatio)
                + ", SMA60: " + TradeFormatter.toString(data.indicators.ratios[0])
                + ", SMA125: " + TradeFormatter.toString(data.indicators.ratios[1])
                + ", SMA150: " + TradeFormatter.toString(data.indicators.ratios[2]);

        String str5 = "Currently held '" + status.heldType + "', position: " + status.heldPosition + ", avgPrice: " + status.avgPrice + "$";
        String str6 = "Equity: " + TradeFormatter.toString(status.GetEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue, data.indicators.actGLDvalue)) + "$";
        String str7 = "Free cash: " + TradeFormatter.toString(status.freeCapital) + "$";

        MailSender.AddLineToMail(str1);
        MailSender.AddLineToMail(str2);
        MailSender.AddLineToMail(str3);
        MailSender.AddLineToMail(str4);
        MailSender.AddLineToMail(str5);
        MailSender.AddLineToMail(str6);
        MailSender.AddLineToMail(str7);

        logger.info(str1);
        logger.info(str2);
        logger.info(str3);
        logger.info(str4);
        logger.info(str5);
        logger.info(str6);
        logger.info(str7);

        if (status.heldType == VXVMTSignal.Type.None) {
            return;
        }

        double equity = status.GetEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue, data.indicators.actGLDvalue);
        double diff = (equity - status.closingEquity);
        double prc = diff / status.closingEquity * 100.0;

        String str8 = "Today's profit/loss: " + TradeFormatter.toString(diff) + "$ = " + TradeFormatter.toString(prc) + "%";

        double unrDiff = GetUnrealizedProfit();
        double unrPrc = unrDiff / status.closingEquity * 100.0;

        String str9 = "Unrealized profit/loss: " + TradeFormatter.toString(unrDiff) + "$ = " + TradeFormatter.toString(unrPrc) + "%";

        MailSender.AddLineToMail(str8);
        MailSender.AddLineToMail(str9);
        logger.info(str8);
        logger.info(str9);
    }
}
