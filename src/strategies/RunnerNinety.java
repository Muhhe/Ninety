/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import communication.IBBroker;
import communication.OrderStatus;
import communication.Position;
import data.CloseData;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import static tradingapp.MainWindow.LOGGER_TADELOG_NAME;
import tradingapp.TradingTimer;
import tradingapp.TradeOrder;

/**
 *
 * @author Muhe
 */
public class RunnerNinety {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private final static Logger loggerTradeLog = Logger.getLogger(LOGGER_TADELOG_NAME );
    
    private final static LocalTime FIRST_CHECK_TIME = LocalTime.of(10, 0);
    private final static Duration DURATION_BEFORECLOSE_HISTDATA = Duration.ofMinutes(5);
    private final static Duration DURATION_BEFORECLOSE_RUNSTRATEGY = Duration.ofMinutes(1);

    public StockDataForNinety stockData = new StockDataForNinety();
    public StatusDataForNinety statusData = new StatusDataForNinety();

    //private final IBCommunication m_IBcomm = new IBCommunication();
    public final IBBroker broker = new IBBroker();

    //public boolean isStrategyRunning = false;
    public boolean isStartScheduled = false;
    
    private TradingTimer timer = new TradingTimer();

    private Ninety ninety = new Ninety();

    public RunnerNinety(int port) {
        statusData.ReadHeldPositions();
        statusData.PrintStatus();
    }
    
