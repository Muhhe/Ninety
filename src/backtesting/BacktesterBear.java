/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import data.CloseData;
import data.IndicatorCalculator;
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
public class BacktesterBear {

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

            double[] dailyChanges = new double[10];

            for (int j = 0; j < 10; j++) {
                dailyChanges[j] = (dataVIX.adjCloses[i + j] - dataVIX.adjCloses[i + j + 1]) / dataVIX.adjCloses[i + j + 1] * 100;
            }

            double std = IndicatorCalculator.StandardDeviation(10, dailyChanges);

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

            if ((std > 11) || (std < 10)) {

                status.freeCapital += heldValue * status.heldPosition;
                status.heldPosition = 0;
                status.heldType = Type.None;

                double value = 0;
                if (std > 11) {
                    status.heldType = Type.VXX;
                    value = dataVXX.adjCloses[i];
                }

                if (std < 10) {
                    status.heldType = Type.XIV;
                    value = dataXIV.adjCloses[i];
                }

                int posToBuy = (int) (status.freeCapital / value);

                status.freeCapital -= value * posToBuy;
                status.heldPosition = posToBuy;
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
