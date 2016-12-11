/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import communication.IBCommunication;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import tradingapp.Stock;
import tradingapp.TradeOrder;
import data.DataCalculatorForNinety;
import data.GoogleActDataGetter;
import data.HistoricData;
import data.IndicatorCalculator;
import data.YahooDataGetter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author Muhe
 */
public class Ninety {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private final IBCommunication m_comm = new IBCommunication();

    DataCalculatorForNinety calculatedStockData;
    public Map<String, HeldStock> heldStocks = new HashMap<>();
    public double moneyToInvest = 100000;

    public int GetBoughtPortions() {
        int boughtPortions = 0;
        for (HeldStock heldStock : heldStocks.values()) {
            boughtPortions += heldStock.boughtPortions;
        }

        if (boughtPortions > 20) {
            logger.severe("Bought portions more than 20!!!");
        }

        return boughtPortions;
    }

    private boolean IsAlreadyHeld(String tickerSymbol) {
        for (HeldStock heldStock : heldStocks.values()) {
            if (heldStock.tickerSymbol == tickerSymbol) {
                return true;
            }
        }

        return false;
    }

    public void Connect(int port) {
        m_comm.connect(port);
    }

    public void LoadStatus() {

        double onePortionPrice = moneyToInvest / 20;
        heldStocks.clear();

        HeldStock held = new HeldStock();
        held.tickerSymbol = "BIIB";
        held.boughtPortions = 10;

        StockPurchase purchase = new StockPurchase();
        purchase.portions = 1;
        purchase.priceForOne = 305.45;
        purchase.position = (int) (onePortionPrice / purchase.priceForOne) * purchase.portions;
        held.purchases.add(purchase);

        purchase = new StockPurchase();
        purchase.portions = 2;
        purchase.priceForOne = 304.20;
        purchase.position = (int) (onePortionPrice / purchase.priceForOne) * purchase.portions;
        held.purchases.add(purchase);

        purchase = new StockPurchase();
        purchase.portions = 3;
        purchase.priceForOne = 303.01;
        purchase.position = (int) (onePortionPrice / purchase.priceForOne) * purchase.portions;
        held.purchases.add(purchase);

        purchase = new StockPurchase();
        purchase.portions = 4;
        purchase.priceForOne = 294.42;
        purchase.position = (int) (onePortionPrice / purchase.priceForOne) * purchase.portions;
        held.purchases.add(purchase);

        heldStocks.put(held.tickerSymbol, held);

        held = new HeldStock();
        held.tickerSymbol = "RTN";
        held.boughtPortions = 1;

        purchase = new StockPurchase();
        purchase.portions = 1;
        purchase.priceForOne = 146.1;
        purchase.position = (int) (onePortionPrice / purchase.priceForOne);
        held.purchases.add(purchase);

        heldStocks.put(held.tickerSymbol, held);

        held = new HeldStock();
        held.tickerSymbol = "MRK";
        held.boughtPortions = 1;

        purchase = new StockPurchase();
        purchase.portions = 1;
        purchase.priceForOne = 59.88;
        purchase.position = (int) (onePortionPrice / purchase.priceForOne) * purchase.portions;
        held.purchases.add(purchase);

        heldStocks.put(held.tickerSymbol, held);

        /*held = new HeldStock();
        held.tickerSymbol = "AMZN";
        held.boughtPortions = 6;
        
        purchase = new StockPurchase();
        purchase.portions = 1;
        purchase.priceForOne = 751.39;
        purchase.position = (int)(onePortionPrice / purchase.priceForOne);
        held.purchases.add(purchase);
        
        purchase = new StockPurchase();
        purchase.portions = 2;
        purchase.priceForOne = 743.47;
        purchase.position = (int)(onePortionPrice / purchase.priceForOne);
        held.purchases.add(purchase);
        
        purchase = new StockPurchase();
        purchase.portions = 3;
        purchase.priceForOne = 740.21;
        purchase.position = (int)(onePortionPrice / purchase.priceForOne);
        held.purchases.add(purchase);

        heldStocks.add(held);*/
        held = new HeldStock();
        held.tickerSymbol = "CELG";
        held.boughtPortions = 3;

        purchase = new StockPurchase();
        purchase.portions = 1;
        purchase.priceForOne = 116.50;
        purchase.position = (int) (onePortionPrice / purchase.priceForOne) * purchase.portions;
        held.purchases.add(purchase);

        purchase = new StockPurchase();
        purchase.portions = 2;
        purchase.priceForOne = 112.56;
        purchase.position = (int) (onePortionPrice / purchase.priceForOne) * purchase.portions;
        held.purchases.add(purchase);

        heldStocks.put(held.tickerSymbol, held);
    }

