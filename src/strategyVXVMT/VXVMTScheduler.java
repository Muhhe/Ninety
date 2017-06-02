/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyVXVMT;

import communication.IBroker;
import data.getters.DataGetterActIB;
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
            FilePaths.tradingStatusPathFileInput
        };

        String[] attachmentsError = {FilePaths.dataLogDirectory + todayString + FilePaths.logPathFile,
            FilePaths.dataLogDirectory + todayString + FilePaths.logCommPathFile,
            FilePaths.dataLogDirectory + todayString + FilePaths.logDetailedPathFile,
            FilePaths.equityPathFile,
            FilePaths.tradingStatusPathFileInput};

        MailSender.SetTradeLogAttachments(attachmentsTradeLog);
        MailSender.SetErrorAttachments(attachmentsError);

        MailSender.SetTradeLogSubject("AOS Trade log VXVMT");
        MailSender.SetCheckSubject("AOS Check VXVMT");
        MailSender.SetErrorSubject("AOS Errors VXVMT");

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
            broker.disconnect();
            return;
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

        status.PrintStatus(data.indicators.actXIVvalue, data.indicators.actVXXvalue);

        broker.disconnect();

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

                broker.disconnect();

                String signalInfo = new String();
                if (signal == null) {
                    logger.severe("Run failed!");
                } else {
                    signalInfo = signal.typeToString() + "-" + TradeFormatter.toString(signal.exposure);
                }

                AddSignalInfoToMail(signal);

                status.UpdateEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue, signalInfo);
                status.SaveTradingStatus();

                ScheduleForTomorrow();
                MailSender.SendTradingLog();
                checkThread.SetChecked();
            }
        };

        TradeTimer.startTaskAt(runTime, taskWrapper);

        double equity = status.GetEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue);
        double diff = (equity - status.closingEquity);
        double prc = diff / status.closingEquity * 100.0;

        MailSender.AddLineToMail("Check ok");
        MailSender.AddLineToMail("Held '" + status.heldType + "', position: " + status.heldPosition);
        MailSender.AddLineToMail("Equity - " + TradeFormatter.toString(status.GetEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue)));
        MailSender.AddLineToMail("Current profit/loss - " + TradeFormatter.toString(diff) + " = " + TradeFormatter.toString(prc) + "%");

        MailSender.SendCheckResult();
    }

    public boolean PrepareData() {
        CheckingThread checkThread = CheckingThread.StartNewCheckingThread(Duration.ofMinutes(5), "Failed to prepare data");
        logger.info("Subscribing data (1 min wait).");
        broker.SubscribeRealtimeData("XIV");
        broker.SubscribeRealtimeData("VXX");
        broker.SubscribeRealtimeData("VXV", IBroker.SecType.IND);
        broker.SubscribeRealtimeData("VXMT", IBroker.SecType.IND);

        try {
            Thread.sleep(60000);
        } catch (InterruptedException ex) {
        }

        data = VXVMTDataPreparator.LoadData(broker);
        VXVMTDataPreparator.ComputeIndicators(data);

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

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }

        IDataGetterAct actGetter = new DataGetterActIB(broker);
        double vxx = actGetter.readActualData("VXX");
        double xiv = actGetter.readActualData("XIV");

        status.PrintStatus(xiv, vxx);

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
                + ", Ratio SMA60: " + TradeFormatter.toString(data.indicators.ratiosLagged[0])
                + ", Ratio SMA125: " + TradeFormatter.toString(data.indicators.ratiosLagged[0])
                + ", Ratio SMA150: " + TradeFormatter.toString(data.indicators.ratiosLagged[0]);

        String str4 = "Today act ratio: " + TradeFormatter.toString(data.indicators.actRatio)
                + ", Ratio SMA60: " + TradeFormatter.toString(data.indicators.ratios[0])
                + ", Ratio SMA125: " + TradeFormatter.toString(data.indicators.ratios[0])
                + ", Ratio SMA150: " + TradeFormatter.toString(data.indicators.ratios[0]);

        double equity = status.GetEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue);
        double diff = (equity - status.closingEquity);
        double prc = diff / status.closingEquity * 100.0;
        
        String str5 = "Currently held '" + status.heldType + "', position: " + status.heldPosition;
        String str6 = "Equity - " + TradeFormatter.toString(status.GetEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue))+ "$";
        String str7 = "Today's profit/loss - " + TradeFormatter.toString(diff) + "$ = " + TradeFormatter.toString(prc) + "%";


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
    }
}
