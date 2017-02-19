/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import communication.IBroker;
import data.CloseData;
import data.DataGetterActGoogle;
import data.DataGetterHistQuandl;
import data.DataGetterHistYahoo;
import data.IDataGetterAct;
import data.IDataGetterHist;
import data.IndicatorCalculator;
import data.StockIndicatorsForNinety;
import data.TickersToTrade;
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
import tradingapp.FilePaths;
import tradingapp.GlobalConfig;
import tradingapp.TradeFormatter;
import tradingapp.TradingTimer;

/**
 *
 * @author Muhe
 */
public class StockDataForNinety {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public Map<String, CloseData> closeDataMap = new HashMap<>(TickersToTrade.GetTickers().length);
    public Map<String, StockIndicatorsForNinety> indicatorsMap = new HashMap<>(TickersToTrade.GetTickers().length);

    public final Semaphore dataMutex = new Semaphore(1);

    private boolean isRealtimeDataSubscribed = false;

    /*public static String[] getSP100() {
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
    }*/

    public void PrepareData(IBroker broker) {

        try {
            logger.fine("PrepareHistData: Getting lock on hist data.");
            dataMutex.acquire();

            if (!isRealtimeDataSubscribed) {
                SubscribeRealtimeData(broker);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                }
            }

            logger.info("Starting to stock data.");
            String[] tickers = TickersToTrade.GetTickers();

            Map<String, Double> actValues = GetActualValues(broker);

            if (actValues == null) {
                logger.severe("Totally failed to load actual data.");
                closeDataMap.clear();
                return;
            }

            LocalDate lastTradingDay = TradingTimer.GetLastTradingDay();
            if (lastTradingDay.compareTo(LocalDate.now()) != 0) {
                logger.warning("Preparing data: Today " + LocalDate.now().toString() + " is not the same as last trading day " + lastTradingDay.toString());
            }

            logger.info("Starting to load historic data.");
            for (String ticker : tickers) {
                logger.finest("Loading hist data for " + ticker);

                Double actValue = actValues.get(ticker);
                if (actValue == null || actValue == 0) {
                    logger.warning("Cannot load actual data for: " + ticker + "! This stock will not be used.");
                    continue;
                }

                boolean failedHist = false;
                for (IDataGetterHist dataGetter : GlobalConfig.GetDataGettersHist()) {
                    if (failedHist) {
                        logger.warning("Trying to load it from " + dataGetter.getName());
                    }

                    CloseData data = dataGetter.readAdjCloseData(TradingTimer.GetLastTradingDay(lastTradingDay.minusDays(1)), ticker, 200, true);
                    if (data == null) {
                        logger.warning("Hist data from " + dataGetter.getName() + " for " + ticker + " are null.");
                        failedHist = false;
                        continue;
                    }

                    data.adjCloses[0] = actValue;
                    data.dates[0] = lastTradingDay;

                    if (NinetyChecker.CheckTickerData(data, ticker)) {
                        if (failedHist) {
                            logger.warning("Hist data from " + dataGetter.getName() + " for " + ticker + " loaded successfuly.");
                            failedHist = false;
                        }
                        closeDataMap.put(ticker, data);
                        break;
                    } else {
                        logger.warning("Failed to load " + dataGetter.getName() + " hist data for " + ticker + ".");
                        failedHist = true;
                        continue;
                    }
                }
                
                if (!failedHist) {
                    logger.warning("Hist data for " + ticker + " failed to load. Skipping this ticker.");
                }
            }

            logger.info("Finished to load historic data");

        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
            logger.info("Thread interuppted: " + ex);
        } finally {
            dataMutex.release();
            logger.fine("PrepareHistData: Released lock on hist data.");
        }
    }

    public void SubscribeRealtimeData(IBroker broker) {
        if (!isRealtimeDataSubscribed) {
            for (String ticker : TickersToTrade.GetTickers()) {
                broker.RequestRealtimeData(ticker);
            }
            isRealtimeDataSubscribed = true;
            logger.fine("Subscribed actual IB data.");
        }
    }

    public void UnSubscribeRealtimeData(IBroker broker) {
        if (isRealtimeDataSubscribed) {
            broker.CancelAllRealtimeData();
            isRealtimeDataSubscribed = false;
            logger.fine("Unubscribed actual IB data.");
        }
    }

    private Map<String, Double> GetActualValues(IBroker broker) {
        Map<String, Double> valuesMap = null;
        logger.info("Starting to load actual data");

        String[] tickers = TickersToTrade.GetTickers();
        boolean failedLvl1 = false;

        for (IDataGetterAct firstLvlGetter : GlobalConfig.GetDataGettersAct()) {
            firstLvlGetter.setBroker(broker);
            valuesMap = firstLvlGetter.readActualData(tickers);

            if (valuesMap != null && (valuesMap.size() >= tickers.length / 2)) {

                if (failedLvl1) {
                    logger.warning("Actual data from " + firstLvlGetter.getName() + " loaded " + valuesMap.size() + " out of " + tickers.length + " tickers.");
                }

                for (String ticker : tickers) {

                    Double actValue = valuesMap.get(ticker);
                    if (actValue == null || actValue == 0) {
                        logger.warning("Actual data for: " + ticker + " were not loaded from " + firstLvlGetter.getName());

                        for (IDataGetterAct secondLvlGetter : GlobalConfig.GetDataGettersAct()) {
                            firstLvlGetter.setBroker(broker);
                            actValue = secondLvlGetter.readActualData(ticker);
                            if (actValue != 0) {
                                logger.warning("Actual data for: " + ticker + " loaded successfuly from " + secondLvlGetter.getName());
                                break;
                            } else {
                                logger.warning("Cannot load actual data for: " + ticker + " from " + secondLvlGetter.getName());
                            }
                        }

                        if (actValue == null || actValue == 0) {
                            logger.warning("Cannot load actual data for: " + ticker + "! This stock will not be used.");
                            continue;
                        }

                        valuesMap.put(ticker, actValue);
                    }
                }

                break;
            } else {
                failedLvl1 = true;
                logger.warning("Failed to load actual data from " + firstLvlGetter.getName() + ".");
            }
        }
        
        return valuesMap;
    }

    public void UpdateDataWithActValuesIB(IBroker broker) {
        /*if (!TradingTimer.IsTradingDay(LocalDate.now())) {
            logger.warning("Today is not a trading day. Cannot update with actual values.");
            return;
        }*/

        if (!isRealtimeDataSubscribed) {
            SubscribeRealtimeData(broker);
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
            }
        }

        try {
            logger.fine("UpdateDataWithActValues: Getting lock on hist data.");
            dataMutex.acquire();

            if (closeDataMap.isEmpty()) {
                logger.severe("updateDataWithActualValues - stockMap.isEmpty");
                return;
            }

            Map<String, Double> actValues = GetActualValues(broker);

            if (actValues == null) {
                logger.severe("Totally failed to load actual data.");
                closeDataMap.clear();
                return;
            }

            LocalDate lastTradingDay = TradingTimer.GetLastTradingDay();
            if (lastTradingDay.compareTo(LocalDate.now()) != 0) {
                logger.warning("Updating with actual data: Today " + LocalDate.now().toString() + " is not the same as last trading day " + lastTradingDay.toString());
            }

            for (Iterator<Map.Entry<String, CloseData>> it = closeDataMap.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, CloseData> entry = it.next();

                Double actValue = actValues.get(entry.getKey());
                if (actValue == null || actValue == 0) {
                    logger.warning("Cannot load actual data for: " + entry.getKey() + "! This stock will not be used.");
                    it.remove();
                    continue;
                }

                entry.getValue().adjCloses[0] = actValue;
                entry.getValue().dates[0] = lastTradingDay;

            }

        } catch (InterruptedException ex) {
        } finally {
            dataMutex.release();
            logger.fine("UpdateDataWithActValues: Released lock on hist data.");
            logger.info("Finished to load actual data");
        }
    }

    /*private void UpdateDataWithActValuesGoogle() {  //TODO: obsolete
        logger.fine("Starting to load actual data from Google");

        IDataGetterAct getter = GlobalConfig.GetDataGettersAct()[0];

        String[] tickerSymbols = TickersToTrade.GetTickers();
        Map<String, Double> valuesMap = getter.readActualData(tickerSymbols);

        if (valuesMap != null) {
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
        } else {
            logger.warning("Failed to load actual data from google at once.");
            logger.info("Loading one at a time...");
            for (Iterator<Map.Entry<String, CloseData>> it = closeDataMap.entrySet().iterator(); it.hasNext();) {
                CloseData closeData = it.next().getValue();
                String ticker = it.next().getKey();

                closeData.adjCloses[0] = getter.readActualData(ticker);
                closeData.dates[0] = LocalDate.now();
                if (closeData.adjCloses[0] == 0) {
                    logger.warning("Cannot load actual data for: " + ticker + ", ticker will not be used!");
                    it.remove();
                }
            }
        }

        logger.fine("Finished to load actual data from Google");

    }*/

    public void CalculateIndicators() {
        try {
            logger.finer("CalculateIndicators: Getting lock on hist data.");
            dataMutex.acquire();

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
            dataMutex.release();
            logger.finer("CalculateIndicators: Released lock on hist data.");
            logger.fine("Finished to compute indicators");
        }
    }

    public void SaveHistDataToFiles() {
        LocalDate today = LocalDate.now();
        String todayString = today.toString();
        for (Map.Entry<String, CloseData> entry : closeDataMap.entrySet()) {
            File file = new File(FilePaths.dataLogDirectory + todayString + "/Historic/" + entry.getKey() + ".csv");
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
            File file = new File(FilePaths.dataLogDirectory + todayString + "/Indicators/" + entry.getKey() + ".txt");
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
        File file = new File(FilePaths.dataLogDirectory + todayString + "/indicators.csv");
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
