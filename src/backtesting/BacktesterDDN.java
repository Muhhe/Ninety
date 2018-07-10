/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import data.CloseData;
import data.IndicatorCalculator;
import data.getters.DataGetterHistCBOE;
import data.getters.IDataGetterHist;
import data.getters.DataGetterHistFile;
import data.getters.DataGetterHistGoogle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.util.logging.Logger;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class BacktesterDDN {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public enum Type {
        VXX, XIV, VXZ, ZIV, None
    }

    static class Status {

        public Type heldType = Type.None;
        public int heldPosition = 0;
        public double freeCapital = 0;
        public double closingEquity = 0;
        public double fees = 0;
    }

    static public void runBacktest(BTSettings settings) {
        IDataGetterHist getterFile = new DataGetterHistFile("backtest/VolData/");
        IDataGetterHist getterG = new DataGetterHistGoogle();
        CloseData dataVXX = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "VXX", false);
        CloseData dataXIV = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "XIV", false);
        CloseData dataVIX = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "VIX", false);

        IDataGetterHist getterCBOE = new DataGetterHistCBOE();
        CloseData dataVXMT = getterCBOE.readAdjCloseData(settings.startDate, settings.endDate, "VIX6M", false);
        CloseData dataSPY = getterG.readAdjCloseData(settings.startDate, settings.endDate, "SPY", false);

        TradeTimer.LoadSpecialTradingDays();

        BTStatistics stats = new BTStatistics(settings.capital, settings.reinvest);

        File equityFile = new File("equity.csv");
        equityFile.delete();

        Status status = new Status();
        status.freeCapital = settings.capital;

        for (int i = dataVIX.adjCloses.length - 11; i >= 1; i--) {

            LocalDate date = dataXIV.dates[i];
            TradeTimer.SetToday(date);

            logger.info("Day - " + date.toString());

            double[] zblebt = new double[5];

            for (int j = 0; j < 5; j++) {
                zblebt[j] = dataVXMT.adjCloses[i + j] - IndicatorCalculator.Volatility(2, dataSPY.adjCloses, i + j) * 100;
                
                logger.warning("dataVXMT - " + dataVXMT.adjCloses[i + j] + ", Volatility - " + IndicatorCalculator.Volatility(2, dataSPY.adjCloses, i + j) * 100);
            }

            double avg5 = IndicatorCalculator.SMA(5, zblebt);
            
            //logger.warning("avg5 - " + avg5);

            double heldValue = 0;
            switch (status.heldType) {
                case VXX:
                    heldValue = dataVXX.adjCloses[i];
                    break;
                case XIV:
                    heldValue = dataXIV.adjCloses[i];
                    break;
            }

            double eq = status.freeCapital + heldValue * status.heldPosition;

            status.freeCapital += heldValue * status.heldPosition;
            status.heldPosition = 0;
            status.heldType = Type.None;

            double value = 0;
            if (avg5 < 0) {
                status.heldType = Type.VXX;
                value = dataVXX.adjCloses[i];
            }

            if (avg5 > 0) {
                status.heldType = Type.XIV;
                value = dataXIV.adjCloses[i];
            }

            int posToBuy = (int) (status.freeCapital / value);

            status.freeCapital -= value * posToBuy;
            status.heldPosition = posToBuy;

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
