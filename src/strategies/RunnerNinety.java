/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import communication.IBCommunication;
import data.CloseData;
import data.StockIndicatorsForNinety;
import data.DataGetterActGoogle;
import data.DataGetterHistYahoo;
import data.IndicatorCalculator;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import tradingapp.Timing;
import tradingapp.TradeOrder;

/**
 *
 * @author Muhe
 */
public class RunnerNinety {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public Map<String, CloseData> m_closeDataMap = new HashMap<String, CloseData>(100);
    public Map<String, StockIndicatorsForNinety> m_dataFor90Map = new HashMap<String, StockIndicatorsForNinety>(100);

    public StatusDataForNinety m_statusDataFor90 = new StatusDataForNinety();

    private final IBCommunication m_IBcomm = new IBCommunication();

    public boolean isRunning = false;

    private Timing m_timerHistData;
    private Timing m_timerStartNinety;

    private Semaphore histDataMutex = new Semaphore(1);

    private Ninety ninety = new Ninety();

    public RunnerNinety(int port) {
        m_IBcomm.connect(port);
    }

    public boolean CheckIfTradingDay() {
        LocalDate date = LocalDate.now();
        DayOfWeek dow = date.getDayOfWeek();

        logger.info("It's " + dow + "!");

        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            logger.info("Nothing to trade during weekend");
            return false;
        }
        //TODO: check if holiday
        return true;
    }

    public void Start() {
        //TODO: check if trading ends early
        ZonedDateTime time = Timing.GetNYTimeNow();
        time = time.plusSeconds(5);
        //time = time.withHour(15).withMinute(50).withSecond(0);
        if (Timing.GetNYTimeNow().compareTo(time) > 0) {
            time = time.plusDays(1);
        }

        m_timerHistData = new Timing(new Runnable() {
            @Override
            public void run() {
                // TODO: check held positions
                PrepareHistData();
                // TODO: run check
            }
        });
        m_timerHistData.startExecutionAt(time);

        Duration timeToHist = Duration.ofSeconds(m_timerHistData.computeTimeFromNowTo(time));

        logger.info("Reading historic data is scheduled for " + time.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + timeToHist.toString());

        time = time.plusSeconds(100);
        //time = time.withMinute(59);

        m_timerStartNinety = new Timing(new Runnable() {
            @Override
            public void run() {
                RunNinety();
            }
        });
        m_timerStartNinety.startExecutionAt(time);

        Duration timeToStart = Duration.ofSeconds(m_timerHistData.computeTimeFromNowTo(time));

        logger.info("Starting Ninety strategy is scheduled for " + time.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        logger.info("Starting in " + timeToStart.toString());

        isRunning = true;
    }

    public void Stop() {
        logger.info("Stopping execution of Ninety strategy.");
        m_timerHistData.stop();
        m_timerStartNinety.stop();
        logger.info("Execution of Ninety strategy is stopped.");
        isRunning = false;
    }

    private void PrepareHistData() {

        try {
            logger.info("PrepareHistData: Getting lock on hist data.");
            histDataMutex.acquire();
            logger.info("Starting to load historic data");
            String[] tickers = getSP100();
            for (String ticker : tickers) {
                CloseData data = DataGetterHistYahoo.readData(LocalDate.now(), 200, ticker);
                m_closeDataMap.put(ticker, data);
            }

            logger.info("Finished to load historic data");

        } catch (InterruptedException ex) {
            Logger.getLogger(RunnerNinety.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            histDataMutex.release();
            logger.info("PrepareHistData: Released lock on hist data.");
        }
    }

    public void UpdateDataWithActValues() {
        if (m_closeDataMap.isEmpty()) {
            logger.severe("updateDataWithActualValues - stockMap.isEmpty");
        }

        logger.info("Starting to load actual data");

        try {
            String[] tickerSymbols = getSP100();
            Map<String, Double> valuesMap = DataGetterActGoogle.readActualData(tickerSymbols);

            if (tickerSymbols.length != valuesMap.size()) {
                logger.warning("Not all actual data has been loaded! Missing " + (tickerSymbols.length - valuesMap.size()) + " stock(s).");
            }

            for (Iterator<Map.Entry<String, CloseData>> it = m_closeDataMap.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, CloseData> entry = it.next();

                Double valueRef = valuesMap.get(entry.getKey());
                if (valueRef == null) {
                    logger.warning("Cannot load actual data for: " + entry.getKey() + "! This stock will not be used.");
                    it.remove();
                    continue;
                }

                entry.getValue().adjCloses[0] = valueRef;
            }

        } catch (IOException | NumberFormatException ex) {
            logger.warning("Failed to load actual data from google at once. Exception: " + ex.getMessage());
            logger.info("Loading one at a time...");
            for (Iterator<Map.Entry<String, CloseData>> it = m_closeDataMap.entrySet().iterator(); it.hasNext();) {
                CloseData closeData = it.next().getValue();
                String symbol = it.next().getKey();
                try {
                    closeData.adjCloses[0] = DataGetterActGoogle.readActualData(symbol);
                } catch (IOException | NumberFormatException ex2) {
                    logger.warning("Cannot load actual data for: " + symbol + ", exception: " + ex2.getMessage());
                    it.remove();
                }
            }
        }

        logger.info("Finished to load actual data");
    }

    public void CalculateIndicators() {
        logger.info("Starting to compute indicators");

        for (Map.Entry<String, CloseData> entry : m_closeDataMap.entrySet()) {
            CloseData value = entry.getValue();
            StockIndicatorsForNinety data90 = new StockIndicatorsForNinety();
            data90.sma200 = IndicatorCalculator.SMA(200, value.adjCloses);
            data90.sma5 = IndicatorCalculator.SMA(5, value.adjCloses);
            data90.rsi2 = IndicatorCalculator.RSI(value.adjCloses);
            data90.actValue = value.adjCloses[0];

            m_dataFor90Map.put(entry.getKey(), data90);
        }

        logger.info("Finished to compute indicators");
    }

    private List<TradeOrder> ProcessStocksToSellIntoOrders(List<HeldStock> stocksToSell) {
        List<TradeOrder> tradeOrders = new ArrayList<TradeOrder>();

        for (HeldStock heldStock : stocksToSell) {
            TradeOrder order = new TradeOrder();
            order.orderType = TradeOrder.OrderType.SELL;
            order.tickerSymbol = heldStock.tickerSymbol;
            order.position = heldStock.GetPosition();
            tradeOrders.add(order);

            StockIndicatorsForNinety stockIndicator = m_dataFor90Map.get(heldStock.tickerSymbol);
            if (stockIndicator == null) {
                logger.severe("Cannot find indicators for " + heldStock.tickerSymbol);
            }

            double profit = (stockIndicator.actValue - heldStock.GetAvgPrice()) * heldStock.GetPosition();
            logger.info("Selling stock '" + heldStock.tickerSymbol + "', position: " + order.position + ", total profit: " + profit + "$.");

            m_statusDataFor90.heldStocks.remove(heldStock.tickerSymbol);
            // TODO: check if sold and compare expected vs real price
        }

        return tradeOrders;
    }

    private TradeOrder ProcessStockToBuyIntoOrder(String stockToBuy, List<HeldStock> stocksToSell) {
        TradeOrder order = null;
        if (stockToBuy != null) {

            for (HeldStock soldStock : stocksToSell) {
                if (stockToBuy == soldStock.tickerSymbol) {
                    logger.info("Don't buy stock you just sold silly! " + stockToBuy);
                    stockToBuy = null;
                    break;
                }
            }

            StockIndicatorsForNinety stockIndicator = m_dataFor90Map.get(stockToBuy);
            if (stockIndicator == null) {
                logger.severe("Cannot find indicators for " + stockToBuy);
                //TODO: pruser
            }

            if (m_statusDataFor90.GetBoughtPortions() < 20) {
                logger.info("Buying new stock '" + stockToBuy + "'!");
                order = new TradeOrder();
                order.orderType = TradeOrder.OrderType.BUY;
                order.tickerSymbol = stockToBuy;
                order.position = (int) (m_statusDataFor90.GetOnePortionValue() / stockIndicator.actValue);

                HeldStock heldStock = new HeldStock();
                heldStock.tickerSymbol = stockToBuy;

                StockPurchase purchase = new StockPurchase();
                purchase.date = Timing.GetNYTimeNow();
                purchase.portions = 1;
                purchase.position = order.position;
                purchase.priceForOne = stockIndicator.actValue;

                heldStock.purchases.add(purchase);
                // TODO: check if bought and compare expected vs real price

                m_statusDataFor90.heldStocks.put(heldStock.tickerSymbol, heldStock);
            } else {
                logger.info("Positions are full at " + m_statusDataFor90.GetBoughtPortions() + "/20!");
            }
        }

        return order;
    }

    private List<TradeOrder> ProcessStocksToBuyMoreIntoOrders(List<HeldStock> stocksToBuyMore) {
        List<TradeOrder> tradeOrders = new ArrayList<TradeOrder>();

        Collections.sort(stocksToBuyMore, new Comparator<HeldStock>() {
            @Override
            public int compare(HeldStock stock1, HeldStock stock2) {
                StockIndicatorsForNinety stockIndicator1 = m_dataFor90Map.get(stock1.tickerSymbol);
                StockIndicatorsForNinety stockIndicator2 = m_dataFor90Map.get(stock2.tickerSymbol);
                if ((stockIndicator1 == null) || (stockIndicator2 == null)) {
                    logger.severe("Cannot find indicators for " + stock1.tickerSymbol + " and/or " + stock2.tickerSymbol);
                    //TODO: pruser
                }
                return Double.compare(stockIndicator1.rsi2, stockIndicator2.rsi2);
            }
        });

        for (HeldStock heldStock : stocksToBuyMore) {
            TradeOrder order = new TradeOrder();
            order.orderType = TradeOrder.OrderType.BUY;
            order.tickerSymbol = heldStock.tickerSymbol;

            StockIndicatorsForNinety stockIndicator = m_dataFor90Map.get(heldStock.tickerSymbol);
            if (stockIndicator == null) {
                logger.severe("Cannot find indicators for " + heldStock.tickerSymbol);
                //TODO: pruser
            }

            if (heldStock.GetPortions() < 10) {
                int newPortions = 0;
                switch (heldStock.GetPortions()) {
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
                        logger.severe("Bought stock '" + heldStock.tickerSymbol + "' has somehow " + heldStock.GetPortions() + " bought portions!!!");
                        continue;
                }

                if (m_statusDataFor90.GetBoughtPortions() + newPortions > 20) {
                    logger.info("Cannot buy " + newPortions + " more portions of '" + heldStock.tickerSymbol + "' because we currently hold " + m_statusDataFor90.GetBoughtPortions() + "/20 portions.");
                    continue;
                }

                order.position = (int) (m_statusDataFor90.GetOnePortionValue() * newPortions / stockIndicator.actValue);

                logger.info("Buying " + order.position + " more stock '" + heldStock.tickerSymbol + "' for " + (stockIndicator.actValue * order.position) + ". " + newPortions + " new portions. RSI2: " + stockIndicator.rsi2);
                tradeOrders.add(order);

                StockPurchase purchase = new StockPurchase();
                purchase.date = Timing.GetNYTimeNow();
                purchase.portions = newPortions;
                purchase.position = order.position;
                purchase.priceForOne = stockIndicator.actValue;

                heldStock.purchases.add(purchase);
                // TODO: check if bought and compare expected vs real price

            } else {
                logger.info("Stock '" + heldStock.tickerSymbol + "' is at max limit, cannot BUY more!");
            }
        }

        return tradeOrders;
    }

    String[] getSP100() {
        String[] tickers = {
            "AAPL", "ABBV", "ABT", "ACN", "AGN", "AIG", "ALL", "AMGN", "AMZN",
            "AXP", "BA", "BAC", "BIIB", "BK", "BLK", "BMY", "C", "CAT", "CELG", "CL", "CMCSA",
            "COF", "COP", "COST", "CSCO", "CVS", "CVX", "DD", "DHR", "DIS", "DOW", "DUK",
            "EMR", "EXC", "F", "FB", "FDX", "FOX", "GD", "GE",
            "GILD", "GM", "GOOG", "GS", "HAL", "HD", "HON", "IBM", "INTC",
            "JNJ", "JPM", "KMI", "KO", "LLY", "LMT", "LOW", "MA", "MCD", "MDLZ", "MDT",
            "MET", "MMM", "MO", "MON", "MRK", "MS", "MSFT", "NEE", "NKE", "ORCL", "OXY",
            "PCLN", "PEP", "PFE", "PG", "PM", "PYPL", "QCOM", "RTN", "SBUX", "SLB",
            "SO", "SPG", "T", "TGT", "TWX", "TXN", "UNH", "UNP", "UPS", "USB",
            "UTX", "V", "VZ", "WBA", "WFC", "WMT", "XOM"};

        return tickers;
    }

    public void BuyLoadedStatus() {
        for (HeldStock heldStock : m_statusDataFor90.heldStocks.values()) {
            TradeOrder order = new TradeOrder();
            order.orderType = TradeOrder.OrderType.BUY;
            order.tickerSymbol = heldStock.tickerSymbol;
            order.position = heldStock.GetPosition();
            m_IBcomm.PlaceOrder(order);
        }
    }

    public void SellAllPositions() {
        m_IBcomm.SellAllPositions();
    }

    private void RunNinety() {

        try {

            if (!CheckIfTradingDay()) {
                logger.info("Trading day finished");
                m_statusDataFor90.PrintStatus();
                return;
            }

            logger.info("RunNinety: Getting lock on hist data.");
            boolean acuiredInTime = histDataMutex.tryAcquire(1, TimeUnit.MINUTES);
            if (!acuiredInTime) {
                logger.severe("Acuire on hist data lock timed out!");
                return;
            }
            logger.info("RunNinety: Got lock on hist data.");

            UpdateDataWithActValues();
            CalculateIndicators();
            //TODO: run check

            logger.info("Starting Ninety strategy");

            m_statusDataFor90.PrintStatus();

            // Selling held stocks
            List<HeldStock> stocksToSell = Ninety.ComputeStocksToSell(m_statusDataFor90.heldStocks, m_dataFor90Map);
            List<TradeOrder> sellOrders = ProcessStocksToSellIntoOrders(stocksToSell);

            for (TradeOrder tradeOrder : sellOrders) {
                m_IBcomm.PlaceOrder(tradeOrder);
            }

            logger.info("Finished computing stocks to sell.");

            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // TODO: wait until done
            // Buying new stock
            String tickerToBuy = Ninety.ComputeStocksToBuy(m_statusDataFor90.heldStocks, m_dataFor90Map);
            TradeOrder buyOrder = ProcessStockToBuyIntoOrder(tickerToBuy, stocksToSell);
            m_IBcomm.PlaceOrder(buyOrder);

            // Buying more held stock
            List<HeldStock> stocksToBuyMore = Ninety.computeStocksToBuyMore(m_statusDataFor90.heldStocks, m_dataFor90Map);
            List<TradeOrder> buyMoreOrders = ProcessStocksToBuyMoreIntoOrders(stocksToBuyMore);

            for (TradeOrder tradeOrder : buyMoreOrders) {
                m_IBcomm.PlaceOrder(tradeOrder);
            }

            logger.info("Finished computing stocks to buy.");

            try {
                Thread.sleep(30000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // TODO: wait until done
            // TODO: check held positions
            m_statusDataFor90.SaveHeldPositionsToFile();

            m_statusDataFor90.PrintStatus();

            logger.info("Trading day finished");

            Start();
        } catch (InterruptedException ex) {
            Logger.getLogger(RunnerNinety.class.getName()).log(Level.SEVERE, null, ex);
        } finally {

            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            histDataMutex.release();
            logger.info("RunNinety: Released lock on hist data.");
            
            Start();
        }
    }
}
