/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import data.CloseData;
import data.DataGetterHistYahoo;
import data.IndicatorCalculator;
import data.StockIndicatorsForNinety;
import java.io.BufferedReader;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import static strategies.StockDataForNinety.getSP100;
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
import java.util.Iterator;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

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
            File inputFile = new File("backtestCache/_settings.xml");
            
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
            File file = new File("backtestCache/" + entry.getKey() + ".txt");
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
                    output.write(Double.toString(adjCloses[inx]));
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

        BufferedWriter output = null;
        try {            
            Element rootElement = new Element("Settings");
            Document doc = new Document(rootElement);
            rootElement.setAttribute("start", startDate.toString());
            rootElement.setAttribute("end", endDate.toString());

            XMLOutputter xmlOutput = new XMLOutputter();

            File fileSettings = new File("backtestCache/_settings.xml");
            fileSettings.createNewFile();
            FileOutputStream oFile = new FileOutputStream(fileSettings, false);

            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(doc, oFile);

        } catch (IOException ex) {
            Logger.getLogger(BackTesterNinety.class.getName()).log(Level.SEVERE, null, ex);
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

    private static Map<String, CloseData> LoadBacktestCache(LocalDate startDate, LocalDate endDate) {

        Map<String, CloseData> map = new HashMap<>();
        
        for (String ticker : StockDataForNinety.getSP100()) {
            FileReader file = null;
            try {
                file = new FileReader("backtestCache/" + ticker + ".txt");
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
                
                map.put(ticker, retData);
                
            } catch (FileNotFoundException ex) {
                CloseData data = LoadTickerDataFromYahoo(ticker, startDate, endDate);
                if (data == null) {
                    continue;
                }
                map.put(ticker, data);
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
    
    public static CloseData LoadTickerDataFromYahoo(String ticker, LocalDate startDate, LocalDate endDate) {
        logger.info("Loading data for: " + ticker);
            
        CloseData closeData = DataGetterHistYahoo.readRawAdjCloseData(startDate, endDate, ticker);
        CloseData first199 = DataGetterHistYahoo.readRawAdjCloseData(startDate.minusDays(300), startDate.minusDays(1), ticker, 199);
        
        if ((closeData == null) || (first199 == null)) {
            return null;
        }

        Stream<LocalDate> stream1 = Arrays.stream(closeData.dates);
        Stream<LocalDate> stream2 = Arrays.stream(first199.dates);
        LocalDate[] dates = Stream.concat(stream1, stream2).toArray(LocalDate[]::new);

        double[] closeValues = concat(closeData.adjCloses, first199.adjCloses);

        assert(dates.length == closeValues.length);

        CloseData data = new CloseData(dates.length);
        data.adjCloses = closeValues;
        data.dates = dates;
        
        return data;
    }

    public static Map<String, CloseData> LoadData(LocalDate startDate, LocalDate endDate) {

        Map<String, CloseData> dataMap = new HashMap<>();

        if (CheckBacktestSettingsInCache(startDate, endDate)) {
            dataMap = LoadBacktestCache(startDate, endDate);
        } else {
            for (String ticker : StockDataForNinety.getSP100()) {
                CloseData data = LoadTickerDataFromYahoo(ticker, startDate, endDate);
                if (data == null) {
                    continue;
                }

                dataMap.put(ticker, data);
            }
        }
        SaveLoadedData(dataMap, startDate, endDate);
        return dataMap;
    }
    
    private static class EquityInTime {
        LocalDate date;
        double profit;
    }
    
    public static double RunTest(LocalDate startDate, LocalDate endDate) {
        Map<String, CloseData> dataMap = LoadData(startDate, endDate);
        StatusDataForNinety statusData = new StatusDataForNinety();
        double totalProfit = 0;
        
        int totalSells = 0;
        int profitSells = 0;
        
        double highestProfit = 0;
        double highestDDproc = 0;
        double highestDD = 0;
        LocalDate dateOfHighestDD = LocalDate.MIN;
        int fees = 0;
        
        List<EquityInTime> equityList = new ArrayList<>();
        
        logger.info("Starting test from " + startDate.toString() + " to " + endDate.toString());
        
        logger.setLevel(Level.INFO);
        
        int size = dataMap.entrySet().iterator().next().getValue().adjCloses.length - 199;
        
        for (int dayInx = 0; dayInx < size; dayInx++) {
            int testingDayInx = size - 1 - dayInx;
            
            LocalDate date = dataMap.entrySet().iterator().next().getValue().dates[testingDayInx];
            logger.info("Starting to compute day " + date.toString() + ", index: " + dayInx + "/" + size + ". Profit so far = " + totalProfit);
            
            EquityInTime eq = new EquityInTime();
            eq.date = date;
            eq.profit = totalProfit;
            
            equityList.add(eq);
            
            Map<String, StockIndicatorsForNinety> indicatorsMap = new HashMap<>(getSP100().length);
            for (String string : StockDataForNinety.getSP100()) {
                CloseData data = dataMap.get(string);
                if (data == null) {
                    continue;
                }
                
                double[] values = data.adjCloses;
                
                if (!data.dates[testingDayInx].equals(date)) {
                    logger.severe("Dates does not equal for " + string + " date " + data.dates[testingDayInx].toString() + " vs expected " + date.toString());
                }
                
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

                profit -= 1;
                fees++;
                statusData.heldStocks.remove(held.tickerSymbol);
                totalProfit += profit;

                totalSells++;
                if (profit > 0) {
                    profitSells++;

                    if (highestProfit < totalProfit) {
                        highestProfit = totalProfit;
                    }
                } else {
                    double dd = ((highestProfit - totalProfit) / (40000 + highestProfit)) * 100;
                    
                    if (highestDDproc < dd) {
                        highestDD = highestProfit - totalProfit;
                        highestDDproc = dd;
                        dateOfHighestDD = date;
                    }
                }
            }

            int remainingPortions = 20 - statusData.GetBoughtPortions();
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
                remainingPortions--;
                
                totalProfit -= 1;
                fees++;
            }

            List<TradeOrder> toBuyMore = Ninety.computeStocksToBuyMore(indicatorsMap, statusData, remainingPortions);
            
            for (TradeOrder tradeOrder : toBuyMore) {
                HeldStock held = statusData.heldStocks.get(tradeOrder.tickerSymbol);
                
                int newPortions = Ninety.GetNewPortionsToBuy(held.GetPortions());
                if (newPortions == 0) {
                    logger.severe("Bought stock '" + held.tickerSymbol + "' has somehow " + held.GetPortions() + " bought portions!!!");
                    //TODO: dafuq?
                }

                StockPurchase purchase = new StockPurchase();
                purchase.portions = newPortions;
                purchase.position = tradeOrder.position;
                purchase.priceForOne = tradeOrder.expectedPrice;

                held.purchases.add(purchase);
                
                logger.info("More stock bought - " + held.toString());
                totalProfit -= 1;
                fees++;
            }
        }
        logger.setLevel(Level.INFO);
        logger.info("TestCompleted. Profit = " + totalProfit + ", succesful = " + (double)profitSells/(double)totalSells*100 + "%");
        logger.info("Highest DD = " + highestDD + "$, " + highestDDproc + "%, date = " + dateOfHighestDD.toString());
        logger.info("Paid on fees = " + fees + "$");
        
        SaveEquityToCsv(equityList);

        return totalProfit;
    }
  
    private static void SaveEquityToCsv(List<EquityInTime> equityList) {
        
        logger.info("Saving equity to CSV");
        
        File file = new File("backtestCache/_equity.csv");
        File directory = new File(file.getParentFile().getAbsolutePath());
        directory.mkdirs();
        BufferedWriter output = null;
        try {
            file.delete();
            file.createNewFile();
            output = new BufferedWriter(new FileWriter(file));

            for (EquityInTime equityInTime : equityList) {
                double profit = equityInTime.profit;
                LocalDate date = equityInTime.date;

                output.write(date.toString());
                output.write(":");
                output.write(Double.toString(profit));
                output.newLine();
            }

        } catch (IOException ex) {
            logger.warning("Cannot create equity CSV");
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
