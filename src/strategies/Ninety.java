/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import data.StockIndicatorsForNinety;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import tradingapp.TradingTimer;
import tradingapp.TradeOrder;

/**
 *
 * @author Muhe
 */
public class Ninety {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private static boolean ComputeIfSellStock(StockIndicatorsForNinety data) {
        return (data.actValue > data.sma5);
    }

    private static double CalculateProfitPercent(HeldStock heldStock, double actValue) {
        double profit = 0;
        double totalPrice = 0;
        for (StockPurchase purchase : heldStock.purchases) {
            profit += (actValue - purchase.priceForOne) * purchase.position;
            totalPrice += purchase.priceForOne * purchase.position;
        }

        return (profit / totalPrice) * 100;
    }

    public static List<TradeOrder> ComputeStocksToSell(Map<String, StockIndicatorsForNinety> dataFor90Map, StatusDataForNinety statusDataFor90) {
        
        logger.info("Started to compute stocks to sell.");

        List<HeldStock> stocksToSell = new ArrayList<HeldStock>();

        for (HeldStock heldStock : statusDataFor90.heldStocks.values()) {
            StockIndicatorsForNinety tickerIndicators = dataFor90Map.get(heldStock.tickerSymbol);
            if (tickerIndicators != null) {
                if (ComputeIfSellStock(tickerIndicators)) {
                    stocksToSell.add(heldStock);
                    double profit = CalculateProfitPercent(heldStock, tickerIndicators.actValue);
                    logger.info("SELL: " + heldStock.tickerSymbol + ", profit: " + profit + "%, actValue: " + tickerIndicators.actValue + ", SMA5: " + tickerIndicators.sma5);
                }
            } else {
                logger.severe("ComputeStocksToSell: Data for bought stock '" + heldStock.tickerSymbol + "' not found!!!");
                //PrintStatus();
                // TODO: co ted? Musi se odchytit uz pri sanity checku.
            }
        }

        return ProcessStocksToSellIntoOrders(stocksToSell, dataFor90Map, statusDataFor90);
    }

    private static boolean computeIfBuyMoreStock(HeldStock heldStock, double actValue) {
        return (actValue < heldStock.purchases.get(heldStock.purchases.size() - 1).priceForOne);
    }

    public static List<TradeOrder> computeStocksToBuyMore(Map<String, StockIndicatorsForNinety> dataFor90Map, StatusDataForNinety statusDataFor90) {

        logger.info("Started to compute held stocks to buy more.");
        List<HeldStock> stocksToBuyMore = new ArrayList<HeldStock>();

        for (HeldStock heldStock : statusDataFor90.heldStocks.values()) {
            StockIndicatorsForNinety tickerIndicators = dataFor90Map.get(heldStock.tickerSymbol);
            if (tickerIndicators != null) {
                if (computeIfBuyMoreStock(heldStock, tickerIndicators.actValue)) {
                    stocksToBuyMore.add(heldStock);
                    logger.info("BUY MORE: " + heldStock.tickerSymbol + ", actValue: " + tickerIndicators.actValue + ", lastBuyValue: " + heldStock.purchases.get(heldStock.purchases.size() - 1).priceForOne + ", SMA5: " + tickerIndicators.sma5);
                }
            } else {
                logger.severe("ComputeStocksToBuyMore: Data for bought stock '" + heldStock.tickerSymbol + "' not found!!!");
                //PrintStatus();
                // TODO: co ted? Musi se odchytit uz pri sanity checku.
            }
        }

        return ProcessStocksToBuyMoreIntoOrders(stocksToBuyMore, dataFor90Map, statusDataFor90);
    }

    private static boolean computeBuyTicker(StockIndicatorsForNinety tickerIndicators) {

        if ((tickerIndicators.actValue < tickerIndicators.sma200) || (tickerIndicators.rsi2 > 10)) {
            return false;
        }

        return true;
    }

    public static TradeOrder ComputeStocksToBuy(Map<String, StockIndicatorsForNinety> dataFor90Map, StatusDataForNinety statusDataFor90, List<TradeOrder> recentlySoldStocks) {
        String stockToBuy = null;
        double rsi2ToBuy = 100;
        logger.info("Started to compute new stocks to buy.");
        
        for (Map.Entry<String, StockIndicatorsForNinety> entry : dataFor90Map.entrySet()) {
            String tickerSymbol = entry.getKey();
            StockIndicatorsForNinety tickerIndicators = entry.getValue();
            
            if (statusDataFor90.heldStocks.containsKey(tickerSymbol)) { // we already hold this stock
                continue;
            }

            if (computeBuyTicker(tickerIndicators)) {

                logger.info("Possible BUY: " + tickerSymbol + ", actValue: " + tickerIndicators.actValue + ", SMA200: " + tickerIndicators.sma200 + ", SMA5: " + tickerIndicators.sma5 + ", RSI2: " + tickerIndicators.rsi2);

                if (stockToBuy == null) {
                    stockToBuy = new String();
                }

                if (tickerIndicators.rsi2 < rsi2ToBuy) {
                    stockToBuy = tickerSymbol;
                    rsi2ToBuy = tickerIndicators.rsi2;
                }
            }
        }

        if (stockToBuy != null) {
            logger.info("FINAL BUY: " + stockToBuy + ", RSI2: " + rsi2ToBuy);
        }

        return ProcessStockToBuyIntoOrder(stockToBuy, recentlySoldStocks, dataFor90Map, statusDataFor90);
    }
    

