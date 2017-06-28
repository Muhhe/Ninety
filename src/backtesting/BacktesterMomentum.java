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
public class BacktesterMomentum {

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
        CloseData dataVXX = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "VXX");
        CloseData dataXIV = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "XIV");
        CloseData dataVXZ = getterG.readAdjCloseData(settings.startDate, settings.endDate, "VXZ");
        CloseData dataZIV = getterG.readAdjCloseData(settings.startDate, settings.endDate, "ZIV");

        TradeTimer.LoadSpecialTradingDays();

        BTStatistics stats = new BTStatistics(settings.capital, settings.reinvest);

        File equityFile = new File("equity.csv");
        equityFile.delete();

        Status status = new Status();
        status.freeCapital = settings.capital;

        for (int i = dataZIV.adjCloses.length - 84; i >= 1; i--) {

            LocalDate date = dataXIV.dates[i];
            TradeTimer.SetToday(date);

            logger.info("Day - " + date.toString());

            double heldValue = 0;
            switch (status.heldType) {
                case VXX:
                    status.heldType = Type.VXX;
                    heldValue = dataVXX.adjCloses[i];
                        break;
                case XIV:
                    status.heldType = Type.XIV;
                    heldValue = dataXIV.adjCloses[i];
                        break;
                case VXZ:
                    status.heldType = Type.VXZ;
                    heldValue = dataVXZ.adjCloses[i];
                        break;
                case ZIV:
                    status.heldType = Type.ZIV;
                    heldValue = dataZIV.adjCloses[i];
                        break;
            }

            status.freeCapital += heldValue * status.heldPosition;
            status.heldPosition = 0;
            status.heldType = Type.None;

            double eq = status.freeCapital;

            double highestProfit = 0;

            double profitVXX = (dataVXX.adjCloses[i] - dataVXX.adjCloses[i + 83]) / dataVXX.adjCloses[i + 83];
            double profitXIV = (dataXIV.adjCloses[i] - dataXIV.adjCloses[i + 83]) / dataXIV.adjCloses[i + 83];
            double profitVXZ = (dataVXZ.adjCloses[i] - dataVXZ.adjCloses[i + 83]) / dataVXZ.adjCloses[i + 83];
            double profitZIV = (dataZIV.adjCloses[i] - dataZIV.adjCloses[i + 83]) / dataZIV.adjCloses[i + 83];

            Type newType = Type.None;
            if (profitVXX > highestProfit) {
                highestProfit = profitVXX;
                newType = Type.VXX;
            }
            if (profitXIV > highestProfit) {
                highestProfit = profitXIV;
                newType = Type.XIV;
            }
            if (profitVXZ > highestProfit) {
                highestProfit = profitVXZ;
                newType = Type.VXZ;
            }
            if (profitZIV > highestProfit) {
                highestProfit = profitZIV;
                newType = Type.ZIV;
            }

            if (highestProfit > 0) {
                double value = 0;
                switch (newType) {
                    case VXX:
                        status.heldType = Type.VXX;
                        value = dataVXX.adjCloses[i];
                        break;
                    case XIV:
                        status.heldType = Type.XIV;
                        value = dataXIV.adjCloses[i];
                        break;
                    case VXZ:
                        status.heldType = Type.VXZ;
                        value = dataVXZ.adjCloses[i];
                        break;
                    case ZIV:
                        status.heldType = Type.ZIV;
                        value = dataZIV.adjCloses[i];
                        break;
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