    public void PrintStatus() {
        //List<String> lines = new ArrayList<>();

        logger.info("Status report: number of held stock: " + heldStocks.size());
        logger.info("Held portions: " + GetBoughtPortions() + "/20");

        for (HeldStock heldStock : heldStocks.values()) {
            logger.info(heldStock.toString());
        }
    }

    public void BuyLoadedStatus() {
        for (HeldStock heldStock : heldStocks.values()) {
            //List<TradeOrder> tradeOrders = new ArrayList<TradeOrder>();

            TradeOrder order = new TradeOrder();
            order.orderType = TradeOrder.OrderType.BUY;
            order.tickerSymbol = heldStock.tickerSymbol;
            order.stocksToTrade = heldStock.GetPosition();
            //tradeOrders.add(order);
            m_comm.PlaceOrder(order);
        }
    }

    public void CalculateHistoricStockData(Date upToDate) {
        if (calculatedStockData == null) {
            calculatedStockData = new DataCalculatorForNinety();
        }
        calculatedStockData.loadHistoric(upToDate);
    }

    public void UpdateStockDataValues(Date actDate) {
        if (calculatedStockData == null) {
            logger.warning("Historic data not initialized before updating with actul. Doing it now!");
            CalculateHistoricStockData(actDate);
        }

        calculatedStockData.updateDataWithActualValues(actDate);
    }

    public void CalculateIndicators() {
        if (calculatedStockData == null) {
            logger.warning("Historic data not initialized before updating with actual values. Doing it now!");
            CalculateHistoricStockData(new Date());
        }

        calculatedStockData.computeIndicators();
    }

    private boolean ComputeIfSellStock(HistoricData historicData) {
        return (historicData.adjClose[0] > historicData.sma5);
    }

    private double CalculateProfit(HeldStock heldStock) {
        double profit = 0;
        for (StockPurchase purchase : heldStock.purchases) {
            profit += (heldStock.actValue - purchase.priceForOne) * purchase.position;
        }

        return profit;
    }

    private double CalculateProfitPercent(HeldStock heldStock) {
        double profit = 0;
        double totalPrice = 0;
        for (StockPurchase purchase : heldStock.purchases) {
            profit += (heldStock.actValue - purchase.priceForOne) * purchase.position;
            totalPrice += purchase.priceForOne * purchase.position;
        }

        return (profit / totalPrice) * 100;
    }

    public List<HeldStock> ComputeStocksToSell() {
        
        logger.info("Started to compute stocks to sell.");

        List<HeldStock> stocksToSell = new ArrayList<HeldStock>();

        for (HeldStock heldStock : heldStocks.values()) {
            HistoricData historicData = calculatedStockData.stockMap.get(heldStock.tickerSymbol);
            if (historicData != null) {
                heldStock.actValue = historicData.adjClose[0];
                if (ComputeIfSellStock(historicData)) {
                    stocksToSell.add(heldStock);
                    double profit = CalculateProfitPercent(heldStock);
                    logger.info("SELL: " + heldStock.tickerSymbol + ", profit: " + profit + "%, actValue: " + historicData.adjClose[0] + ", origValue: " + heldStock.purchases.get(0).priceForOne + ", SMA5: " + historicData.sma5);
                }
            } else {
                logger.severe("ComputeStocksToSell: Data for bought stock '" + heldStock.tickerSymbol + "' not found!!!");
                PrintStatus();
                // TODO: co ted? Musi se odchytit uz pri sanity checku.
            }
        }

        return stocksToSell;
    }

    private boolean computeIfBuyMoreStock(HeldStock heldStock, HistoricData historicData) {
        return (historicData.adjClose[0] < heldStock.purchases.get(heldStock.purchases.size() - 1).priceForOne);
    }

