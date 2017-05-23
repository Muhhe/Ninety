/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyVXVMT;

import communication.IBroker;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import strategy90.TickersToTrade;
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

    private final VXVMTStatus status = new VXVMTStatus();
    public final IBroker broker;

    public VXVMTScheduler(IBroker broker) {
        this.broker = broker;
    }

    private void RunNow() {
        new VXVMTRunner(status, broker).Run();
    }

    private void ScheduleForTomorrow() {
        logger.info("Scheduling for tomorrow!");
        ZonedDateTime tomorrowCheck = TradeTimer.GetNYTimeNow().plusDays(1).with(FIRST_CHECK_TIME);
        TradeTimer.startTaskAt(tomorrowCheck, this::ScheduleRun);

        Duration durationToNextRun = Duration.ofSeconds(TradeTimer.computeTimeFromNowTo(tomorrowCheck));

        logger.info("Next check is scheduled for " + tomorrowCheck.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + durationToNextRun.toString());

        MailSender.SendErrors();
        MailSender.SendCheckResult();
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
        TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plusSeconds(2), this::RunNow);
    }

    public void ScheduleRun() {
        NewDayInit();

        ZonedDateTime now = TradeTimer.GetNYTimeNow();
        LocalTime closeTimeLocal = TradeTimer.GetTodayCloseTime();
        if (closeTimeLocal == null) {
            logger.info("No trading today.");
            ScheduleForTomorrow();
            return;
        }

        ZonedDateTime closeTimeZoned = now.with(closeTimeLocal);
        ZonedDateTime runTime = closeTimeZoned.minus(DURATION_BEFORECLOSE_RUNSTRATEGY);
        Duration timeToStart = Duration.ofSeconds(TradeTimer.computeTimeFromNowTo(runTime));

        logger.info("Starting VXVMT strategy is scheduled for " + runTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + timeToStart.toString());

        if (now.compareTo(runTime) > 0) {
            logger.info("Not enough time today.");
            ScheduleForTomorrow();
            return;
        }
        
         Runnable taskWrapper = new Runnable() {
            @Override
            public void run() {
                logger.info("Starting run!");
                new VXVMTRunner(status, broker).Run();
                logger.info("Run finished!");
                
                ScheduleForTomorrow();
        

                status.SaveTradingStatus();

                MailSender.SendErrors();
                MailSender.SendTradingLog();
            }
         };

        TradeTimer.startTaskAt(runTime, taskWrapper);
    }
}
