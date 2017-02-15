/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import communication.IBBroker;
import data.CloseData;
import data.DataGetterActGoogle;
import data.DataGetterHistQuandl;
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
import tradingapp.TradeFormatter;
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

    public boolean isRealtimeDataSubscribed = false;

    public static String[] getSP100() {
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
    
    private void FillFirstDato(CloseData data) {                
        if ((data != null) && (data.adjCloses.length > 2)) {
            data.adjCloses[0] = data.adjCloses[1];
            data.dates[0] = LocalDate.now();
        }
    }

    public void PrepareHistData() {

        try {
            logger.fine("PrepareHistData: Getting lock on hist data.");
            histDataMutex.acquire();
            logger.info("Starting to load historic data");
            String[] tickers = getSP100();
            for (String ticker : tickers) {
                logger.finest("Loading hist data for " + ticker);
                
                CloseData data = DataGetterHistYahoo.readData(LocalDate.now(), 200, ticker);
                
                FillFirstDato(data);

                if ((data == null) || !NinetyChecker.CheckTickerData(data, ticker)) {
                    logger.warning("Failed to load Yahoo hist data for " + ticker + ". Trying to load it from Quandl.");
                    data = DataGetterHistQuandl.readData(LocalDate.now(), 200, ticker);
                    FillFirstDato(data);
                    
                    if ((data != null) && NinetyChecker.CheckTickerData(data, ticker)) {
                        logger.warning("Hist data from Quandl for " + ticker + " loaded successfuly.");
                        closeDataMap.put(ticker, data);
                    } else {
                        logger.severe("Failed to load hist data for " + ticker);
                    }
                } else {
                    closeDataMap.put(ticker, data);
                }
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

    public void SubscribeRealtimeData(IBBroker broker) {
        if (!isRealtimeDataSubscribed) {
            for (String ticker : getSP100()) {
                broker.RequestRealtimeData(ticker);
            }
            isRealtimeDataSubscribed = true;
            logger.fine("Subscribed actual IB data.");
        }
    }

    public void UnSubscribeRealtimeData(IBBroker broker) {
        if (isRealtimeDataSubscribed) {
            broker.CancelAllRealtimeData();
            isRealtimeDataSubscribed = false;
            logger.fine("Unubscribed actual IB data.");
        }
    }

    private boolean CheckRealtimeDataOnIB(IBBroker broker) {
        int tickersFilled = 0;

        for (Iterator<Map.Entry<String, CloseData>> it = closeDataMap.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, CloseData> entry = it.next();

            double actValue = broker.GetLastPrice(entry.getKey());
            if (actValue != 0) {
                tickersFilled++;
            }
        }
        
        logger.fine("Actual IB data - loaded " + tickersFilled + " out of " + closeDataMap.size());
            
        return tickersFilled >= closeDataMap.size()/2;
    }

    public void UpdateDataWithActValuesIB(IBBroker broker) {
        if (!TradingTimer.IsTradingDay(LocalDate.now())) {
            logger.warning("Today is not a trading day. Cannot update with actual values.");
            return;
        }

        if (!isRealtimeDataSubscribed) {
            SubscribeRealtimeData(broker);
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
            }
        }

        try {
            logger.fine("UpdateDataWithActValues: Getting lock on hist data.");
            histDataMutex.acquire();

            if (closeDataMap.isEmpty()) {
                logger.severe("updateDataWithActualValues - stockMap.isEmpty");
                return;
            }

            logger.info("Starting to load actual data");

            if (CheckRealtimeDataOnIB(broker)) {

                for (Iterator<Map.Entry<String, CloseData>> it = closeDataMap.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<String, CloseData> entry = it.next();

                    double actValue = broker.GetLastPrice(entry.getKey());
                    if (actValue == 0) {

                        try {
                            actValue = DataGetterActGoogle.readActualData(entry.getKey());
                        } catch (IOException | NumberFormatException ex) {
                            logger.warning("Cannot load actual data for: " + entry.getKey() + ", exception: " + ex.getMessage() + "! This stock will not be used.");
                            it.remove();
                            continue;
                        }

                        logger.info("Failed to load actual data from IB for: " + entry.getKey() + "! Load from Google succeded.");
                    }

                    entry.getValue().adjCloses[0] = actValue;
                    entry.getValue().dates[0] = LocalDate.now();
                }

            } else {
                logger.warning("Failed to load real-time data from IB. Trying to load it from Google.");
                UpdateDataWithActValuesGoogle();
            }

        } catch (InterruptedException ex) {
        } finally {
            histDataMutex.release();
            logger.fine("UpdateDataWithActValues: Released lock on hist data.");
            logger.info("Finished to load actual data");
        }
    }

    private void UpdateDataWithActValuesGoogle() {
        try {
            logger.fine("Starting to load actual data from Google");

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
                String ticker = it.next().getKey();
                try {
                    closeData.adjCloses[0] = DataGetterActGoogle.readActualData(ticker);
                    closeData.dates[0] = LocalDate.now();
                } catch (IOException | NumberFormatException ex2) {
                    logger.warning("Cannot load actual data for: " + ticker + ", exception: " + ex2.getMessage());
                    it.remove();
                }
            }
        } finally {
            logger.fine("Finished to load actual data from Google");
        }
    }

    public void CalculateIndicators() {
        try {
            logger.finer("CalculateIndicators: Getting lock on hist data.");
            histDataMutex.acquire();

            logger.fine("Starting to compute indicators");
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
            logger.finer("CalculateIndicators: Released lock on hist data.");
            logger.fine("Finished to compute indicators");
        }
    }

    public void SaveHistDataToFiles() {
        LocalDate today = LocalDate.now();
        String todayString = today.toString();
        for (Map.Entry<String, CloseData> entry : closeDataMap.entrySet()) {
            File file = new File("dataLog/" + todayString + "/Historic/" + entry.getKey() + ".csv");
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
                    output.write(",");
                    output.write(TradeFormatter.toString(adjCloses[inx]));
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
                output.write("ActValue: " + TradeFormatter.toString(indicators.actValue));
                output.newLine();
                output.write("SMA200: " + TradeFormatter.toString(indicators.sma200));
                output.newLine();
                output.write("SMA5: " + TradeFormatter.toString(indicators.sma5));
                output.newLine();
                output.write("RSI2: " + TradeFormatter.toString(indicators.rsi2));

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

            output.write("Ticker,ActValue,SMA200,SMA5,RSI2");
            for (Map.Entry<String, StockIndicatorsForNinety> entry : indicatorsMap.entrySet()) {
                StockIndicatorsForNinety indicators = entry.getValue();
                output.newLine();
                output.write(entry.getKey());
                output.append(',');
                output.write(TradeFormatter.toString(indicators.actValue));
                output.append(',');
                output.write(TradeFormatter.toString(indicators.sma200));
                output.append(',');
                output.write(TradeFormatter.toString(indicators.sma5));
                output.append(',');
                output.write(TradeFormatter.toString(indicators.rsi2));
            }
        } catch (IOException ex) {
            logger.warning("Cannot create indicator CSV log file.");
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
