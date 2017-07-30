/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import communication.IBroker;
import data.CloseData;
import data.getters.IDataGetterHist;
import data.IndicatorCalculator;
import data.getters.DataGetterHistFile;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.util.logging.Logger;
import strategyVXVMT.VXVMTRunner;
import strategyVXVMT.VXVMTSignal;
import strategyVXVMT.VXVMTStatus;
import test.BrokerNoIB;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class BacktesterTrend {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    static public void runBacktest(BTSettings settings) {
        IDataGetterHist getterFile = new DataGetterHistFile("backtest/VolData/");
        //IDataGetterHist getterFile = new DataGetterHistGoogle();
        CloseData dataVXX = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "VXX", false);
        CloseData dataXIV = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "XIV", false);

        TradeTimer.LoadSpecialTradingDays();

        BTStatistics stats = new BTStatistics(settings.capital, settings.reinvest);

        File equityFile = new File("equity.csv");
        equityFile.delete();

        double lastCapital = settings.capital;

        IBroker broker = new BrokerNoIB();
        VXVMTStatus status = new VXVMTStatus();
        status.freeCapital = settings.capital;

        for (int i = dataXIV.adjCloses.length - 106; i >= 1; i--) {

            LocalDate date = dataXIV.dates[i];
            TradeTimer.SetToday(date);

            logger.info("Day - " + date.toString());

            double sma10 = IndicatorCalculator.SMA(10, dataXIV.adjCloses, i);
            double sma100 = IndicatorCalculator.SMA(100, dataXIV.adjCloses, i);

            double[] smasRatio = new double[6];
            smasRatio[0] = IndicatorCalculator.SMA(10, dataXIV.adjCloses, i) / IndicatorCalculator.SMA(100, dataXIV.adjCloses, i);
            smasRatio[1] = IndicatorCalculator.SMA(10, dataXIV.adjCloses, i+1) / IndicatorCalculator.SMA(100, dataXIV.adjCloses, i+1);
            smasRatio[2] = IndicatorCalculator.SMA(10, dataXIV.adjCloses, i+2) / IndicatorCalculator.SMA(100, dataXIV.adjCloses, i+2);
            smasRatio[3] = IndicatorCalculator.SMA(10, dataXIV.adjCloses, i+3) / IndicatorCalculator.SMA(100, dataXIV.adjCloses, i+3);
            smasRatio[4] = IndicatorCalculator.SMA(10, dataXIV.adjCloses, i+4) / IndicatorCalculator.SMA(100, dataXIV.adjCloses, i+4);
            smasRatio[5] = IndicatorCalculator.SMA(10, dataXIV.adjCloses, i+5) / IndicatorCalculator.SMA(100, dataXIV.adjCloses, i+5);
            
            double emaRat = IndicatorCalculator.EMA(3, smasRatio);
            
            double heldValue = 0;
            if (status.heldType == VXVMTSignal.Type.XIV) {
                heldValue = dataXIV.adjCloses[i];
            } else {
                heldValue = dataVXX.adjCloses[i];
            }
            
            
            status.freeCapital += heldValue * status.heldPosition;
            status.heldPosition = 0;
            status.heldType = VXVMTSignal.Type.None;

            double eq = status.freeCapital;
            
            //if (sma10 > sma100) {
            if (emaRat > 1) {
                int posToBuy = (int) (status.freeCapital / dataXIV.adjCloses[i]);

                status.freeCapital -= dataXIV.adjCloses[i] * posToBuy;
                status.heldPosition = posToBuy;
                status.heldType = VXVMTSignal.Type.XIV;
            } else {
                int posToBuy = (int) (status.freeCapital / dataVXX.adjCloses[i]);

                status.freeCapital -= dataVXX.adjCloses[i] * posToBuy;
                status.heldPosition = posToBuy;
                status.heldType = VXVMTSignal.Type.VXX;
            }

            stats.StartDay(date);

            stats.UpdateEquity(eq, date);
            UpdateEquityFile(eq, "equity.csv", (status.heldType.toString() + " - " + TradeFormatter.toString(1 - (status.freeCapital / eq))));
            //UpdateEquityFile(xivPos * dataXIV.adjCloses[i], "vix.csv", null);
            //UpdateEquityFile(spyPos * dataSPY.adjCloses[i], "spy.csv", null);*/

            stats.EndDay();
        }

        stats.LogStats(settings);

        stats.SaveEquityToCsv();

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
