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
        NewDayInit();
        broker.connect();
        logger.info("Subscribing data (3 sec).");
        broker.SubscribeRealtimeData("XIV");
        broker.SubscribeRealtimeData("VXX");
        broker.SubscribeRealtimeData("VXV", IBroker.SecType.IND);
        broker.SubscribeRealtimeData("VXMT", IBroker.SecType.IND);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }
        data = VXVMTDataPreparator.LoadData(broker);
        if (!VXVMTChecker.CheckData(data)) {
            logger.severe("Data check failed!");
            return;
        }

        TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plusSeconds(2), this::RunNow);
    }

    public void ScheduleRun() {
        NewDayInit();

        broker.connect();

        if (!VXVMTChecker.CheckHeldPositions(status, broker)) {
            logger.severe("Held position check failed! Scheduling check for next hour.");
            TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plusHours(1), this::ScheduleRun);
            MailSender.SendErrors();
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

        logger.info("Subscribing data (10 sec).");
        broker.SubscribeRealtimeData("XIV");
        broker.SubscribeRealtimeData("VXX");
        broker.SubscribeRealtimeData("VXV", IBroker.SecType.IND);
        broker.SubscribeRealtimeData("VXMT", IBroker.SecType.IND);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
        }

        data = VXVMTDataPreparator.LoadData(broker);

        if (!VXVMTChecker.CheckData(data)) {
            logger.severe("Data check failed! Scheduling check for next hour.");
            TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plusHours(1), this::ScheduleRun);
            MailSender.SendErrors();
            return;
        }

        status.PrintStatus(data.actXIVvalue, data.actVXXvalue);

        broker.disconnect();

        ZonedDateTime closeTimeZoned = now.with(closeTimeLocal);
        ZonedDateTime runTime = closeTimeZoned.minus(DURATION_BEFORECLOSE_RUNSTRATEGY);
        Duration timeToStart = Duration.ofSeconds(TradeTimer.computeTimeFromNowTo(runTime));

        if (now.compareTo(runTime) > 0) {
            logger.info("Not enough time today.");
            ScheduleForTomorrow();
            return;
        }

        logger.info("Starting VXVMT strategy is scheduled for " + runTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + timeToStart.toString());

        Runnable taskWrapper = new Runnable() {
            @Override
            public void run() {
                broker.connect();
                VXVMTChecker.CheckHeldPositions(status, broker);
                RunNow();
                
                broker.disconnect();

                status.UpdateEquity(data.actXIVvalue, data.actVXXvalue);
                status.SaveTradingStatus();
                
                ScheduleForTomorrow();
                MailSender.SendTradingLog();
            }
        };

        TradeTimer.startTaskAt(runTime, taskWrapper);

        double equity = status.GetEquity(data.actXIVvalue, data.actVXXvalue);
        double diff = (equity - status.closingEquity);
        double prc = diff / status.closingEquity * 100.0;

        MailSender.AddLineToMail("Check ok");
        MailSender.AddLineToMail("Held '" + status.heldType + "', position: " + status.heldPosition);
        MailSender.AddLineToMail("Equity - " + TradeFormatter.toString(status.GetEquity(data.actXIVvalue, data.actVXXvalue)));
        MailSender.AddLineToMail("Current profit/loss - " + TradeFormatter.toString(diff) + " = " + TradeFormatter.toString(prc) + "%");

        MailSender.SendCheckResult();
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
}
