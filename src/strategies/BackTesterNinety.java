/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import data.DataGetterHistYahoo;
import data.IndicatorCalculator;
import data.StockIndicatorsForNinety;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static strategies.StockDataForNinety.getSP100;
import tradingapp.TradeOrder;

/**
 *
 * @author Muhe
 */
public class BackTesterNinety {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    public static Map<String, double[]> LoadData(LocalDate startDate, LocalDate endDate) {

        Map<String, double[]> dataMap = new HashMap<>();

        for (String string : StockDataForNinety.getSP100()) {
            logger.info("Loading data for: " + string);
            
            List<Double> closeDataList = DataGetterHistYahoo.readRawAdjCloseData(startDate, endDate, string);
            List<Double> first199 = DataGetterHistYahoo.readRawAdjCloseData(startDate.minusDays(300), startDate.minusDays(1), string, 199);
            
            closeDataList.addAll(first199);
            
            double[] arr = closeDataList.stream().mapToDouble(Double::doubleValue).toArray();
            dataMap.put(string, arr);
        }
        return dataMap;
    }
    
    public static double RunTest(LocalDate startDate, LocalDate endDate) {
        Map<String, double[]> dataMap = LoadData(startDate, endDate);
        StatusDataForNinety statusData = new StatusDataForNinety();
        //statusData.moneyToInvest = 40000;
        double totalProfit = 0;
        
        long days = startDate.until(endDate, ChronoUnit.DAYS);
        
        logger.setLevel(Level.INFO);
        
        int size = dataMap.get("AAPL").length - 199;
        
        for (int dayInx = 0; dayInx < size; dayInx++) {
            
            logger.info("Starting to compute day " + dayInx + "/" + size + ".Profit so far = " + totalProfit);
            
            Map<String, StockIndicatorsForNinety> indicatorsMap = new HashMap<>(getSP100().length);
            
            for (String string : StockDataForNinety.getSP100()) {
                double[] values = dataMap.get(string);
                
                int testingDayInx = values.length - 200 - dayInx;
                double[] values200 = Arrays.copyOfRange(values, testingDayInx, testingDayInx + 200);
                
                StockIndicatorsForNinety data90 = new StockIndicatorsForNinety();
                data90.sma200 = IndicatorCalculator.SMA(200, values200);
                data90.sma5 = IndicatorCalculator.SMA(5, values200);
                data90.rsi2 = IndicatorCalculator.RSI(values200);
                data90.actValue = values200[0];

                indicatorsMap.put(string, data90);
            }
            
            List<TradeOrder> toSell = Ninety.ComputeStocksToSell(indicatorsMap, statusData);
            
            for (TradeOrder tradeOrder : toSell) {
                HeldStock held = statusData.heldStocks.get(tradeOrder.tickerSymbol);
                double profit = (tradeOrder.expectedPrice - held.GetAvgPrice()) * held.GetPosition();
                logger.info("Stock sold - profit: " + profit + ", " + tradeOrder.toString());

                statusData.heldStocks.remove(held.tickerSymbol);
                totalProfit += profit;
            }

            TradeOrder toBuy = Ninety.ComputeStocksToBuy(indicatorsMap, statusData, toSell);

            if (toBuy != null) {
                HeldStock held = new HeldStock();
                held.tickerSymbol = toBuy.tickerSymbol;

                StockPurchase purchase = new StockPurchase();
                //purchase.date = order.timestampFilled;
                purchase.portions = 1;
                purchase.position = toBuy.position;
                purchase.priceForOne = toBuy.expectedPrice;

                held.purchases.add(purchase);

                statusData.heldStocks.put(held.tickerSymbol, held);
                logger.info("New stock added: " + held.toString());
            }

            List<TradeOrder> toBuyMore = Ninety.computeStocksToBuyMore(indicatorsMap, statusData);
            
            for (TradeOrder tradeOrder : toBuyMore) {
                HeldStock held = statusData.heldStocks.get(tradeOrder.tickerSymbol);
                
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
                purchase.portions = newPortions;
                purchase.position = tradeOrder.position;
                purchase.priceForOne = tradeOrder.expectedPrice;

                held.purchases.add(purchase);
                
                logger.info("More stock bought - " + held.toString());
            }
        }
        
        logger.info("TestCompleted. Profit = " + totalProfit);

        return totalProfit;
    }
}