    public void RunNow() {
        isStartScheduled = true;
        
        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                //broker.connect();
                stockData.PrepareHistData();  
                timer.LoadSpecialTradingDays();
                
                //RunNinety();
                
                stockData.UpdateDataWithActValues(timer);
                stockData.CheckHistData(LocalDate.now(), timer);
                stockData.CalculateIndicators();
                //TODO: run check

                statusData.PrintStatus();

                logger.info("Starting Ninety strategy");
                
                List<TradeOrder> sells = RunNinetySells();

                stockData.UpdateDataWithActValues(timer);
                
                RunNinetyBuys(sells);
                
                //broker.disconnect();
                
                copyLogFileToDataLog();
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

        logger.info("Next check is scheduled for " + tomorrowCheck.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
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
        CheckHeldPositions();
        
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
                LocalDate today = LocalDate.now();
                String todayString = today.toString();
                FileHandler fileHandler = null;
                try {
                    File file = new File("dataLog/" + todayString);
                    file.mkdirs();
                    
                    fileHandler = new FileHandler("dataLog/" + todayString + "/TradeLog.txt");
                } catch (IOException | SecurityException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                loggerTradeLog.addHandler(fileHandler);
                
                //RunNinety();
                stockData.UpdateDataWithActValues(timer);
                stockData.CalculateIndicators();
                stockData.CheckHistData(today, timer);

                statusData.PrintStatus();

                logger.info("Starting Ninety strategy");
                
                List<TradeOrder> sells = RunNinetySells();
                
                if (!broker.waitUntilOrdersClosed(20)) {
                    logger.severe("Some SELL orders were not closed on time.");
                }

                ProcessSubmittedOrders();
                CheckHeldPositions();

                stockData.UpdateDataWithActValues(timer);
                stockData.CheckHistData(today, timer);
                
                RunNinetyBuys(sells);
                
                if (!broker.waitUntilOrdersClosed(60)) {
                    logger.severe("Some orders were not closed on time.");
                }

                ProcessSubmittedOrders();
                broker.orderStatusMap.clear();

                CheckHeldPositions();

                statusData.SaveHeldPositionsToFile();
                
                logger.info("Trading day finished");
                statusData.PrintStatus();
                isStartScheduled = false;
                
                ScheduleForTomorrowDay();
                
                fileHandler.close();
                loggerTradeLog.removeHandler(fileHandler);
                
                broker.disconnect();
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
    
    public List<TradeOrder> RunNinetySells() {
        logger.info("Starting computing stocks to sell");

        statusData.PrintStatus();

        // Selling held stocks
        List<TradeOrder> sellOrders = Ninety.ComputeStocksToSell(stockData.indicatorsMap, statusData);

        for (TradeOrder tradeOrder : sellOrders) {
            broker.PlaceOrder(tradeOrder);
        }

        logger.info("Finished computing stocks to sell.");
        
        return sellOrders;
    }
    
    private void RunNinetyBuys(List<TradeOrder> sellOrders) {
        logger.info("Starting computing stocks to buy");
        // Buying new stock
        TradeOrder buyOrder = Ninety.ComputeStocksToBuy(stockData.indicatorsMap, statusData, sellOrders);
        broker.PlaceOrder(buyOrder);

        // Buying more held stock
        List<TradeOrder> buyMoreOrders = Ninety.computeStocksToBuyMore(stockData.indicatorsMap, statusData);

        for (TradeOrder tradeOrder : buyMoreOrders) {
            broker.PlaceOrder(tradeOrder);
        }

        logger.info("Finished computing stocks to buy.");
    }
    

    /*private void RunNinety() {

        try {

            //if (timer.GetTodayCloseTime() == null) {
            //    return;
            //}

            // TODO: co kdyz je to moc dlouho? Poresit v StockData a hodit dyztak vyjimku
            //logger.info("RunNinety: Getting lock on hist data.");
            //boolean acuiredInTime = stockData.histDataMutex.tryAcquire(1, TimeUnit.MINUTES);
            //if (!acuiredInTime) {
            //    logger.severe("Acuire on hist data lock timed out!");
            //    return;
            //}
            //logger.info("RunNinety: Got lock on hist data.");

            //stockData.UpdateDataWithActValues(timer);
            //stockData.CalculateIndicators();
            //TODO: run check

            logger.info("Starting Ninety strategy");

            statusData.PrintStatus();

            // Selling held stocks
            List<TradeOrder> sellOrders = Ninety.ComputeStocksToSell(stockData.indicatorsMap, statusData);

            for (TradeOrder tradeOrder : sellOrders) {
                broker.PlaceOrder(tradeOrder);
            }

            logger.info("Finished computing stocks to sell.");

            if (!broker.waitUntilOrdersClosed(20)) {
                logger.severe("Some SELL orders were not closed on time.");
            }
            
            ProcessSubmittedOrders();
            
            CheckHeldPositions();
            
            stockData.UpdateDataWithActValues(timer);

            // Buying new stock
            TradeOrder buyOrder = Ninety.ComputeStocksToBuy(stockData.indicatorsMap, statusData, sellOrders);
            broker.PlaceOrder(buyOrder);

            // Buying more held stock
            List<TradeOrder> buyMoreOrders = Ninety.computeStocksToBuyMore(stockData.indicatorsMap, statusData);

            for (TradeOrder tradeOrder : buyMoreOrders) {
                broker.PlaceOrder(tradeOrder);
            }

            logger.info("Finished computing stocks to buy.");

            if (!broker.waitUntilOrdersClosed(60)) {
                logger.severe("Some orders was not closed on time.");
            }
            
            // TODO: predelat
            ProcessSubmittedOrders();
            broker.orderStatusMap.clear();
            
            CheckHeldPositions();
            
            statusData.SaveHeldPositionsToFile();
            
        } finally {
            
            logger.info("Trading day finished");
            statusData.PrintStatus();
            //isStrategyRunning = false;
            isStartScheduled = false;
        }
    }*/

    private void ProcessSubmittedOrders() {
        
        for (Iterator<Map.Entry<Integer, OrderStatus>> it = broker.orderStatusMap.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, OrderStatus> entry = it.next();
            OrderStatus order = entry.getValue();

            if (order.status != OrderStatus.Status.FILLED) {
                logger.severe("Order NOT closed - " + order.toString());
                UpdateHeldByOrderStatus(order);
                continue;
            }

            logger.info("Order closed - " + order.toString());
            UpdateHeldByOrderStatus(order);

            it.remove();
        }
    }
    
    private void UpdateHeldByOrderStatus(OrderStatus order) {
        HeldStock held = statusData.heldStocks.get(order.order.tickerSymbol);
        
        if (order.filled == 0) {
            return;
        }
        
        // Add new stock
        if (held == null) {
            if (order.order.orderType == TradeOrder.OrderType.SELL) {
                logger.severe("Trying to sell not held stock: " + order.order.tickerSymbol);
                return;
            }

            held = new HeldStock();
            held.tickerSymbol = order.order.tickerSymbol;

            StockPurchase purchase = new StockPurchase();
            purchase.date = order.timestampFilled;
            purchase.portions = 1;
            purchase.position = order.filled;
            purchase.priceForOne = order.fillPrice;

            held.purchases.add(purchase);

            statusData.heldStocks.put(held.tickerSymbol, held);
            logger.finer("New stock added: " + held.toString());
            
            loggerTradeLog.info(order.toString());
            //TODO: doplnit indicatory atd.
            return;
        }

        if (order.order.orderType == TradeOrder.OrderType.SELL) {
            double profit = (order.fillPrice - held.GetAvgPrice()) * held.GetPosition();
            logger.info("Stock removed - profit: " + profit + ", " + order.toString());
            
            loggerTradeLog.info(order.toString() + ", profit: " + profit);
            //TODO: doplnit indicatory atd.
            
            if (order.filled != held.GetPosition()) {
                logger.severe("Not all position has been sold for: " + held.tickerSymbol);
                // TODO: nejak poresit
                return;
            }

            statusData.heldStocks.remove(held.tickerSymbol);
        } else {
            
            int newPortions = 1;
            switch (held.GetPortions()) {
                case 1:
                    newPortions = 2;
                    break;
                case 3:
                    newPortions = 3;
                    break;
                case 6:
                    newPortions = 4;
                    break;
                default:
                    logger.severe("Bought stock '" + held.tickerSymbol + "' has somehow " + held.GetPortions() + " bought portions!!!");
                    //TODO: dafuq?
            }
            
            StockPurchase purchase = new StockPurchase();
            purchase.date = order.timestampFilled;
            purchase.portions = newPortions;
            purchase.position = order.filled;
            purchase.priceForOne = order.fillPrice;

            held.purchases.add(purchase);
            logger.info("More stock bought - " + held.toString());
            
            loggerTradeLog.info(order.toString());
            //TODO: doplnit indicatory atd.
        }
    }
    
    public void CheckHeldPositions() {
        List<Position> allPositions = broker.getAllPositions();
        
        logger.fine("Held postions on IB: " + allPositions.size());
        for (Position position : allPositions) {
            logger.fine("Stock: " + position.toString());
            
            if (!statusData.heldStocks.containsKey(position.tickerSymbol)) {
                logger.warning("Stock " + position.tickerSymbol + " found on IB but it's not locally saved. Position " + position.pos);
            }
        }
        
        boolean isOk = true;
        for (HeldStock held : statusData.heldStocks.values()) {
            boolean found = false;
            for (Position position : allPositions) {
                if (position.tickerSymbol.contentEquals(held.tickerSymbol)) {
                    found = true;
                    
                    if (held.GetPosition() != position.pos) {
                        logger.severe("Held position mismatch for ticker: " + held.tickerSymbol + ", position on IB: " + position.pos + " vs saved: " + held.GetPosition());
                        isOk = false;
                    }
                    break;
                }
            }
            if (!found) {
                logger.severe("Held position not found on IB: " + held.tickerSymbol + ", position: " + held.GetPosition());
                        isOk = false;
            }
        }
        if (isOk) {
            logger.info("Check on held position - OK");
        } else {
            logger.severe("Check on held position - FAILED");
        }
    }
    
    private static void copyLogFileToDataLog() {
        
        String todayString = LocalDate.now().toString();
        File file = new File("dataLog/" + todayString + "/log.txt");
        File directory = new File(file.getParentFile().getAbsolutePath());
        directory.mkdirs();
        
        File localLog = new File("Logging.txt");
        
        try {
            Files.copy(localLog.toPath(), file.toPath());
        } catch (IOException ex) {
            logger.severe("Cannot copy local log to dataLog. " + ex);
        }
        
        localLog.delete();
    }
}
