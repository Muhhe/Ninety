/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import communication.IBBroker;
import communication.IBCommunication;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;
import tradingapp.TradingTimer;
import tradingapp.TradeOrder;

/**
 *
 * @author Muhe
 */
public class RunnerNinety {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private final static LocalTime FIRST_CHECK_TIME = LocalTime.of(10, 0);
    //private final static Duration DURATION_BEFORECLOSE_HISTDATA = Duration.ofMinutes(5);
    //private final static Duration DURATION_BEFORECLOSE_RUNSTRATEGY = Duration.ofMinutes(1);
    
    private final static Duration DURATION_BEFORECLOSE_HISTDATA = Duration.ofMinutes(1);
    private final static Duration DURATION_BEFORECLOSE_RUNSTRATEGY = Duration.ofMinutes(0);

    public StockDataForNinety stockData = new StockDataForNinety();
    public StatusDataForNinety statusData = new StatusDataForNinety();

    //private final IBCommunication m_IBcomm = new IBCommunication();
    private final IBBroker broker = new IBBroker();

    //public boolean isStrategyRunning = false;
    public boolean isStartScheduled = false;
    
    private TradingTimer timer = new TradingTimer();

    private Ninety ninety = new Ninety();

    public RunnerNinety(int port) {
        //statusData.ReadHeldPositions();
    }
    
