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
import data.getters.DataGetterHistCBOE;
import data.getters.DataGetterHistFile;
import data.getters.DataGetterHistGoogle;
import data.getters.DataGetterHistYahoo;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.logging.Logger;
import strategyVXVMT.VXVMTChecker;
import strategyVXVMT.VXVMTData;
import strategyVXVMT.VXVMTDataPreparator;
import strategyVXVMT.VXVMTRunner;
import strategyVXVMT.VXVMTSignal;
import strategyVXVMT.VXVMTStatus;
import strategyVXVMT.VXVMTStrategy;
import test.BrokerNoIB;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class BacktesterVXVMT {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private enum Signal {
        VXX, XIV, None
    }

    static public CloseData getRatioData(BTSettings settings) {
        IDataGetterHist getter = new DataGetterHistCBOE();
        //IDataGetterHist getter = new DataGetterHistFile("backtest/VolData/");

        //LocalDate startDate = LocalDate.of(2010, Month.NOVEMBER, 30);
        //LocalDate endDate = LocalDate.of(2017, Month.APRIL, 28);
        logger.info("Loading VXV");
        CloseData dataVXV = getter.readAdjCloseData(settings.startDate, settings.endDate, "VXV", false);
        logger.info("Loading VXMT");
        CloseData dataVXMT = getter.readAdjCloseData(settings.startDate, settings.endDate, "VXMT", false);

        if (dataVXV.adjCloses.length != dataVXMT.adjCloses.length) {
            logger.warning("data not equal");
        }

        int dataLength = dataVXV.adjCloses.length;
        double[] ratio = new double[dataLength];
        LocalDate[] dates = new LocalDate[dataLength];
        for (int i = 0; i < dataLength; i++) {
            ratio[i] = dataVXV.adjCloses[i] / dataVXMT.adjCloses[i];
            dates[i] = dataVXV.dates[i];
        }

        CloseData dataRatio = new CloseData(0);
        dataRatio.adjCloses = ratio;
        dataRatio.dates = dates;
        return dataRatio;
    }

    static private class Status {

        double capital = 0;
        VXVMTSignal.Type heldType = VXVMTSignal.Type.None;
        int position = 0;
        double exposure;
    }

    static private class NewStatus {

        double ratio = 0;
        Signal heldType = Signal.None;
    }

    static public class MonthlyStats {

        double startingCap = 0;
        double profit = 0;
        int daysXIV = 0;
        double profitXIV = 0;
        int daysVXX = 0;
        double profitVXX = 0;
        int totalDays = 0;
        double maxDD = 0;
    }

    static private NewStatus CalcStrat(CloseData ratioData, int index) {

        double actRatio = ratioData.adjCloses[index];
        //double actRatio = IndicatorCalculator.SMA(3, ratioData.adjCloses, index);

        double[] smas = new double[]{
            IndicatorCalculator.SMA(60, ratioData.adjCloses, index),
            IndicatorCalculator.SMA(125, ratioData.adjCloses, index),
            IndicatorCalculator.SMA(150, ratioData.adjCloses, index)
        };

        double[] weights = new double[]{
            //1.0 / smas.length,
            //1.0 / smas.length,
            //1.0 / smas.length
            0.45, 0.35, 0.20
        };

        double sum = 0;
        for (double weight : weights) {
            sum += weight;
        }

        assert (sum == 1);

        double voteForXIV = 0;
        double voteForVXX = 0;

        for (int i = 0; i < smas.length; i++) {
            if (actRatio < smas[i] && actRatio < 1) {
                voteForXIV += weights[i];
            } else if (actRatio > smas[i] && actRatio > 1) {
                voteForVXX += weights[i];
            }
        }

        Signal selectedSignal = Signal.None;
        double targetPortion = 0;
        if (voteForVXX > voteForXIV) {
            selectedSignal = Signal.VXX;
            targetPortion = voteForVXX;
            if (voteForVXX < 0.7) {
                selectedSignal = Signal.None;
                targetPortion = 0;
            }
        } else if (voteForVXX < voteForXIV) {
            selectedSignal = Signal.XIV;
            targetPortion = voteForXIV;
        }

        NewStatus newStatus = new NewStatus();
        newStatus.heldType = selectedSignal;
        newStatus.ratio = targetPortion;

        return newStatus;
    }

    static private VXVMTData GetDataForDay(int dayInx, CloseData dataVXV, CloseData dataVXMT) {

        VXVMTData data = new VXVMTData();
        data.dataVXMT = new CloseData(0);
        data.dataVXV = new CloseData(0);

        data.dataVXV.adjCloses = Arrays.copyOfRange(dataVXV.adjCloses, dayInx, dayInx + 151);
        data.dataVXV.dates = Arrays.copyOfRange(dataVXV.dates, dayInx, dayInx + 151);

        data.dataVXMT.adjCloses = Arrays.copyOfRange(dataVXMT.adjCloses, dayInx, dayInx + 151);
        data.dataVXMT.dates = Arrays.copyOfRange(dataVXMT.dates, dayInx, dayInx + 151);

        //VXVMTChecker.CheckTickerData(data.dataVXV, "VXV");
        //VXVMTChecker.CheckTickerData(data.dataVXMT, "VXMT");
        return data;
    }

    static private int GetMaxPosition(VXVMTSignal.Type signalType, VXVMTData data, VXVMTStatus status) {
        return GetDesiredPosition(signalType, 1, data, status);
    }

    static private int GetDesiredPosition(VXVMTSignal.Type signalType, double exposure, VXVMTData data, VXVMTStatus status) {
        double value = 0;

        if (signalType == VXVMTSignal.Type.VXX) {
            value = data.indicators.actVXXvalue;
        } else {
            value = data.indicators.actXIVvalue;
        }

        // Budget is lowered by 1% for safety reasons (slipage etc.)
        double budget = status.GetEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue) * 0.99;

        return (int) (budget / value * exposure);
    }

    static public void runBacktest(BTSettings settings) {
        IDataGetterHist getterFile = new DataGetterHistFile("backtest/VolData/");
        //IDataGetterHist getterFile = new DataGetterHistGoogle();
        CloseData dataVXX = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "VXX", false);
        CloseData dataXIV = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "XIV", false);

        IDataGetterHist getterCBOE = new DataGetterHistCBOE();
        logger.info("Loading VXV");
        CloseData dataVXV = getterCBOE.readAdjCloseData(settings.startDate, settings.endDate, "VXV", false);
        logger.info("Loading VXMT");
        CloseData dataVXMT = getterCBOE.readAdjCloseData(settings.startDate, settings.endDate, "VXMT", false);

        if (dataVXV.adjCloses.length != dataVXMT.adjCloses.length) {
            logger.warning("data not equal");
        }

        TradeTimer.LoadSpecialTradingDays();

        BTStatistics stats = new BTStatistics(settings.capital, settings.reinvest);

        File equityFile = new File("equity.csv");
        equityFile.delete();
        File xivFile = new File("xiv.csv");
        xivFile.delete();

        int startInx = dataVXV.dates.length - 152;
        int xivPos = (int) (settings.capital / dataXIV.adjCloses[startInx]);
        //int spyPos = (int) (settings.capital / dataSPY.adjCloses[startInx]);

        double profitXIV = 0;
        double profitVXX = 0;
        double lastCapital = settings.capital;

        int daysXIV = 0;
        int daysVXX = 0;

        MonthlyStats monthStats = new MonthlyStats();
        LocalDate lastDate = LocalDate.MIN;
        CreateMonthlyStatsFile();

        IBroker broker = new BrokerNoIB();
        VXVMTStatus status = new VXVMTStatus();
        status.freeCapital = settings.capital;

        VXVMTRunner runner = new VXVMTRunner(status, broker);

        double dokup = 0;
        double lastKup = 0;
        for (int i = startInx; i >= 1; i--) {

            LocalDate date = dataXIV.dates[i];
            TradeTimer.SetToday(date);

            logger.info("Day - " + date.toString());

            VXVMTData data = GetDataForDay(i, dataVXV, dataVXMT);
            VXVMTDataPreparator.ComputeIndicators(data);
            data.indicators.actVXXvalue = dataVXX.adjCloses[i];
            data.indicators.actXIVvalue = dataXIV.adjCloses[i];
            VXVMTChecker.CheckDataIndicators(data);

            if (status.heldType == VXVMTSignal.Type.XIV) {
                status.freeCapital += data.indicators.actXIVvalue * status.heldPosition;
                status.heldPosition = 0;
            }
            if (status.heldType == VXVMTSignal.Type.VXX) {
                status.freeCapital += data.indicators.actVXXvalue * status.heldPosition;
                status.heldPosition = 0;
            }

            double eq = status.GetEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue);

            if (status.heldType == VXVMTSignal.Type.XIV) {
                profitXIV += (eq - lastCapital) / lastCapital * 100;
            }
            if (status.heldType == VXVMTSignal.Type.VXX) {
                profitVXX += (eq - lastCapital) / lastCapital * 100;
            }

            // runner.RunStrategy(data);
            VXVMTSignal signal = VXVMTStrategy.CalculateFinalSignal(data);

            boolean nope = false;
            for (int j = 0; j < 3; j++) {
                if (signal.VXXSignals[j]) {
                    nope = true;
                }
            }
            
            if (signal.exposure < 0.0 && !nope) {
                double rsi = IndicatorCalculator.RSI(dataXIV.adjCloses, 3, i);
                if (dokup != 0) {
                    if ((lastKup - data.indicators.actXIVvalue) / lastKup > 0.05) {
                        dokup += 2* dokup;
                        dokup = Double.min(1, dokup);
                        logger.log(BTLogLvl.BACKTEST, "Dokup: " + dokup);
                    }
                } else if (rsi < 20.0) {
                    dokup = 0.2;
                    lastKup = data.indicators.actXIVvalue;
                    logger.log(BTLogLvl.BACKTEST, "New dokup: " + dokup);
                }
                double sma = IndicatorCalculator.SMA(5, dataXIV.adjCloses, i);
                if (sma < data.indicators.actXIVvalue) {
                    dokup = 0;
                    logger.log(BTLogLvl.BACKTEST, "Prodej: " + dokup);
                }
            }
            else {
                dokup = 0;
                lastKup = 0;
            }
            
            if (dokup > 0) {
                signal.exposure = dokup;
                signal.type = VXVMTSignal.Type.XIV;
            }

            if (signal.type == VXVMTSignal.Type.XIV) {
                status.heldPosition = GetDesiredPosition(VXVMTSignal.Type.XIV, signal.exposure, data, status);
                status.freeCapital -= data.indicators.actXIVvalue * status.heldPosition;
            } else if (signal.type == VXVMTSignal.Type.VXX) {
                status.heldPosition = GetDesiredPosition(VXVMTSignal.Type.VXX, signal.exposure, data, status);
                status.freeCapital -= data.indicators.actVXXvalue * status.heldPosition;
            }
            status.heldType = signal.type;

            stats.StartDay(date);

            stats.UpdateEquity(eq, date);
            //UpdateEquityFile(eq, "equity.csv", (status.heldType.toString() + " - " + TradeFormatter.toString(1 - (status.freeCapital / eq))));
            UpdateEquityFile(eq, "equity.csv", (status.heldType.toString() + " - " + TradeFormatter.toString(signal.exposure) + " - " + TradeFormatter.toString(dokup)));
            UpdateEquityFile(xivPos * dataXIV.adjCloses[i], "xiv.csv", null);
            //UpdateEquityFile(spyPos * dataSPY.adjCloses[i], "spy.csv", null);*/

            lastCapital = eq;

            stats.EndDay();

            if (lastDate.getMonth() != date.getMonth() && !lastDate.isEqual(LocalDate.MIN)) {
                UpdateMonthlyStats(lastDate, monthStats);
                monthStats = new MonthlyStats();
            }

            lastDate = date;

            monthStats.totalDays++;
        }

        UpdateMonthlyStats(lastDate, monthStats);

        stats.LogStats(settings);
        stats.SaveEquityToCsv();
        double totalProfit = profitXIV + profitVXX;
        double profitXIVProc = profitXIV;// / totalProfit * 100;
        double profitVXXProc = profitVXX;// / totalProfit * 100;
        logger.log(BTLogLvl.BACKTEST, "Profit XIV: " /*+ TradeFormatter.toString(profitXIV) + "$ = " */ + TradeFormatter.toString(profitXIVProc)
                + "%, Profit VXX: " /*+ TradeFormatter.toString(profitVXX) + "$ = "*/ + TradeFormatter.toString(profitVXXProc) + "%");

        double daysXIVproc = (double) daysXIV / startInx * 100.0;
        double daysVXXproc = (double) daysVXX / startInx * 100.0;
        logger.log(BTLogLvl.BACKTEST, "Days in XIV: " + daysXIV + " = " + TradeFormatter.toString(daysXIVproc)
                + "%, Days in VXX: " + daysVXX + " = " + TradeFormatter.toString(daysVXXproc) + "%, Days total: " + startInx);

        logger.log(BTLogLvl.BACKTEST, "Fees: " + status.fees);

    }

    static public void runBacktest2(BTSettings settings) {

        IDataGetterHist getter = new DataGetterHistFile("backtest/VolData/");
        CloseData dataVXX = getter.readAdjCloseData(settings.startDate, settings.endDate, "VXX", false);
        CloseData dataXIV = getter.readAdjCloseData(settings.startDate, settings.endDate, "XIV", false);

        CloseData dataSPY = getter.readAdjCloseData(settings.startDate, settings.endDate, "SPY", false);

        CloseData ratioData = getRatioData(settings);

        logger.log(BTLogLvl.BACKTEST, "Ratio data loaded");

        Status stat = new Status();
        stat.capital = settings.capital;

        BTStatistics stats = new BTStatistics(settings.capital, settings.reinvest);

        File equityFile = new File("equity.csv");
        equityFile.delete();
        File vixFile = new File("vix.csv");
        vixFile.delete();

        int startInx = ratioData.dates.length - 151;
        int xivPos = (int) (settings.capital / dataXIV.adjCloses[startInx]);
        //int spyPos = (int) (settings.capital / dataSPY.adjCloses[startInx]);

        double profitXIV = 0;
        double profitVXX = 0;
        double lastCapital = settings.capital;

        int daysXIV = 0;
        int daysVXX = 0;

        MonthlyStats monthStats = new MonthlyStats();
        LocalDate lastDate = LocalDate.MIN;
        CreateMonthlyStatsFile();

        for (int i = startInx; i >= 1; i--) {

            if (!dataXIV.dates[i].equals(ratioData.dates[i])) {
                logger.severe("DatesNotEqual!!!!!!!!!!");
            }

            double currentValue;
            if (stat.heldType == VXVMTSignal.Type.XIV) {
                currentValue = dataXIV.adjCloses[i];
            } else {
                currentValue = dataVXX.adjCloses[i];
            }

            LocalDate date = dataXIV.dates[i];
            TradeTimer.SetToday(date);
            double eq = (stat.capital + stat.position * currentValue);

            stats.StartDay(date);
            stats.UpdateEquity(eq, date);
            UpdateEquityFile(eq, "equity.csv", (stat.heldType.toString() + " - " + TradeFormatter.toString(stat.exposure)));
            UpdateEquityFile(xivPos * dataXIV.adjCloses[i], "vix.csv", null);
            //UpdateEquityFile(spyPos * dataSPY.adjCloses[i], "spy.csv", null);

            stats.EndDay();

            if (lastDate.getMonth() != date.getMonth() && !lastDate.isEqual(LocalDate.MIN)) {
                UpdateMonthlyStats(lastDate, monthStats);
                monthStats = new MonthlyStats();
            }

            lastDate = date;

            monthStats.totalDays++;

            VXVMTData indicators = new VXVMTData();

            /*indicators.actRatioLagged = ratioData.adjCloses[i + 1];
            indicators.ratiosLagged[0] = IndicatorCalculator.SMA(60, ratioData.adjCloses, i + 1);
            indicators.ratiosLagged[1] = IndicatorCalculator.SMA(125, ratioData.adjCloses, i + 1);
            indicators.ratiosLagged[2] = IndicatorCalculator.SMA(150, ratioData.adjCloses, i + 1);

            indicators.actRatio = ratioData.adjCloses[i];
            indicators.ratios[0] = IndicatorCalculator.SMA(60, ratioData.adjCloses, i);
            indicators.ratios[1] = IndicatorCalculator.SMA(125, ratioData.adjCloses, i);
            indicators.ratios[2] = IndicatorCalculator.SMA(150, ratioData.adjCloses, i);*/
            VXVMTSignal signal = VXVMTStrategy.CalculateFinalSignal(indicators);

            //NewStatus newStat = CalcStrat(ratioData, i);
            // Sell all
            if (stat.heldType != VXVMTSignal.Type.None) {
                double heldValue;
                if (stat.heldType == VXVMTSignal.Type.XIV) {
                    heldValue = dataXIV.adjCloses[i];
                    daysXIV++;
                    monthStats.daysXIV++;
                } else {
                    heldValue = dataVXX.adjCloses[i];
                    daysVXX++;
                    monthStats.daysVXX++;
                }

                stat.capital += stat.position * heldValue;

                double profit = stat.capital - lastCapital;
                double profitProc = profit / stat.capital * 100;
                monthStats.profit += profit;
                lastCapital = stat.capital;

                if (stat.heldType == VXVMTSignal.Type.XIV) {
                    profitXIV += profitProc;
                    monthStats.profitXIV += profit;
                } else {
                    profitVXX += profitProc;
                    monthStats.profitVXX += profit;
                }

                stat.position = 0;
                stat.heldType = VXVMTSignal.Type.None;
                stat.exposure = 0;
            }

            //VXVMTSignal tomorowStat = CalcStrat(ratioData, i - 1);
            //if ((tomorowStat.heldType == signal.type) || (signal.type != Signal.VXX)) {
            //Buy new
            if (signal.type != VXVMTSignal.Type.None) {
                double newValue;
                if (signal.type == VXVMTSignal.Type.XIV) {
                    newValue = dataXIV.adjCloses[i];
                } else {
                    newValue = dataVXX.adjCloses[i];
                }

                int newPos = (int) (signal.exposure * stat.capital / newValue);
                stat.capital -= newPos * newValue;
                stat.position = newPos;
                stat.heldType = signal.type;
                stat.exposure = signal.exposure;

            }
            //}
        }

        UpdateMonthlyStats(lastDate, monthStats);

        stats.LogStats(settings);
        stats.SaveEquityToCsv();
        double totalProfit = profitXIV + profitVXX;
        double profitXIVProc = profitXIV;// / totalProfit * 100;
        double profitVXXProc = profitVXX;// / totalProfit * 100;
        logger.log(BTLogLvl.BACKTEST, "Profit XIV: " /*+ TradeFormatter.toString(profitXIV) + "$ = " */ + TradeFormatter.toString(profitXIVProc)
                + "%, Profit VXX: " /*+ TradeFormatter.toString(profitVXX) + "$ = "*/ + TradeFormatter.toString(profitVXXProc) + "%");

        double daysXIVproc = (double) daysXIV / startInx * 100.0;
        double daysVXXproc = (double) daysVXX / startInx * 100.0;
        logger.log(BTLogLvl.BACKTEST, "Days in XIV: " + daysXIV + " = " + TradeFormatter.toString(daysXIVproc)
                + "%, Days in VXX: " + daysVXX + " = " + TradeFormatter.toString(daysVXXproc) + "%, Days total: " + startInx);
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

    static public void CreateMonthlyStatsFile() {
        File file = new File("monthlyStats.csv");
        file.delete();

        Writer writer = null;
        try {
            File equityFile = new File("monthlyStats.csv");
            equityFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(equityFile, true), "UTF-8"));
            writer.write("Month, Profit, Days, XIV, %, Profit XIV, Days VXX, %, ProfitVXX");
            writer.write("\r\n");

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

    static public void UpdateMonthlyStats(LocalDate date, MonthlyStats stats) {
        Writer writer = null;
        try {
            File equityFile = new File("monthlyStats.csv");
            equityFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(equityFile, true), "UTF-8"));
            writer.write(date.format(DateTimeFormatter.ofPattern("YYYY/MM")));
            writer.write(",");
            writer.write(TradeFormatter.toString(stats.profit));
            writer.write(",");
            writer.write(Integer.toString(stats.totalDays));
            writer.write(",");
            writer.write(Integer.toString(stats.daysXIV));
            writer.write(",");
            writer.write(TradeFormatter.toString(((double) stats.daysXIV / stats.totalDays * 100.0)));
            writer.write(",");
            writer.write(TradeFormatter.toString(stats.profitXIV));
            writer.write(",");
            writer.write(Integer.toString(stats.daysVXX));
            writer.write(",");
            writer.write(TradeFormatter.toString(((double) stats.daysVXX / stats.totalDays * 100.0)));
            writer.write(",");
            writer.write(TradeFormatter.toString(stats.profitVXX));
            writer.write("\r\n");

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
