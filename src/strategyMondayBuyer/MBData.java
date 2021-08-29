/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyMondayBuyer;

import backtesting.BTLogLvl;
import communication.IBroker;
import data.CloseData;
import data.IndicatorCalculator;
import data.Utils;
import data.getters.IDataGetterHist;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import strategy90.TickersToTrade;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import tradingapp.FilePaths;
import tradingapp.GlobalConfig;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;
import data.OHLCData;
import java.util.List;

/**
 *
 * @author Muhe
 */
public class MBData {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    //public Map<String, CloseData> closeDataMap = new HashMap<>(TickersToTrade.GetTickers().length);
    public Map<String, OHLCData> ohlcDataMap = new HashMap<>(TickersToTrade.GetTickers().length);
    public Map<String, OHLCData> actualDataMap = new HashMap<>();
    public Map<String, MBIndicators> indicatorsMap = new HashMap<>(TickersToTrade.GetTickers().length);
    private CloseData spy;

    public final Semaphore dataMutex = new Semaphore(1);

    public static final int DAYS_TO_LOAD = 255;
    public static final int MAX_SUBSCRIBED = 50;

    public boolean spxBT50 = true; // TODO

    private final int offset = 0;//GlobalConfig.isBacktest ? 0 : 1;

    final IBroker broker;

    public MBData(IBroker broker) {
        this.broker = broker;
    }

    public void SubscribeHistData(String[] tickers, int startInx, int endInx) {
        for (int i = startInx; i < endInx; i++) {
            broker.RequestHistoricalData(tickers[i], DAYS_TO_LOAD);
            TradeTimer.wait(20);
        }
        logger.fine("Subscribed hist IB data.");
    }

    public void PrepareActualData(List<String> tickers) {
        for (String ticker : tickers) {
            PrepareActualData(ticker);
        }
    }

    public void PrepareActualData(Map<String, MBHeldTicker> tickers) {
        for (Map.Entry<String, MBHeldTicker> entry : tickers.entrySet()) {
            PrepareActualData(entry.getKey());
        }
    }

    public void PrepareActualData(String ticker) {
        IDataGetterHist dataGetter = GlobalConfig.GetDataGettersHist()[0];

        logger.finest("Loading hist data for " + ticker + " from " + dataGetter.getName());

        OHLCData data = dataGetter.readAdjOHLCData(TradeTimer.GetLastTradingDay(TradeTimer.GetLocalDateNow()), ticker, 1, true);

        actualDataMap.put(ticker, data);
    }

    public double getLastKnownPrice(String ticker) {
        if (actualDataMap == null || actualDataMap.isEmpty() || actualDataMap.get(ticker) == null) {
            return ohlcDataMap.get(ticker).adjCloses[offset];
        } else {
            return actualDataMap.get(ticker).opens[0];
        }
    }

    public void PrepareOHLCData(Map<String, MBHeldTicker> tickers) {
        for (Map.Entry<String, MBHeldTicker> entry : tickers.entrySet()) {
            String ticker = entry.getKey();

            IDataGetterHist dataGetter = GlobalConfig.GetDataGettersHist()[0];

            logger.finest("Loading hist data for " + ticker + " from " + dataGetter.getName());

            OHLCData data = dataGetter.readAdjOHLCData(TradeTimer.GetLastTradingDay(TradeTimer.GetLocalDateNow().minusDays(1)), ticker, 1, true);

            ohlcDataMap.put(ticker, data);
        }
    }