    private static List<TradeOrder> ProcessStocksToSellIntoOrders(List<HeldStock> stocksToSell, Map<String, StockIndicatorsForNinety> dataFor90Map, StatusDataForNinety statusDataFor90) {
        List<TradeOrder> tradeOrders = new ArrayList<TradeOrder>();

        for (HeldStock heldStock : stocksToSell) {
            StockIndicatorsForNinety stockIndicator = dataFor90Map.get(heldStock.tickerSymbol);
            if (stockIndicator == null) {
                logger.severe("Cannot find indicators for " + heldStock.tickerSymbol);
                continue;
            }
            
            TradeOrder order = new TradeOrder();
            order.orderType = TradeOrder.OrderType.SELL;
            order.tickerSymbol = heldStock.tickerSymbol;
            order.position = heldStock.GetPosition();
            order.expectedPrice = stockIndicator.actValue;
            tradeOrders.add(order);

            logger.info("Selling stock '" + heldStock.tickerSymbol + "', position: " + order.position);
            //statusDataFor90.heldStocks.remove(heldStock.tickerSymbol);
            // TODO: check if sold and compare expected vs real price
        }

        return tradeOrders;
    }

    private static TradeOrder ProcessStockToBuyIntoOrder(String stockToBuy, List<TradeOrder> recentlySoldStocks, Map<String, StockIndicatorsForNinety> dataFor90Map, StatusDataForNinety statusDataFor90) {
        TradeOrder order = null;
        if (stockToBuy != null) {

            for (TradeOrder soldStock : recentlySoldStocks) {
                if (stockToBuy == soldStock.tickerSymbol) {
                    logger.info("Don't buy stock you just sold silly! " + stockToBuy);
                    stockToBuy = null;
                    break;
                }
            }

            StockIndicatorsForNinety stockIndicator = dataFor90Map.get(stockToBuy);
            if (stockIndicator == null) {
                logger.severe("Cannot find indicators for " + stockToBuy);
                return null;
                //TODO: pruser
            }

            if (statusDataFor90.GetBoughtPortions() < 20) {
                logger.info("Buying new stock '" + stockToBuy + "'!");
                order = new TradeOrder();
                order.orderType = TradeOrder.OrderType.BUY;
                order.tickerSymbol = stockToBuy;
                order.position = (int) (statusDataFor90.GetOnePortionValue() / stockIndicator.actValue);
                order.expectedPrice = stockIndicator.actValue;

                /*HeldStock heldStock = new HeldStock();
                heldStock.tickerSymbol = stockToBuy;

                StockPurchase purchase = new StockPurchase();
                purchase.date = TradingTimer.GetNYTimeNow();
                purchase.portions = 1;
                purchase.position = order.position;
                purchase.priceForOne = stockIndicator.actValue;

                heldStock.purchases.add(purchase);
                // TODO: check if bought and compare expected vs real price

                statusDataFor90.heldStocks.put(heldStock.tickerSymbol, heldStock);*/
            } else {
                logger.info("Positions are full at " + statusDataFor90.GetBoughtPortions() + "/20!");
            }
        }

        return order;
    }

    private static List<TradeOrder> ProcessStocksToBuyMoreIntoOrders(List<HeldStock> stocksToBuyMore, Map<String, StockIndicatorsForNinety> dataFor90Map, StatusDataForNinety statusDataFor90) {
        List<TradeOrder> tradeOrders = new ArrayList<TradeOrder>();

        Collections.sort(stocksToBuyMore, new Comparator<HeldStock>() {
            @Override
            public int compare(HeldStock stock1, HeldStock stock2) {
                StockIndicatorsForNinety stockIndicator1 = dataFor90Map.get(stock1.tickerSymbol);
                StockIndicatorsForNinety stockIndicator2 = dataFor90Map.get(stock2.tickerSymbol);
                if ((stockIndicator1 == null) || (stockIndicator2 == null)) {
                    logger.severe("Cannot find indicators for " + stock1.tickerSymbol + " and/or " + stock2.tickerSymbol);
                    return -1;
                    //TODO: pruser
                }
                return Double.compare(stockIndicator1.rsi2, stockIndicator2.rsi2);
            }
        });

        for (HeldStock heldStock : stocksToBuyMore) {
            TradeOrder order = new TradeOrder();
            order.orderType = TradeOrder.OrderType.BUY;
            order.tickerSymbol = heldStock.tickerSymbol;

            StockIndicatorsForNinety stockIndicator = dataFor90Map.get(heldStock.tickerSymbol);
            if (stockIndicator == null) {
                logger.severe("Cannot find indicators for " + heldStock.tickerSymbol);
                continue;
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

                if (statusDataFor90.GetBoughtPortions() + newPortions > 20) {
                    logger.info("Cannot buy " + newPortions + " more portions of '" + heldStock.tickerSymbol + "' because we currently hold " + statusDataFor90.GetBoughtPortions() + "/20 portions.");
                    continue;
                }

                order.position = (int) (statusDataFor90.GetOnePortionValue() * newPortions / stockIndicator.actValue);
                order.expectedPrice = stockIndicator.actValue;

                logger.info("Buying " + order.position + " more stock '" + heldStock.tickerSymbol + "' for " + (stockIndicator.actValue * order.position) + ". " + newPortions + " new portions. RSI2: " + stockIndicator.rsi2);
                /*tradeOrders.add(order);

                StockPurchase purchase = new StockPurchase();
                purchase.date = TradingTimer.GetNYTimeNow();
                purchase.portions = newPortions;
                purchase.position = order.position;
                purchase.priceForOne = stockIndicator.actValue;

                heldStock.purchases.add(purchase);
                // TODO: check if bought and compare expected vs real price*/

            } else {
                logger.info("Stock '" + heldStock.tickerSymbol + "' is at max limit, cannot BUY more!");
            }
        }

        return tradeOrders;
    }
}
