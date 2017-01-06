/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import data.CloseData;
import data.DataGetterActGoogle;
import data.DataGetterHistYahoo;
import data.IndicatorCalculator;
import data.StockIndicatorsForNinety;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import tradingapp.TradingTimer;

/**
 *
 * @author Muhe
 */
public class StockDataForNinety {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public Map<String, CloseData> closeDataMap = new HashMap<>(getSP100().length);
    public Map<String, StockIndicatorsForNinety> indicatorsMap = new HashMap<>(getSP100().length);

    public final Semaphore histDataMutex = new Semaphore(1);

    String[] getSP100() {
        String[] tickers = {
            "AAPL", "ABBV", "ABT", "ACN", "AGN", "AIG", "ALL", "AMGN", "AMZN",
            "AXP", "BA", "BAC", "BIIB", "BK", "BLK", "BMY", "C", "CAT", "CELG", "CL", "CMCSA",
            "COF", "COP", "COST", "CSCO", "CVS", "CVX", "DD", "DHR", "DIS", "DOW", "DUK",
            "EMR", "EXC", "F", "FB", "FDX", "FOX", "GD", "GE",
            "GILD", "GM", "GOOG", "GS", "HAL", "HD", "HON", "IBM", "INTC",
            "JNJ", "JPM", "KMI", "KO", "LLY", "LMT", "LOW", "MA", "MCD", "MDLZ", "MDT",
            "MET", "MMM", "MO", "MON", "MRK", "MS", "MSFT", "NKE", "ORCL", "OXY",
            "PCLN", "PEP", "PFE", "PG", "PM", "PYPL", "QCOM", "RTN", "SBUX", "SLB",
            "SO", "SPG", "T", "TGT", "TWX", "TXN", "UNH", "UNP", "UPS", "USB",
            "UTX", "V", "VZ", "WBA", "WFC", "WMT", "XOM"}; //"NEE" - blbe se nacita z YAHOO

        return tickers;
    }

    public void PrepareHistData() {

        try {
            logger.fine("PrepareHistData: Getting lock on hist data.");
            histDataMutex.acquire();
            logger.info("Starting to load historic data");
            String[] tickers = getSP100();
            for (String ticker : tickers) {
                CloseData data = DataGetterHistYahoo.readData(LocalDate.now(), 200, ticker);
                closeDataMap.put(ticker, data);
            }

            logger.info("Finished to load historic data");

        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
            logger.info("Thread interuppted: " + ex);
        } finally {
            histDataMutex.release();
            logger.fine("PrepareHistData: Released lock on hist data.");
        }
    }

    public void UpdateDataWithActValues(TradingTimer timer) {
        if (!timer.IsTradingDay(LocalDate.now())) {
            logger.fine("Today is not a trading day. Cannot update with actual values.");
            return;
        }
        
        try {
            logger.fine("UpdateDataWithActValues: Getting lock on hist data.");
            histDataMutex.acquire();

            if (closeDataMap.isEmpty()) {
                logger.severe("updateDataWithActualValues - stockMap.isEmpty");
                return;
            }

            logger.info("Starting to load actual data");

            String[] tickerSymbols = getSP100();
            Map<String, Double> valuesMap = DataGetterActGoogle.readActualData(tickerSymbols);

            if (tickerSymbols.length != valuesMap.size()) {
                logger.warning("Not all actual data has been loaded! Missing " + (tickerSymbols.length - valuesMap.size()) + " stock(s).");
            }

            for (Iterator<Map.Entry<String, CloseData>> it = closeDataMap.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, CloseData> entry = it.next();

                Double valueRef = valuesMap.get(entry.getKey());
                if (valueRef == null) {
                    logger.warning("Cannot load actual data for: " + entry.getKey() + "! This stock will not be used.");
                    it.remove();
                    continue;
                }

                entry.getValue().adjCloses[0] = valueRef;
                entry.getValue().dates[0] = LocalDate.now();
            }

        } catch (IOException | NumberFormatException ex) {
            logger.warning("Failed to load actual data from google at once. Exception: " + ex.getMessage());
            logger.info("Loading one at a time...");
            for (Iterator<Map.Entry<String, CloseData>> it = closeDataMap.entrySet().iterator(); it.hasNext();) {
                CloseData closeData = it.next().getValue();
                String symbol = it.next().getKey();
                try {
                    closeData.adjCloses[0] = DataGetterActGoogle.readActualData(symbol);
                    closeData.dates[0] = LocalDate.now();
                } catch (IOException | NumberFormatException ex2) {
                    logger.warning("Cannot load actual data for: " + symbol + ", exception: " + ex2.getMessage());
                    it.remove();
                }
            }
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
            logger.info("Thread interuppted: " + ex);
        } finally {
            histDataMutex.release();
            logger.fine("UpdateDataWithActValues: Released lock on hist data.");
            logger.info("Finished to load actual data");
            
            SaveHistDataToFiles();
        }
    }

    public void CalculateIndicators() {
        try {
            logger.fine("CalculateIndicators: Getting lock on hist data.");
            histDataMutex.acquire();

            logger.info("Starting to compute indicators");
            for (Map.Entry<String, CloseData> entry : closeDataMap.entrySet()) {
                CloseData value = entry.getValue();
                StockIndicatorsForNinety data90 = new StockIndicatorsForNinety();
                data90.sma200 = IndicatorCalculator.SMA(200, value.adjCloses);
                data90.sma5 = IndicatorCalculator.SMA(5, value.adjCloses);
                data90.rsi2 = IndicatorCalculator.RSI(value.adjCloses);
                data90.actValue = value.adjCloses[0];

                indicatorsMap.put(entry.getKey(), data90);
            }

        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
            logger.info("Thread interuppted: " + ex);
        } finally {
            histDataMutex.release();
            logger.fine("CalculateIndicators: Released lock on hist data.");
            logger.info("Finished to compute indicators");
            
            SaveStockIndicatorsToFiles();
            SaveIndicatorsToCSVFile();
        }
    }