    public void PrepareData(String[] tickers) {

        ohlcDataMap.clear();
        actualDataMap.clear();
        indicatorsMap.clear();
        broker.RequestHistoricalData("SPY", DAYS_TO_LOAD);
        TradeTimer.wait(20000);
        IDataGetterHist dataGet = GlobalConfig.GetDataGettersHist()[0];
        spy = dataGet.readAdjCloseData(TradeTimer.GetLastTradingDay(TradeTimer.GetLocalDateNow().minusDays(1)), "SPY", DAYS_TO_LOAD, false);
        broker.CancelAllHistoricalData();

        LocalDate firstDateToLoad = TradeTimer.GetLastTradingDay(TradeTimer.GetLocalDateNow().minusDays(1));
        if (spy == null || !MBChecker.CheckValues(spy.adjCloses, spy.dates, "SPY") || !MBChecker.CheckDates(spy.dates, "SPY")) {
            logger.warning("Failed to load SPY data!");
        }
        if (spy.dates[offset].compareTo(firstDateToLoad) != 0) {
            logger.warning("Failed SPY, wrong first date " + spy.dates[offset] + " vs " + firstDateToLoad);
        }

        try {
            logger.fine("PrepareHistData: Getting lock on hist data.");
            dataMutex.acquire();

            logger.info("Starting to load historic data.");
            int startInx = 0;
            while (startInx < tickers.length) {
                int endInx = Math.min(startInx + MAX_SUBSCRIBED, tickers.length);
                if (!GlobalConfig.isBacktest) {
                    SubscribeHistData(tickers, startInx, endInx);
                    TradeTimer.wait(150000);
                }
                for (int i = startInx; i < endInx; i++) {

                    boolean failedHist = false;
                    for (IDataGetterHist dataGetter : GlobalConfig.GetDataGettersHist()) {

                        logger.finest("Loading hist data for " + tickers[i] + " from " + dataGetter.getName());
                        if (failedHist) {
                            logger.warning("Trying to load it from " + dataGetter.getName());
                        }

                        OHLCData data = dataGetter.readAdjOHLCData(firstDateToLoad, tickers[i], DAYS_TO_LOAD, false);

                        if (!CheckTickers(data, tickers[i])) {
                            //logger.warning("Hist data from " + dataGetter.getName() + " for " + tickers[i] + " are wrong.");
                            failedHist = true;
                            continue;
                        }

                        //logger.info("Loaded " + tickers[i] + ", Value: " + data.adjCloses[0]);
                        if (data.adjCloses[offset] < 5.0) {
                            logger.warning("Skipped " + tickers[i] + ", Too low value: " + data.adjCloses[1]);
                            failedHist = true;
                            continue;
                        }
                        if (data.dates[offset].compareTo(firstDateToLoad) != 0) {
                            logger.warning("Failed " + tickers[i] + ", wrong first date " + data.dates[offset] + " vs " + firstDateToLoad);
                            failedHist = true;
                            continue;
                        }
                        if (CheckTickers(data, tickers[i])) {
                            if (failedHist) {
                                logger.warning("Hist data from " + dataGetter.getName() + " for " + tickers[i] + " loaded successfuly.");
                                failedHist = false;
                            }
                            ohlcDataMap.put(tickers[i], data);
                            break;
                        } else {
                            logger.warning("Failed to load " + dataGetter.getName() + " hist data for " + tickers[i] + ".");
                            failedHist = true;
                            continue;
                        }
                    }

                    if (failedHist) {
                        logger.warning("Hist data for " + tickers[i] + " failed to load. Skipping this ticker.");
                    }
                }
                startInx = endInx;
                broker.CancelAllHistoricalData();
            }

            //SaveHistDataToFiles();
            calculateIndicators();
            //SaveStockIndicatorsToFiles();
            logger.info("Finished loading data");
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
            logger.info("Thread interuppted: " + ex);
        } finally {
            dataMutex.release();
            logger.fine("PrepareHistData: Released lock on hist data.");
        }
    }

    public void calculateIndicators() {
        for (Map.Entry<String, OHLCData> entry : ohlcDataMap.entrySet()) {
            OHLCData value = entry.getValue();

            OHLCData weeklyData = Utils.GetLastDaysInWeek(value, offset);

            /*for (LocalDate date : weeklyData.dates) {
                logger.info(entry.getKey() + " A: " + date.getDayOfWeek() + " " + date);
            }*/
            MBIndicators indicators = new MBIndicators();
            indicators.rsi2 = IndicatorCalculator.RSI(weeklyData.adjCloses, 14, 0);
            double sma10 = IndicatorCalculator.SMA(10, weeklyData.adjCloses);
            indicators.sma10x2 = weeklyData.adjCloses[0] < sma10 && weeklyData.adjCloses[1] < sma10;
            indicators.vol = IndicatorCalculator.Volatility(10, weeklyData.adjCloses, 0);
            indicatorsMap.put(entry.getKey(), indicators);
        }

        CloseData weeklySpx = Utils.GetLastDaysInWeek(spy, offset);
        int bt = 0;
        for (int i = offset + 1; i < weeklySpx.adjCloses.length; ++i) {
            if (spy.adjCloses[offset] > weeklySpx.adjCloses[i]) {
                bt++;
            }
        }

        spxBT50 = (bt > (weeklySpx.adjCloses.length / 2));

        //todo
//        spxBT50 = true;
//        logger.warning("spxBT50 = true");
        logger.log(BTLogLvl.BT_STATS, "spxBT50 - " + spxBT50);
    }

    public boolean CheckTickers(OHLCData data, String ticker) {
        if (data == null) {
            logger.warning("Failed to load hist data for " + ticker + " - is null.");
            return false;
        }
        return MBChecker.CheckTickerData(data, ticker);
    }

    public void SaveHistDataToFiles() {
        LocalDate today = TradeTimer.GetLocalDateNow();
        String todayString = today.toString();
        for (Map.Entry<String, OHLCData> entry : ohlcDataMap.entrySet()) {
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
        for (Map.Entry<String, MBIndicators> entry : indicatorsMap.entrySet()) {
            File file = new File(FilePaths.dataLogDirectory + todayString + "/Indicators/" + entry.getKey() + ".txt");
            File directory = new File(file.getParentFile().getAbsolutePath());
            directory.mkdirs();
            BufferedWriter output = null;
            try {
                file.createNewFile();
                output = new BufferedWriter(new FileWriter(file));

                MBIndicators indicators = entry.getValue();
                output.write("SMA10x2: " + indicators.sma10x2);
                output.newLine();
                output.write("VOL: " + TradeFormatter.toString(indicators.vol));
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
}
