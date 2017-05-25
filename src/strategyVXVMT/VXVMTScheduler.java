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
import java.util.logging.Level;
import java.util.logging.Logger;
import tradingapp.FilePaths;
import tradingapp.MailSender;
import tradingapp.Settings;
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
    public final IBroker broker;

    public VXVMTScheduler(IBroker broker) {
        Settings.ReadSettings();
        status.LoadTradingStatus();
        this.broker = broker;
    }

    private void RunNow() {
        logger.info("Starting run!");
        new VXVMTRunner(status, broker).Run();
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
        logger.info("Starting in 2 seconds.");
        TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plusSeconds(2), this::RunNow);
    }

    public void ScheduleRun() {
        NewDayInit();

        broker.connect();

        VXVMTChecker.CheckHeldPositions(status, broker);

        ZonedDateTime now = TradeTimer.GetNYTimeNow();
        LocalTime closeTimeLocal = TradeTimer.GetTodayCloseTime();
        if (closeTimeLocal == null) {
            logger.info("No trading today.");
            ScheduleForTomorrow();

            broker.disconnect();
            return;
        }

        // Ulozit data
        VXVMTIndicators indicators = VXVMTDataPreparator.LoadData(broker);
        broker.SubscribeRealtimeData("XIV");
        broker.SubscribeRealtimeData("VXX");
        broker.SubscribeRealtimeData("VXV", IBroker.SecType.IND);
        broker.SubscribeRealtimeData("VXMT", IBroker.SecType.IND);
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }
        
        status.PrintStatus(indicators.actXIVvalue, indicators.actVXXvalue);

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
                VXVMTChecker.CheckHeldPositions(status, broker);
                RunNow();

                ScheduleForTomorrow();
                MailSender.SendTradingLog();
            }
        };

        TradeTimer.startTaskAt(runTime, taskWrapper);
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
