/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import communication.IBroker;
import data.CloseData;
import data.IndicatorCalculator;
import data.Utils;
import data.getters.DataGetterHistFile;
import data.getters.IDataGetterHist;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import strategy90.TickersToTrade;
import strategyMondayBuyer.MBData;
import strategyMondayBuyer.MBHeldTicker;
import strategyMondayBuyer.MBRunner;
import strategyMondayBuyer.MBStatus;
import strategyVXVMT.VXVMTSignal;
import strategyVXVMT.VXVMTStatus;
import test.BrokerNoIB;
import tradingapp.FilePaths;
import tradingapp.GlobalConfig;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class BacktesterMB {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void RunTest(BTSettings settings) {
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

        BTStatistics stats = new BTStatistics(settings.capital, settings.reinvest);
        IBroker broker = new BrokerNoIB();

        //String[] tickers = TickersToTrade.GetTickers();
        IDataGetterHist getterFile = new DataGetterHistFile("backtest/SP500/", "ddMMyyyy", 4, true);

        GlobalConfig.AddDataGetterHist(getterFile);
        CloseData dataSPY = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "SPY", true);

        String[] tickers = Utils.LoadTickers();

        MBStatus status = new MBStatus();
        status.equity = 10000;

        for (int i = dataSPY.adjCloses.length - 255; i >= 1; i--) {
            LocalDate date = dataSPY.dates[i];
            TradeTimer.SetToday(date);
            logger.log(BTLogLvl.BT_STATS, "Day - " + date.toString());
            boolean firstDayOfWeek = TradeTimer.isFirstDoW();//(date.getDayOfWeek().getValue() < dataSPY.dates[i + 1].getDayOfWeek().getValue());

            MBData data = new MBData(broker);
            MBRunner runner = new MBRunner(data, status, broker);
            data.PrepareActualData(status.heldTickers);
            String[] keys = status.heldTickers.keySet().toArray(new String[status.heldTickers.keySet().size()]);
            data.PrepareData(keys);
            runner.runSells();

            if (firstDayOfWeek) {
                data.PrepareData(tickers);
                runner.run();
            }

            logger.log(BTLogLvl.BT_STATS, "Current equity = " + TradeFormatter.toString(status.equity) + "$");

            stats.StartDay(date);

            stats.UpdateEquity(status.equity, date);
            UpdateEquityFile(status.equity, FilePaths.equityPathFile, null);
            //UpdateEquityFile(xivPos * dataXIV.adjCloses[i], "vix.csv", null);
            //UpdateEquityFile(spyPos * dataSPY.adjCloses[i], "spy.csv", null);*/

            stats.EndDay();
        }
    }

    static public void UpdateEquityFile(double currentCash, String path, String addInfo) {
        Writer writer = null;
        try {
            File equityFile = new File(path);
            equityFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(equityFile, true), "UTF-8"));
            String line = TradeTimer.GetLocalDateNow().toString() + "," + currentCash;
            if (addInfo != null) {
                line += "," + addInfo;
            }
            line += "\r\n";
            writer.append(line);

            logger.fine("Updated equity file with value " + currentCash);
        } catch (FileNotFoundException ex) {
            logger.severe("Cannot find equity file: " + ex);
        } catch (IOException ex) {
            logger.severe("Error updating equity file: " + ex);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                logger.severe("Error updating equity file: " + ex);
            }
        }
    }
}
