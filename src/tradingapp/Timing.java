/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class Timing {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    Runnable myTask;
    volatile boolean isStopIssued;

    public Timing(Runnable myTask_) {
        myTask = myTask_;

    }

    public void startExecutionAt(int targetHour, int targetMin, int targetSec) {
        Runnable taskWrapper = new Runnable() {

            @Override
            public void run() {
                myTask.run();
            }

        };
        
        long delay = computeTimeFromNowTo(targetHour, targetMin, targetSec);
        executorService.schedule(taskWrapper, delay, TimeUnit.SECONDS);
    }

    public long computeTimeFromNowTo(int targetHour, int targetMin, int targetSec) {
        LocalDateTime localNow = LocalDateTime.now();
        ZoneId currentZone = ZoneId.systemDefault();
        ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
        ZoneId zoneNY = ZoneId.of("America/New_York");
        ZonedDateTime zonedNowNY = zonedNow.withZoneSameInstant(zoneNY);
        ZonedDateTime zonedNextTargetNY = zonedNowNY.withHour(targetHour).withMinute(targetMin).withSecond(targetSec).withNano(0);
        if (zonedNowNY.compareTo(zonedNextTargetNY) > 0) {
            zonedNextTargetNY = zonedNextTargetNY.plusDays(1);
        }

        Duration duration = Duration.between(zonedNowNY, zonedNextTargetNY);
        return duration.getSeconds();
    }

    public void stop() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            logger.severe(ex.getMessage());
        }
    }
}
