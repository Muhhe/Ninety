/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import data.StockIndicatorsForNinety;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;

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

    public static List<HeldStock> ComputeStocksToSell(Map<String, HeldStock> heldStocks, Map<String, StockIndicatorsForNinety> dataFor90Map) {
        
        logger.info("Started to compute stocks to sell.");

        List<HeldStock> stocksToSell = new ArrayList<HeldStock>();

        for (HeldStock heldStock : heldStocks.values()) {
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

        return stocksToSell;
    }

    private static boolean computeIfBuyMoreStock(HeldStock heldStock, double actValue) {
        return (actValue < heldStock.purchases.get(heldStock.purchases.size() - 1).priceForOne);
    }

    public static List<HeldStock> computeStocksToBuyMore(Map<String, HeldStock> heldStocks, Map<String, StockIndicatorsForNinety> dataFor90Map) {

        logger.info("Started to compute held stocks to buy more.");
        List<HeldStock> stocksToBuyMore = new ArrayList<HeldStock>();

        for (HeldStock heldStock : heldStocks.values()) {
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

        return stocksToBuyMore;
    }

    private static boolean computeBuyTicker(StockIndicatorsForNinety tickerIndicators) {

        if ((tickerIndicators.actValue < tickerIndicators.sma200) || (tickerIndicators.rsi2 > 10)) {
            return false;
        }

        return true;
    }

    public static String ComputeStocksToBuy(Map<String, HeldStock> heldStocks, Map<String, StockIndicatorsForNinety> dataFor90Map) {
        String stockToBuy = null;
        double rsi2ToBuy = 100;
        logger.info("Started to compute new stocks to buy.");
        
        for (Map.Entry<String, StockIndicatorsForNinety> entry : dataFor90Map.entrySet()) {
            String tickerSymbol = entry.getKey();
            StockIndicatorsForNinety tickerIndicators = entry.getValue();
            
            if (heldStocks.containsKey(tickerSymbol)) { // we already hold this stock
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

        return stockToBuy;
    }
}
