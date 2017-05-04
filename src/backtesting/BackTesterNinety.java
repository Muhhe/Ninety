/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import communication.IBroker;
import communication.OrderStatus;
import data.CloseData;
import data.getters.IDataGetterHist;
import data.IndicatorCalculator;
import data.StockIndicatorsForNinety;
import data.TickersToTrade;
import java.io.BufferedReader;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import tradingapp.TradeOrder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import strategy90.Ninety;
import strategy90.StatusDataForNinety;
import test.BrokerNoIB;
import tradingapp.FilePaths;
import tradingapp.GlobalConfig;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class BackTesterNinety {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static double[] concat(double[] array1, double[] array2) {
        double[] array1and2 = new double[array1.length + array2.length];
        System.arraycopy(array1, 0, array1and2, 0, array1.length);
        System.arraycopy(array2, 0, array1and2, array1.length, array2.length);
        return array1and2;
    }
    
    public static boolean CheckBacktestSettingsInCache(LocalDate startDate, LocalDate endDate) {
        try {
            File inputFile = new File("backtest/cache/_settings.xml");
            
            if (!inputFile.exists()) {
                return false;
            }
            
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);

            Element rootElement = document.getRootElement();
            Attribute attStart = rootElement.getAttribute("start");
            LocalDate start = LocalDate.parse(attStart.getValue());
            Attribute attEnd = rootElement.getAttribute("end");
            LocalDate end = LocalDate.parse(attEnd.getValue());

            return startDate.isEqual(start) && endDate.isEqual(end);

        } catch (JDOMException e) {
            e.printStackTrace();
            logger.severe("Error in loading from XML: JDOMException.\r\n" + e);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.severe("Error in loading from XML: IOException.\r\n" + ioe);
        }

        return false;
    }

    public static void SaveLoadedData(Map<String, CloseData> dataMap, LocalDate startDate, LocalDate endDate) {
        for (Map.Entry<String, CloseData> entry : dataMap.entrySet()) {
            File file = new File("backtest/cache/" + entry.getKey() + ".txt");
            File directory = new File(file.getParentFile().getAbsolutePath());
            directory.mkdirs();
            BufferedWriter output = null;
            try {
                file.createNewFile();
                output = new BufferedWriter(new FileWriter(file));

                double[] adjCloses = entry.getValue().adjCloses;
                LocalDate[] dates = entry.getValue().dates;
                assert (adjCloses.length == dates.length);

                for (int inx = 0; inx < adjCloses.length; inx++) {
                    output.write(dates[inx].toString());
                    output.write(";");
                    output.write(TradeFormatter.toString(adjCloses[inx]));
                    output.newLine();
                }

            } catch (IOException ex) {
                logger.warning("Cannot create backtest cache data for: " + entry.getKey());
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    private static Map<String, CloseData> LoadBacktestCache(LocalDate startDate, LocalDate endDate) {

        Map<String, CloseData> map = new HashMap<>();
        
        int dataSize = 0;
        for (String ticker : TickersToTrade.GetTickers()) {
            FileReader file = null;
            try {
                file = new FileReader("backtest/cache/" + ticker + ".txt");
                BufferedReader br = new BufferedReader(file);
                
                List<LocalDate> dates = new ArrayList<>();
                List<Double> adjCloses = new ArrayList<>();
                String line;
                while ((line = br.readLine()) != null) {
                
                    String[] dateLine = line.split(";");

                    dates.add(LocalDate.parse(dateLine[0], DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    adjCloses.add(Double.parseDouble(dateLine[1]));
                }
                
                CloseData retData = new CloseData(0);

                retData.adjCloses = adjCloses.stream().mapToDouble(Double::doubleValue).toArray();

                retData.dates = new LocalDate[dates.size()];
                retData.dates = dates.toArray(retData.dates);
                

                if (retData.dates.length > dataSize) {
                    if (dataSize != 0) {
                        logger.warning("Data size increased for " + ticker);
                    }
                    dataSize = retData.dates.length;
                }

                map.put(ticker, retData);

            } catch (FileNotFoundException ex) {
                CloseData data = LoadTickerData(ticker, startDate, endDate);
                if (data == null) {
                    continue;
                }
                
                if (data.dates.length < dataSize) {
                    logger.warning("Data for " + ticker + " are not complete. Only " + data.dates.length + " out of " + dataSize + " loaded.");
                } else {
                    map.put(ticker, data);
                }
            } catch (IOException ex) {
                logger.severe("Error while reading " + ticker);
            } finally {
                try {
                    if (file != null) {
                        file.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(BackTesterNinety.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        return map;
    }
    
    public static CloseData LoadTickerData(String ticker, LocalDate startDate, LocalDate endDate) {
        CloseData data = null;
        for (IDataGetterHist getter : GlobalConfig.GetDataGettersHist()) {
            logger.info("Loading " + getter.getName() + " data for: " + ticker);

            CloseData closeData = getter.readAdjCloseData(startDate, endDate, ticker);
            CloseData first199 = getter.readAdjCloseData(startDate.minusDays(300), startDate.minusDays(1), ticker, 199, false);

            if ((closeData == null) || (first199 == null) || (first199.adjCloses.length != 199)) {
                logger.info("Failed: Loading " + getter.getName() + " data for: " + ticker);
                continue;
            }

            Stream<LocalDate> stream1 = Arrays.stream(closeData.dates);
            Stream<LocalDate> stream2 = Arrays.stream(first199.dates);
            LocalDate[] dates = Stream.concat(stream1, stream2).toArray(LocalDate[]::new);

            double[] closeValues = concat(closeData.adjCloses, first199.adjCloses);

            assert(dates.length == closeValues.length);

            data = new CloseData(dates.length);
            data.adjCloses = closeValues;
            data.dates = dates;
            
            break;
        }
        
        return data;
    }

    public static Map<String, CloseData> LoadData(LocalDate startDate, LocalDate endDate) {

        Map<String, CloseData> dataMap = new HashMap<>();
        
        int dataSize = 0;

        if (CheckBacktestSettingsInCache(startDate, endDate)) {
            dataMap = LoadBacktestCache(startDate, endDate);
            logger.log(BTLogLvl.BACKTEST, "Data loaded from cache.");
        } else {
            File dir = new File("backtest/cache/");
            for(File file: dir.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
            
            for (String ticker : TickersToTrade.GetTickers()) {
                CloseData data = LoadTickerData(ticker, startDate, endDate);
                if (data == null) {
                    continue;
                }

                if (data.dates.length > dataSize) {
                    if (dataSize != 0) {
                        logger.warning("Data size increased for " + ticker);
                    }
                    dataSize = data.dates.length;
                }

                if (data.dates.length < dataSize) {
                    logger.warning("Data for " + ticker + " are not complete. Only " + data.dates.length + " out of " + dataSize + " loaded.");
                    continue;
                }

                dataMap.put(ticker, data);
                logger.log(BTLogLvl.BACKTEST, "Loaded " + ticker);
            }
        }
        return dataMap;
    }

    public static Map<String, StockIndicatorsForNinety> CalulateIndicators(Map<String, CloseData> dataMap, int testingDayInx) {
        Map<String, StockIndicatorsForNinety> indicatorsMap = new HashMap<>(TickersToTrade.GetTickers().length);
        for (String ticker : dataMap.keySet()) {
            CloseData data = dataMap.get(ticker);

            double[] values200 = Arrays.copyOfRange(data.adjCloses, testingDayInx, testingDayInx + 200);

            StockIndicatorsForNinety data90 = new StockIndicatorsForNinety();
            data90.sma200 = IndicatorCalculator.SMA(200, values200);
            data90.sma5 = IndicatorCalculator.SMA(5, values200);
            data90.rsi2 = IndicatorCalculator.RSI(values200);
            data90.actValue = values200[0];

            indicatorsMap.put(ticker, data90);
        }
        
        return indicatorsMap;
    }

    public static double RunTest(BTSettings settings) {

        FilePaths.tradingStatusPathFileInput = "backtest/TradingStatus.xml";
        FilePaths.tradingStatusPathFileInput = "backtest/TradingStatus.xml";
        
        FilePaths.tradeLogDetailedPathFile = "backtest/TradeLogDetailed.txt";
        FilePaths.tradeLogPathFile = "backtest/TradeLog.csv";
        
        FilePaths.equityPathFile = "backtest/Equity.csv";
        
        try {
            File file = new File(FilePaths.tradeLogDetailedPathFile);
            file.delete();
            file = new File(FilePaths.tradeLogPathFile);
            file.delete();
            file = new File(FilePaths.equityPathFile);
            file.delete();
        } catch (Exception e) {
            logger.warning("Exception: " + e);
        }

        Map<String, CloseData> dataMap = LoadData(settings.startDate, settings.endDate);
        SaveLoadedData(dataMap, settings.startDate, settings.endDate);
        StatusDataForNinety statusData = new StatusDataForNinety();

        statusData.moneyToInvest = settings.capital * settings.leverage;
        statusData.currentCash = settings.capital;
        
        BTStatistics stats = new BTStatistics(settings.capital, settings.reinvest);
        IBroker broker = new BrokerNoIB();
        
        logger.log(BTLogLvl.BACKTEST, "Starting test from " + settings.startDate.toString() + " to " + settings.endDate.toString());
        logger.log(BTLogLvl.BACKTEST, "Number of used ticker - " + dataMap.size() + " out of " + TickersToTrade.GetTickers().length);
        
        int size = dataMap.entrySet().iterator().next().getValue().adjCloses.length - 199;
        
        for (int dayInx = 0; dayInx < size; dayInx++) {
            int testingDayInx = size - 1 - dayInx;
            
            LocalDate date = dataMap.entrySet().iterator().next().getValue().dates[testingDayInx];
            TradeTimer.SetToday(date);
            
            logger.log(BTLogLvl.BACKTEST, "Starting to compute day " + date.toString() + ", day: " + dayInx + "/" + (size-1) + ". Equity so far = " + TradeFormatter.toString(stats.equity));
            
            for (String ticker : dataMap.keySet()) {
                CloseData data = dataMap.get(ticker);
                if (data == null) {
                    logger.warning("Data for " + ticker + " are null.");
                    //dataMap.remove(ticker);
                    continue;
                }
                
                if (data.dates.length != (size + 199)) {
                    logger.warning("Data for ticker " + ticker + " have only " + data.dates.length + " entries out of " + (size + 199));
                    //dataMap.remove(ticker);
                    continue;
                }
                
                if (!data.dates[testingDayInx].equals(date)) {
                    logger.warning("Dates does not equal for " + ticker + " date " + data.dates[testingDayInx].toString() + " vs expected " + date.toString());
                }
            }
            
            stats.StartDay(date);
            
            Map<String, StockIndicatorsForNinety> indicatorsMap = CalulateIndicators(dataMap, testingDayInx);
                        
            List<TradeOrder> toSell = Ninety.ComputeStocksToSell(indicatorsMap, statusData);
 
            toSell.forEach((tradeOrder) -> {
                broker.PlaceOrder(tradeOrder);
            });

            for (OrderStatus orderStatus : broker.GetOrderStatuses().values()) {
                double profit = statusData.heldStocks.get(orderStatus.order.tickerSymbol).CalculateProfitIfSold(orderStatus.fillPrice);
                
                stats.AddSell(profit, orderStatus.filled, date);
                
                statusData.UpdateHeldByOrderStatus(orderStatus);
            }

            broker.clearOrderMaps();

            int remainingPortions = 20 - statusData.GetBoughtPortions();
            TradeOrder toBuy = Ninety.ComputeStocksToBuy(indicatorsMap, statusData, toSell);
            if (toBuy != null) {
                broker.PlaceOrder(toBuy);
                remainingPortions--;
            }
            
            List<TradeOrder> toBuyMore = Ninety.computeStocksToBuyMore(indicatorsMap, statusData, remainingPortions);
            for (TradeOrder tradeOrder : toBuyMore) {
                broker.PlaceOrder(tradeOrder);
            }
            
            for (OrderStatus orderStatus : broker.GetOrderStatuses().values()) {
                statusData.UpdateHeldByOrderStatus(orderStatus);
                
                stats.addBuy(orderStatus.filled);
            }
            broker.clearOrderMaps();
            
            //statusData.UpdateEquityFile();
            
            stats.EndDay();
            
            if (settings.reinvest) {
                statusData.moneyToInvest = statusData.currentCash * settings.leverage;
            }
        }
        
        stats.LogStats(settings);
        logger.log(BTLogLvl.BT_STATS, "Current cash = " + TradeFormatter.toString(statusData.currentCash) + "$");
        
        stats.SaveEquityToCsv();

        return stats.equity;
    }
}
