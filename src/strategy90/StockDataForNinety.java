/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategy90;

import communication.IBroker;
import data.CloseData;
import data.getters.IDataGetterAct;
import data.getters.IDataGetterHist;
import data.IndicatorCalculator;
import data.getters.DataGetterHistFile;
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
import tradingapp.TradeTimer;

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

    public void PrepareData() {

        try {
            logger.fine("PrepareHistData: Getting lock on hist data.");
            dataMutex.acquire();

            if (!isRealtimeDataSubscribed) {
                logger.warning("Actual data not subscribed.");
            }

            logger.info("Starting to load stock data.");
            String[] tickers = TickersToTrade.GetTickers();

            Map<String, Double> actValues = GetActualValues();

            if (actValues == null) {
                logger.severe("Totally failed to load actual data.");
                closeDataMap.clear();
                return;
            }

            LocalDate lastTradingDay = TradeTimer.GetLastTradingDay();
            if (lastTradingDay.compareTo(TradeTimer.GetLocalDateNow()) != 0) {
                logger.warning("Preparing data: Today " + TradeTimer.GetLocalDateNow().toString() + " is not the same as last trading day " + lastTradingDay.toString());
            }

            //IDataGetterHist dataGetterFile = new DataGetterHistFile(FilePaths.dataLogDirectory + TradeTimer.GetLocalDateNow().toString() + "/Historic/");
            logger.info("Starting to load historic data.");
            for (String ticker : tickers) {

                Double actValue = actValues.get(ticker);
                if (actValue == null || actValue == 0) {
                    logger.warning("Cannot load actual data for: " + ticker + "! This stock will not be used.");
                    continue;
                }

                boolean failedHist = false;
                // boolean filesRead = false;
                for (IDataGetterHist dataGetter : GlobalConfig.GetDataGettersHist()) {

                    //if (!filesRead) {
                    //    dataGetter = dataGetterFile;
                    //}
                    logger.finest("Loading hist data for " + ticker + " from " + dataGetter.getName());
                    if (failedHist) {
                        logger.warning("Trying to load it from " + dataGetter.getName());
                    }

                    //LocalDate date = lastTradingDay;
                    //if (filesRead) {
                    //    date = TradeTimer.GetLastTradingDay(lastTradingDay.minusDays(1));
                    //}
                    CloseData data = dataGetter.readAdjCloseData(TradeTimer.GetLastTradingDay(lastTradingDay.minusDays(1)), ticker, 200, true);

                    //filesRead = true;
                    if (data == null) {
                        logger.warning("Hist data from " + dataGetter.getName() + " for " + ticker + " are null.");
                        failedHist = true;
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
                        SaveHistDataToFiles();
                        break;
                    } else {
                        logger.warning("Failed to load " + dataGetter.getName() + " hist data for " + ticker + ".");
                        failedHist = true;
                        continue;
                    }
                }

                if (failedHist) {
                    logger.warning("Hist data for " + ticker + " failed to load. Skipping this ticker.");
                }
            }

            logger.info("Finished loading data");

            CalculateIndicators();

        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
            logger.info("Thread interuppted: " + ex);
        } finally {
            dataMutex.release();
            logger.fine("PrepareHistData: Released lock on hist data.");
        }
    }

    public void SubscribeActData(IBroker broker) {
        if (!isRealtimeDataSubscribed) {
            for (String ticker : TickersToTrade.GetTickers()) {
                broker.SubscribeRealtimeData(ticker);
            }
            isRealtimeDataSubscribed = true;
            logger.fine("Subscribed actual IB data.");
        }
    }

    public void SubscribeHistData(IBroker broker) {
        for (String ticker : TickersToTrade.GetTickers()) {
            broker.RequestHistoricalData(ticker, 200);
        }
        logger.fine("Subscribed hist IB data.");
    }

    public void UnSubscribeRealtimeData(IBroker broker) {
        if (isRealtimeDataSubscribed) {
            broker.CancelAllRealtimeData();
            isRealtimeDataSubscribed = false;
            logger.fine("Unubscribed actual IB data.");
        }
    }

    private Map<String, Double> GetActualValues() {
        Map<String, Double> valuesMap = null;
        logger.info("Starting to load actual data");

        String[] tickers = TickersToTrade.GetTickers();
        boolean failedLvl1 = false;

        for (IDataGetterAct firstLvlGetter : GlobalConfig.GetDataGettersAct()) {
            logger.fine("Starting to load act data from " + firstLvlGetter.getName());

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
                if (valuesMap != null) {
                    logger.warning("Loaded only " + valuesMap.size() + " tickers.");
                }
            }
        }

        return valuesMap;
    }

    public void UpdateDataWithActValues() {

        if (!isRealtimeDataSubscribed) {
            logger.warning("Actual data not subscribed.");
        }

        try {
            logger.fine("UpdateDataWithActValues: Getting lock on hist data.");
            dataMutex.acquire();

            if (closeDataMap.isEmpty()) {
                logger.severe("updateDataWithActualValues - stockMap.isEmpty");
                return;
            }

            Map<String, Double> actValues = GetActualValues();

            if (actValues == null) {
                logger.severe("Totally failed to load actual data.");
                closeDataMap.clear();
                return;
            }

            LocalDate lastTradingDay = TradeTimer.GetLastTradingDay();
            if (lastTradingDay.compareTo(TradeTimer.GetLocalDateNow()) != 0) {
                logger.warning("Updating with actual data: Today " + TradeTimer.GetLocalDateNow().toString() + " is not the same as last trading day " + lastTradingDay.toString());
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

            CalculateIndicators();

        } catch (InterruptedException ex) {
        } finally {
            dataMutex.release();
            logger.fine("UpdateDataWithActValues: Released lock on hist data.");
            logger.info("Finished to load actual data");
        }
    }

    private void CalculateIndicators() {
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
        logger.fine("Finished to compute indicators");
    }

    public void ClearData() {
        closeDataMap.clear();
        indicatorsMap.clear();
    }

    public void SaveHistDataToFiles() {
        LocalDate today = TradeTimer.GetLocalDateNow();
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
        LocalDate today = TradeTimer.GetLocalDateNow();
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
        String todayString = TradeTimer.GetLocalDateNow().toString();
        File file = new File(FilePaths.dataLogDirectory + todayString + FilePaths.indicatorsPathFile);
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
