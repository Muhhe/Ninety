/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author Muhe
 */
public class TradingTimer {

    private static TradingTimer instance = null;
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private final static LocalTime DEFAULT_CLOSE_TIME = LocalTime.of(16, 00);

    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);

    private List<TradingDay> specialTradingDays = new ArrayList<TradingDay>();

    public final static ZoneId ZONE_NY = ZoneId.of("America/New_York");

    private static class TradingDay {

        LocalDate date = null;
        LocalTime closingTime = null;
    }
    
    protected TradingTimer() {
        // Exists only to defeat instantiation.
    }

    public static TradingTimer getInstance() {
        if (instance == null) {
            instance = new TradingTimer();
        }
        return instance;
    }

    public static ZonedDateTime GetNYTimeNow() {
        return ZonedDateTime.now(ZONE_NY);
    }

    public static void startTaskAt(final ZonedDateTime time, Runnable runnableTask) {
        TradingTimer timer = getInstance();

        long delay = computeTimeFromNowTo(time);
        
        if (delay < 0) {
            logger.severe("Execution start time is set to past!!!");
            return;
        }
        
        timer.executorService.schedule(runnableTask, delay, TimeUnit.SECONDS);
    }

    public static void startTaskAt(int targetHour, int targetMin, int targetSec, Runnable runnableTask) {
        TradingTimer timer = getInstance();
        
        long delay = computeTimeFromNowTo(targetHour, targetMin, targetSec);
        timer.executorService.schedule(runnableTask, delay, TimeUnit.SECONDS);
    }

    public static long computeTimeFromNowTo(ZonedDateTime time) {        
        Duration duration = Duration.between(GetNYTimeNow(), time);
        return duration.getSeconds();
    }

    public static long computeTimeFromNowTo(int targetHour, int targetMin, int targetSec) {
        ZonedDateTime zonedNowNY = GetNYTimeNow();
        ZonedDateTime zonedNextTargetNY = zonedNowNY.withHour(targetHour).withMinute(targetMin).withSecond(targetSec).withNano(0);
        if (zonedNowNY.compareTo(zonedNextTargetNY) > 0) {
            zonedNextTargetNY = zonedNextTargetNY.plusDays(1);
        }

        Duration duration = Duration.between(zonedNowNY, zonedNextTargetNY);
        return duration.getSeconds();
    }

    public static void stop() {
        TradingTimer timer = getInstance();
        timer.executorService.shutdownNow();
        try {
            if (!timer.executorService.awaitTermination(10, TimeUnit.MINUTES)) {
                logger.severe("Cannot stop execution.");
            }
        } catch (InterruptedException ex) {
            logger.severe(ex.getMessage());
        }
        timer.executorService = Executors.newScheduledThreadPool(5); 
    }
    
    public static boolean IsTradingDay(LocalDate day) {
        TradingTimer timer = getInstance();
        
        DayOfWeek dow = day.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        
        for (TradingDay specialDay : timer.specialTradingDays) {
            if (specialDay.date.equals(day)) {
                return specialDay.closingTime != null;
            }
        }
        
        return true;
    }
    
    public static LocalTime GetTodayCloseTime() {
        TradingTimer timer = getInstance();
        
        LocalDate today = LocalDate.now();
        LocalTime closingTime = DEFAULT_CLOSE_TIME;
        
        DayOfWeek dow = today.getDayOfWeek();

        logger.info("It's " + dow + "!");

        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            logger.info("Nothing to trade during weekend");
            return null;
        }
        
        for (TradingDay day : timer.specialTradingDays) {
            if (day.date.equals(today)) {
                if (day.closingTime == null) {
                    logger.info("Today is holiday!");
                    return null;
                } else {
                    closingTime = day.closingTime;
                    logger.info("Today is a special day! Closing at " + closingTime.toString());
                    break;
                }
            }
        }
        
        return closingTime;
    }
    
    public static void LoadSpecialTradingDays() {
        TradingTimer timer = getInstance();
        
        logger.fine("Loading special days!");
        timer.specialTradingDays.clear();
        try {
            File inputFile = new File("specialTradingDays.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);

            Element rootElement = document.getRootElement();
            List<Element> dayElements = rootElement.getChildren();

            for (Element dayElement : dayElements) {
                TradingDay day = new TradingDay();
                Attribute attribute = dayElement.getAttribute("date");
                if (attribute != null) {
                    String dateStr = attribute.getValue();
                    day.date = LocalDate.parse(dateStr);
                } else {
                    logger.severe("Failed to load special trading day from " + inputFile.getAbsolutePath());
                    continue;
                }
                
                attribute = dayElement.getAttribute("closeTime");
                if (attribute != null) {
                    String timeStr = attribute.getValue();
                    day.closingTime = LocalTime.parse(timeStr);
                } else {
                    day.closingTime = null;
                }

                timer.specialTradingDays.add(day);
            }
            
            logger.fine("Special days loaded: " + timer.specialTradingDays.size());
        } catch (JDOMException e) {
            e.printStackTrace();
            logger.severe("Error in loading special days from XML: JDOMException.\r\n" + e);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.severe("Error in loading special days from XML: IOException.\r\n" + ioe);
        }
    }
}