    public List<HeldStock> computeStocksToBuyMore() {

        logger.info("Started to compute held stocks to buy more.");
        List<HeldStock> stocksToBuyMore = new ArrayList<HeldStock>();

        for (HeldStock heldStock : heldStocks.values()) {
            HistoricData historicData = calculatedStockData.stockMap.get(heldStock.tickerSymbol);
            if (historicData != null) {
                heldStock.actValue = historicData.adjClose[0];
                if (computeIfBuyMoreStock(heldStock, historicData)) {
                    stocksToBuyMore.add(heldStock);
                    logger.info("BUY MORE: " + heldStock.tickerSymbol + ", actValue: " + historicData.adjClose[0] + ", lastBuyValue: " + heldStock.purchases.get(heldStock.purchases.size() - 1).priceForOne + ", SMA5: " + historicData.sma5);
                }
            } else {
                logger.severe("ComputeStocksToBuyMore: Data for bought stock '" + heldStock.tickerSymbol + "' not found!!!");
                PrintStatus();
                // TODO: co ted? Musi se odchytit uz pri sanity checku.
            }
        }

        return stocksToBuyMore;
    }

    boolean computeBuyTicker(double actValue, double sma200, double sma5, double rsi) {

        if ((actValue < sma200) || (rsi > 10)) {
            return false;
        }

        return true;
    }

    public Stock ComputeStocksToBuy() {
        Stock stockToBuy = null;
        logger.info("Started to compute new stocks to buy.");

        for (HistoricData oneData : calculatedStockData.stockMap.values()) {

            if (heldStocks.containsKey(oneData.tickerSymbol)) { // we already hold this stock
                continue;
            }

            if (computeBuyTicker(oneData.adjClose[0], oneData.sma200, oneData.sma5, oneData.rsi2)) {
                if (IsAlreadyHeld(oneData.tickerSymbol)) {
                    continue;
                }

                logger.info("Possible BUY: " + oneData.tickerSymbol + ", actValue: " + oneData.adjClose[0] + ", SMA200: " + oneData.sma200 + ", SMA5: " + oneData.sma5 + ", RSI2: " + oneData.rsi2);

                if (stockToBuy == null) {
                    stockToBuy = new Stock();
                    stockToBuy.rsi2 = 100;
                }

                if (oneData.rsi2 < stockToBuy.rsi2) {
                    stockToBuy.tickerSymbol = oneData.tickerSymbol;
                    stockToBuy.value = oneData.adjClose[0];
                    stockToBuy.rsi2 = oneData.rsi2;
                }
            }
        }

        if (stockToBuy != null) {
            logger.info("FINAL BUY: " + stockToBuy.tickerSymbol + ", Value: " + stockToBuy.value + ", RSI2: " + stockToBuy.rsi2);
        }

        return stockToBuy;
    }