    public void RunNow() {
        isStartScheduled = true;
        
        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
        
                broker.connect();
                stockData.PrepareHistData();
                RunNinety();
            }
        });
        
        thr.start();
    }
    
    private void ScheduleForTomorrowDay() {
        logger.info("Scheduling for tomorrow!");
        isStartScheduled = true;
        ZonedDateTime tomorrowCheck = TradingTimer.GetNYTimeNow().plusDays(1).with(FIRST_CHECK_TIME);
        timer.startTaskAt(tomorrowCheck, new Runnable() {
            @Override
            public void run() {
                ScheduleFirstCheck();
            }
        });
        
        Duration durationToNextRun = Duration.ofSeconds(TradingTimer.computeTimeFromNowTo(tomorrowCheck));

        logger.info("Next run is scheduled for " + tomorrowCheck.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + durationToNextRun.toString());
    }
    
    public void ScheduleFirstCheck() {      
        timer.LoadSpecialTradingDays();
        
        ZonedDateTime now = TradingTimer.GetNYTimeNow();
        LocalTime closeTime = timer.GetTodayCloseTime();

        if (closeTime == null) {
            logger.info("No trading today.");
            ScheduleForTomorrowDay();
            return;
        }

        logger.info("Time in NY now: " + now);
        logger.info("Closing at: " + closeTime);
        
        ZonedDateTime lastCall = now.with(closeTime).minus(DURATION_BEFORECLOSE_HISTDATA);
        logger.info("LastCall: " + lastCall);
        
        if (now.compareTo(lastCall) > 0) {
            logger.info("Not enough time today.");
            ScheduleForTomorrowDay();
            return;
        }
          
        isStartScheduled = true;
        ScheduleLoadingHistDataAndStrategyRun(now.with(closeTime));
        
        broker.connect();
    }

    public void ScheduleLoadingHistDataAndStrategyRun(ZonedDateTime closeTime) {
        
        /*if (isStrategyRunning) {
            logger.warning("Ninety strategy is already running.");
            return;
        }*/
        
        ZonedDateTime timeLoadHistData = closeTime.minus(DURATION_BEFORECLOSE_HISTDATA);
        timer.startTaskAt(timeLoadHistData, new Runnable() {
            @Override
            public void run() {
                // TODO: check held positions
                stockData.PrepareHistData();
                // TODO: run check
            }
        });

        Duration timeToHist = Duration.ofSeconds(TradingTimer.computeTimeFromNowTo(timeLoadHistData));

        logger.info("Reading historic data is scheduled for " + timeLoadHistData.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + timeToHist.toString());

        ZonedDateTime timeRunStrategy = closeTime.minus(DURATION_BEFORECLOSE_RUNSTRATEGY);
        timer.startTaskAt(timeRunStrategy, new Runnable() {
            @Override
            public void run() {
                RunNinety();
                ScheduleForTomorrowDay();
            }
        });

        Duration timeToStart = Duration.ofSeconds(TradingTimer.computeTimeFromNowTo(timeRunStrategy));

        logger.info("Starting Ninety strategy is scheduled for " + timeRunStrategy.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + timeToStart.toString());

        //isStrategyRunning = true;
    }

    public void Stop() {
        logger.info("Stopping execution of Ninety strategy.");
        timer.stop();
        logger.info("Execution of Ninety strategy is stopped.");
        //isStrategyRunning = false;
        isStartScheduled = false;
    }
    public void BuyLoadedStatus() {
        for (HeldStock heldStock : statusData.heldStocks.values()) {
            TradeOrder order = new TradeOrder();
            order.orderType = TradeOrder.OrderType.BUY;
            order.tickerSymbol = heldStock.tickerSymbol;
            order.position = heldStock.GetPosition();
            broker.PlaceOrder(order);
        }
    }

    public void SellAllPositions() {
        //m_IBBroker.SellAllPositions();
    }

    private void RunNinety() {

        try {

            //if (timer.GetTodayCloseTime() == null) {
            //    return;
            //}

            // TODO: co kdyz je to moc dlouho? Poresit v StockData a hodit dyztak vyjimku
            /*logger.info("RunNinety: Getting lock on hist data.");
            boolean acuiredInTime = stockData.histDataMutex.tryAcquire(1, TimeUnit.MINUTES);
            if (!acuiredInTime) {
                logger.severe("Acuire on hist data lock timed out!");
                return;
            }
            logger.info("RunNinety: Got lock on hist data.");*/

            stockData.UpdateDataWithActValues();
            stockData.CalculateIndicators();
            //TODO: run check

            logger.info("Starting Ninety strategy");

            statusData.PrintStatus();

            // Selling held stocks
            List<TradeOrder> sellOrders = Ninety.ComputeStocksToSell(stockData.indicatorsMap, statusData);
            //List<TradeOrder> sellOrders = ProcessStocksToSellIntoOrders(stocksToSell);

            for (TradeOrder tradeOrder : sellOrders) {
                broker.PlaceOrder(tradeOrder);
            }

            logger.info("Finished computing stocks to sell.");

            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                logger.info("Thread interuppted: " + ex);
            }

            // TODO: wait until done
            // Buying new stock
            TradeOrder buyOrder = Ninety.ComputeStocksToBuy(stockData.indicatorsMap, statusData, sellOrders);
            //TradeOrder buyOrder = ProcessStockToBuyIntoOrder(tickerToBuy, stocksToSell);
            broker.PlaceOrder(buyOrder);

            // Buying more held stock
            List<TradeOrder> buyMoreOrders = Ninety.computeStocksToBuyMore(stockData.indicatorsMap, statusData);
            //List<TradeOrder> buyMoreOrders = ProcessStocksToBuyMoreIntoOrders(stocksToBuyMore);

            for (TradeOrder tradeOrder : buyMoreOrders) {
                broker.PlaceOrder(tradeOrder);
            }

            logger.info("Finished computing stocks to buy.");

            try {
                Thread.sleep(30000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                logger.info("Thread interuppted: " + ex);
            }

            // TODO: wait until done
            // TODO: check held positions
            statusData.SaveHeldPositionsToFile();

        //} catch (InterruptedException ex) {
        //    Logger.getLogger(RunnerNinety.class.getName()).log(Level.SEVERE, null, ex);
        } finally {

            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                logger.info("Thread interuppted: " + ex);
            }

            //stockData.histDataMutex.release();
            //logger.info("RunNinety: Released lock on hist data.");
            
            logger.info("Trading day finished");
            statusData.PrintStatus();
            //isStrategyRunning = false;
            isStartScheduled = false;
        }
    }
}
