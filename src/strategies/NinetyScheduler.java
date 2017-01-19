/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import communication.IBBroker;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import static tradingapp.MainWindow.LOGGER_TADELOG_NAME;
import tradingapp.TradingTimer;

/**
 *
 * @author Muhe
 */
public class NinetyScheduler {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private final static Logger loggerTradeLog = Logger.getLogger(LOGGER_TADELOG_NAME);

    private final static LocalTime FIRST_CHECK_TIME = LocalTime.of(10, 0);
    private final static Duration DURATION_BEFORECLOSE_HISTDATA = Duration.ofMinutes(5);
    private final static Duration DURATION_BEFORECLOSE_RUNSTRATEGY = Duration.ofMinutes(1);

    public StockDataForNinety stockData = new StockDataForNinety();
    public StatusDataForNinety statusData = new StatusDataForNinety();

    public final IBBroker broker = new IBBroker();
    public boolean isStartScheduled = false;
    
    public final Semaphore dataMutex = new Semaphore(1);

    public NinetyScheduler() {
        statusData.ReadHeldPositions();
        statusData.PrintStatus();
    }

    public void RunNow() {
        isStartScheduled = true;
        TradingTimer.LoadSpecialTradingDays();
        
        ScheduleLoadingHistData(ZonedDateTime.now().plusSeconds(3));
        ScheduleTradingRun(ZonedDateTime.now().plusMinutes(1));
    }

    private void ScheduleForTomorrow() {
        logger.info("Scheduling for tomorrow!");
        isStartScheduled = true;
        ZonedDateTime tomorrowCheck = TradingTimer.GetNYTimeNow().plusDays(1).with(FIRST_CHECK_TIME);
        TradingTimer.startTaskAt(tomorrowCheck, new Runnable() {
            @Override
            public void run() {
                ScheduleFirstCheck();
            }
        });

        Duration durationToNextRun = Duration.ofSeconds(TradingTimer.computeTimeFromNowTo(tomorrowCheck));
        
        copyLogFileToDataLog();

        logger.info("Next check is scheduled for " + tomorrowCheck.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + durationToNextRun.toString());
    }

    public void ScheduleFirstCheck() {
        TradingTimer.LoadSpecialTradingDays();

        ZonedDateTime now = TradingTimer.GetNYTimeNow();
        LocalTime closeTimeLocal = TradingTimer.GetTodayCloseTime();

        if (closeTimeLocal == null) {
            logger.info("No trading today.");
            ScheduleForTomorrow();
            return;
        }

        logger.info("Time in NY now: " + now);
        logger.info("Closing at: " + closeTimeLocal);

        ZonedDateTime lastCall = now.with(closeTimeLocal).minus(DURATION_BEFORECLOSE_HISTDATA);
        logger.info("LastCall: " + lastCall);

        if (now.compareTo(lastCall) > 0) {
            logger.info("Not enough time today.");
            ScheduleForTomorrow();
            return;
        }

        isStartScheduled = true;
        
        ZonedDateTime closeTimeZoned = now.with(closeTimeLocal);
        
        ScheduleLoadingHistData(closeTimeZoned.minus(DURATION_BEFORECLOSE_HISTDATA));
        
        ScheduleTradingRun(closeTimeZoned.minus(DURATION_BEFORECLOSE_RUNSTRATEGY));
                

        broker.connect();
        NinetyChecker.CheckHeldPositions(statusData, broker);
        broker.disconnect();
    }
    
    public void ScheduleLoadingHistData(ZonedDateTime runTime) {
        Runnable taskWrapper = new Runnable() {
            @Override
            public void run() {
                try {
                    logger.finer("Acquiring lock for LoadingHistData run.");
                    dataMutex.acquire();
                    new NinetyDataPreparator(stockData, broker).run();
                    dataMutex.release();
                    logger.finer("Released lock for LoadingHistData run.");
                } catch (InterruptedException ex) {
                    throw new IllegalStateException("InterruptedException");
                }
            }
        };
        
        TradingTimer.startTaskAt(runTime, taskWrapper);

        Duration timeToHist = Duration.ofSeconds(TradingTimer.computeTimeFromNowTo(runTime));

        logger.info("Reading historic data is scheduled for " + runTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + timeToHist.toString());
    }
    
    public void ScheduleTradingRun(ZonedDateTime runTime) {
        Runnable taskWrapper = new Runnable() {
            @Override
            public void run() {
                try {
                    logger.finer("Acquiring lock for trading run.");
                    dataMutex.acquire();
                    new NinetyRunner(stockData, statusData, broker).run();
                    ScheduleForTomorrow();
                    dataMutex.release();
                    logger.finer("Released lock for trading run.");
                } catch (InterruptedException ex) {
                    throw new IllegalStateException("InterruptedException");
                }
            }
        };

        TradingTimer.startTaskAt(runTime, taskWrapper);

        Duration timeToStart = Duration.ofSeconds(TradingTimer.computeTimeFromNowTo(runTime));

        logger.info("Starting Ninety strategy is scheduled for " + runTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + timeToStart.toString());
    }
    
    public void Stop() {
        logger.info("Stopping execution of Ninety strategy.");
        TradingTimer.stop();
        logger.info("Execution of Ninety strategy is stopped.");
        isStartScheduled = false;
    }
    
    private static void copyLogFileToDataLog() {
        
        String todayString = LocalDate.now().toString();
        File newLogFile = new File("dataLog/" + todayString + "/log.txt");
        File directory = new File(newLogFile.getParentFile().getAbsolutePath());
        directory.mkdirs();
        
        try {
            newLogFile.delete();

            File localLog = new File("Logging.txt");
            Files.copy(localLog.toPath(), newLogFile.toPath());
            localLog.delete();
            
        } catch (IOException ex) {
            logger.severe("Cannot copy local log to dataLog. " + ex);
        }
    }
}