    public void RunStrategy() {
        Date date = new Date();
        CalculateHistoricStockData(date);

        logger.info("Loaded historic values: " + calculatedStockData.stockMap.size());

        date = new Date();
        UpdateStockDataValues(date);

        logger.info("Updated values: " + calculatedStockData.stockMap.size());

        CalculateIndicators();

        List<TradeOrder> tradeOrders = new ArrayList<TradeOrder>();

        List<HeldStock> stocksToSell = ComputeStocksToSell();
        for (HeldStock heldStock : stocksToSell) {
            TradeOrder order = new TradeOrder();
            order.orderType = TradeOrder.OrderType.SELL;
            order.tickerSymbol = heldStock.tickerSymbol;
            order.stocksToTrade = heldStock.GetPosition();
            tradeOrders.add(order);

            double profit = (heldStock.actValue - heldStock.GetAvgPrice()) * heldStock.GetPosition();
            logger.info("Selling stock '" + heldStock.tickerSymbol + "', position: " + order.stocksToTrade + ", total profit: " + profit + "$.");

            heldStocks.remove(heldStock.tickerSymbol);
        }

        for (TradeOrder tradeOrder : tradeOrders) {
            m_comm.PlaceOrder(tradeOrder);
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        tradeOrders.clear();

        Stock stockToBuy = ComputeStocksToBuy();
        if (stockToBuy != null) {

            for (HeldStock soldStock : stocksToSell) { // Zamezit aby to najednou prodalo a koupilo tu samou akcii !!!!
                if (stockToBuy.tickerSymbol == soldStock.tickerSymbol) {
                    logger.info("Don't buy stock you just sold silly! " + stockToBuy.tickerSymbol);
                    stockToBuy = null;
                    break;
                }
            }

            if (GetBoughtPortions() < 20) {
                logger.info("Buying new stock '" + stockToBuy.tickerSymbol + "'!");
                TradeOrder order = new TradeOrder();
                order.orderType = TradeOrder.OrderType.BUY;
                order.tickerSymbol = stockToBuy.tickerSymbol;
                order.limitTotalMoney = moneyToInvest / 20;
                order.stocksToTrade = (int) (order.limitTotalMoney / stockToBuy.value);

                tradeOrders.add(order);

                HeldStock heldStock = new HeldStock();
                heldStock.actValue = stockToBuy.value;
                heldStock.boughtPortions = 1;
                heldStock.rsi2 = stockToBuy.rsi2;
                heldStock.tickerSymbol = stockToBuy.tickerSymbol;

                StockPurchase purchase = new StockPurchase();
                purchase.date = new Date();
                purchase.portions = 1;
                purchase.position = order.stocksToTrade;
                purchase.priceForOne = heldStock.actValue;

                heldStock.purchases.add(purchase);

                heldStocks.put(heldStock.tickerSymbol, heldStock);
            } else {
                logger.info("Positions are full at " + GetBoughtPortions() + "/20!");
            }
        }

        for (TradeOrder tradeOrder : tradeOrders) {
            m_comm.PlaceOrder(tradeOrder);
        }
        tradeOrders.clear();

        List<HeldStock> stocksToBuyMore = computeStocksToBuyMore();
        Collections.sort(stocksToBuyMore, new Comparator<HeldStock>() {
            @Override
            public int compare(HeldStock o1, HeldStock o2) {
                return Double.compare(o1.rsi2, o2.rsi2);
            }
        });

        for (HeldStock heldStock : stocksToBuyMore) {
            TradeOrder order = new TradeOrder();
            order.orderType = TradeOrder.OrderType.BUY;
            order.tickerSymbol = heldStock.tickerSymbol;

            double onePieceOfMoney = moneyToInvest / 20;

            if (heldStock.boughtPortions < 10) {
                int newPortions = 0;
                switch (heldStock.boughtPortions) {
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
                        logger.severe("Bought stock '" + heldStock.tickerSymbol + "' has somehow " + heldStock.boughtPortions + " bought portions!!!");
                        continue;
                }

                if (GetBoughtPortions() + newPortions > 20) {
                    logger.info("Cannot buy " + newPortions + " more portions of '" + heldStock.tickerSymbol + "' because we currently hold " + GetBoughtPortions() + "/20 portions.");
                }

                order.limitTotalMoney = onePieceOfMoney * newPortions;
                order.stocksToTrade = (int) (order.limitTotalMoney / heldStock.actValue);

                logger.info("Buying " + order.stocksToTrade + " more stock '" + heldStock.tickerSymbol + "' for " + (heldStock.actValue * order.stocksToTrade) + ". " + newPortions + " new portions. RSI2: " + heldStock.rsi2);
                tradeOrders.add(order);

                heldStock.boughtPortions += newPortions;

                StockPurchase purchase = new StockPurchase();
                purchase.date = new Date();
                purchase.portions = newPortions;
                purchase.position = order.stocksToTrade;
                purchase.priceForOne = heldStock.actValue;

                heldStock.purchases.add(purchase);

            } else {
                logger.info("Stock '" + heldStock.tickerSymbol + "' is at max limit, cannot BUY more!");
            }
        }

        for (TradeOrder tradeOrder : tradeOrders) {
            m_comm.PlaceOrder(tradeOrder);
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        tradeOrders.clear();

        SaveHeldPositionsToFile();
    }

    public void SellAllPositions() {
        m_comm.SellAllPositions();
    }

    public void SaveHeldPositionsToFile() {
        try {
            List<String> lines = new ArrayList<>();

            lines.add("Number of held stock:" + heldStocks.size());

            for (HeldStock heldStock : heldStocks.values()) {
                lines.add(heldStock.toString());
            }

            Path file = Paths.get("HeldPositions.txt");
            Files.write(file, lines, Charset.forName("UTF-8"));

        } catch (IOException ex) {
            logger.severe("Failed to create file: " + ex.getMessage());
        }
    }

    public void ReadHeldPositions() {

        heldStocks.clear();

        try {
            Path file = Paths.get("HeldPositions.txt");
            List<String> allLines = Files.readAllLines(file);

            Iterator<String> iterator = allLines.iterator();
            String next = iterator.next();
            String[] split = next.split(":");
            int count = Integer.parseInt(split[1]);

            iterator.remove();

            for (int i = 0; i < count; i++) {
                HeldStock held = new HeldStock();
                held.LoadFromString(allLines);
                heldStocks.put(held.tickerSymbol, held);
            }

        } catch (IOException ex) {
            logger.severe("Failed to create file: " + ex.getMessage());
        }
    }
}