    public void SaveHistDataToFiles() {
        LocalDate today = LocalDate.now();
        String todayString = today.toString();
        for (Map.Entry<String, CloseData> entry : closeDataMap.entrySet()) {
            File file = new File("dataLog/" + todayString + "/Historic/" + entry.getKey() + ".txt");
            File directory = new File(file.getParentFile().getAbsolutePath());
            directory.mkdirs();
            BufferedWriter output = null;
            try {
                file.createNewFile();
                output = new BufferedWriter(new FileWriter(file));
                
                double[] adjCloses = entry.getValue().adjCloses;
                LocalDate[] dates = entry.getValue().dates;
                assert(adjCloses.length == dates.length);
                
                for (int inx = 0; inx < adjCloses.length; inx++) {
                    output.write(dates[inx].toString());
                    output.write(";");
                    output.write(Double.toString(adjCloses[inx]));
                    output.newLine();
                }

            } catch (IOException ex) {
                logger.warning("Cannot create historic data log for: " + entry.getKey());
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
    
    public void SaveStockIndicatorsToFiles() {
        LocalDate today = LocalDate.now();
        String todayString = today.toString();
        for (Map.Entry<String, StockIndicatorsForNinety> entry : indicatorsMap.entrySet()) {
            File file = new File("dataLog/" + todayString + "/Indicators/" + entry.getKey() + ".txt");
            File directory = new File(file.getParentFile().getAbsolutePath());
            directory.mkdirs();
            BufferedWriter output = null;
            try {
                file.createNewFile();
                output = new BufferedWriter(new FileWriter(file));
                        
                StockIndicatorsForNinety indicators = entry.getValue();
                output.write("ActValue: " + Double.toString(indicators.actValue));
                output.newLine();
                output.write("SMA200: " + Double.toString(indicators.sma200));
                output.newLine();
                output.write("SMA5: " + Double.toString(indicators.sma5));
                output.newLine();
                output.write("RSI2: " + Double.toString(indicators.rsi2));
                
            } catch (IOException ex) {
                logger.warning("Cannot create indicators log for: " + entry.getKey());
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
    
    public void SaveIndicatorsToCSVFile() {
        String todayString = LocalDate.now().toString();
        File file = new File("dataLog/" + todayString + "/indicators.csv");
        File directory = new File(file.getParentFile().getAbsolutePath());
        directory.mkdirs();
        BufferedWriter output = null;
        try {
            file.createNewFile();
            output = new BufferedWriter(new FileWriter(file));

            output.write("Ticker;ActValue;SMA200;SMA5;RSI2");
            for (Map.Entry<String, StockIndicatorsForNinety> entry : indicatorsMap.entrySet()) {
                StockIndicatorsForNinety indicators = entry.getValue();
                output.newLine();
                output.write(entry.getKey());
                output.append(';');
                output.write(Double.toString(indicators.actValue));
                output.append(';');
                output.write(Double.toString(indicators.sma200));
                output.append(';');
                output.write(Double.toString(indicators.sma5));
                output.append(';');
                output.write(Double.toString(indicators.rsi2));
            }
        } catch (IOException ex) {
            logger.warning("Cannot create indicator CSV  log file.");
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

    // TODO: udelat z timeru singleton?
    public void CheckHistData(final LocalDate uptoDay, TradingTimer timer) {
        logger.fine("Starting history data check.");
        boolean isOk = true;
        for (Map.Entry<String, CloseData> entry : closeDataMap.entrySet()) {
            String ticker = entry.getKey();
            CloseData data = entry.getValue();

            if ((data.adjCloses.length != 200)
                    || (data.dates.length != 200)) {
                logger.severe("Failed check hist data for: " + ticker + ". Length is not 200 but " + data.adjCloses.length);
                isOk = false;
            }
            LocalDate checkDate = uptoDay;
            while (!timer.IsTradingDay(checkDate)) {
                checkDate = checkDate.minusDays(1);
            }
            for (LocalDate date : data.dates) {
                boolean isLocalOk = true;
                switch (date.compareTo(checkDate)) {
                    case 0:
                        //logger.finest("Date OK. Date should be " + checkDate + " and is " + date);
                        break;
                    case 1:
                        logger.severe("Failed check hist data for: " + ticker + ". Date should be " + checkDate + " but is " + date);
                        isLocalOk = false;
                        break;
                    case -1:
                        logger.severe("Failed check hist data for: " + ticker + ". Date should be " + checkDate + " but is " + date);
                        isLocalOk = false;
                        break;
                    default:    //TODO: cislo znamena posun dnu - predelat switch
                        logger.severe("Failed check hist data for: " + ticker + ". Unknown compare value. Date should be " + checkDate + " but is " + date);
                        isLocalOk = false;
                }
                if (!isLocalOk) {
                    isOk = false;
                    break;
                }

                checkDate = checkDate.minusDays(1);
                while (!timer.IsTradingDay(checkDate)) {
                    checkDate = checkDate.minusDays(1);
                }
            }

            for (double adjClose : data.adjCloses) {
                if (adjClose == 0) {
                    logger.severe("Failed check hist data for: " + ticker + ". AdjClose value is 0");
                    isOk = false;
                }
            }
        }
        
        if (isOk) {
            logger.fine("History data check - OK");
        } else {
            logger.warning("History data check - FAILED");
        }
    }
}
