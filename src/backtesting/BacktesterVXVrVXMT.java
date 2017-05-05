/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import data.CloseData;
import data.getters.DataGetterHistCBOE;
import data.getters.DataGetterHistYahoo;
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
import java.time.Month;
import java.util.logging.Logger;
import test.TestPlatform;
import tradingapp.FilePaths;
import tradingapp.TradeOrder;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class BacktesterVXVrVXMT {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private enum Signal {
        VXX, XIV, None
    }

    static public CloseData getRatioData(BTSettings settings) {
        //IDataGetterHist getter = new DataGetterHistCBOE();
        IDataGetterHist getter = new DataGetterHistFile("backtest/VolData/");

        //LocalDate startDate = LocalDate.of(2010, Month.NOVEMBER, 30);
        //LocalDate endDate = LocalDate.of(2017, Month.APRIL, 28);
        logger.info("Loading VXV");
        CloseData dataVXV = getter.readAdjCloseData(settings.startDate, settings.endDate, "VXV");
        logger.info("Loading VXMT");
        CloseData dataVXMT = getter.readAdjCloseData(settings.startDate, settings.endDate, "VXMT");

        if (dataVXV.adjCloses.length != dataVXMT.adjCloses.length) {
            logger.warning("data not equal");
        }

        int dataLength = dataVXV.adjCloses.length;
        double[] ratio = new double[dataLength];
        LocalDate[] dates = new LocalDate[dataLength];
        for (int i = 0; i < dataLength; i++) {
            /*ratio[dataLength - 1 - i] = dataVXV.adjCloses[i] / dataVXMT.adjCloses[i];
            dates[dataLength - 1 - i] = dataVXV.dates[i];

            if (ratio[dataLength - 1 - i] > 1) {
                ratVN1++;
            }*/

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
        Signal heldType = Signal.None;
        int position = 0;
    }

    static private class NewStatus {

        double ratio = 0;
        Signal heldType = Signal.None;
    }

    static private NewStatus CalcStratQST(CloseData ratioData, int index) {

        double actRatio = ratioData.adjCloses[index];
        //double actRatio = IndicatorCalculator.SMA(3, ratioData.adjCloses, index);

        double[] smas = new double[]{
            IndicatorCalculator.SMA(60, ratioData.adjCloses, index)
            //, IndicatorCalculator.SMA(125, ratioData.adjCloses, index)
            //, IndicatorCalculator.SMA(150, ratioData.adjCloses, index)
        };

        double[] weights = new double[]{
            1.0 / smas.length
            , 1.0 / smas.length
            , 1.0 / smas.length
        };
        
        double sum = 0;
        for (double weight : weights) {
            sum += weight;
        }
        
        assert(sum == 1);

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
        } else if (voteForVXX < voteForXIV) {
            selectedSignal = Signal.XIV;
            targetPortion = voteForXIV;
        }

        NewStatus newStatus = new NewStatus();
        newStatus.heldType = selectedSignal;
        newStatus.ratio = targetPortion;

        return newStatus;
    }

    static public void runBacktest(BTSettings settings) {

        IDataGetterHist getter = new DataGetterHistFile("backtest/VolData/");
        CloseData dataVXX = getter.readAdjCloseData(settings.startDate, settings.endDate, "VXX");
        CloseData dataXIV = getter.readAdjCloseData(settings.startDate, settings.endDate, "XIV");

        CloseData dataSPY = getter.readAdjCloseData(settings.startDate, settings.endDate, "SPY");

        CloseData ratioData = getRatioData(settings);

        logger.log(BTLogLvl.BACKTEST, "Ratio data loaded");

        Status stat = new Status();
        stat.capital = settings.capital;

        BTStatistics stats = new BTStatistics(settings.capital, settings.reinvest);

        File equityFile = new File("equity.csv");
        equityFile.delete();
        File vixFile = new File("vix.csv");
        vixFile.delete();

        int startInx = ratioData.dates.length - 150;
        int xivPos = (int) (settings.capital / dataXIV.adjCloses[startInx]);
        //int spyPos = (int) (settings.capital / dataSPY.adjCloses[startInx]);

        for (int i = startInx; i >= 0; i--) {

            if (!dataXIV.dates[i].equals(ratioData.dates[i])) {
                logger.severe("DatesNotEqual!!!!!!!!!!");
            }

            //VMS
            /*if (actRatio < sma60 && sma60 < 1) {
                voteForXIV++;
            } else if (actRatio > sma60 && sma60 > 1) {
                voteForVXX++;
            }

            if (actRatio < sma125 && sma125 < 1) {
                voteForXIV++;
            } else if (actRatio > sma125 && sma125 > 1) {
                voteForVXX++;
            }

            if (actRatio < sma150 && sma150 < 1) {
                voteForXIV++;
            } else if (actRatio > sma150 && sma150 > 1) {
                voteForVXX++;
            }*/
            double currentValue;
            if (stat.heldType == Signal.XIV) {
                currentValue = dataXIV.adjCloses[i];
            } else {
                currentValue = dataVXX.adjCloses[i];
            }

            LocalDate date = dataXIV.dates[i];
            TradeTimer.SetToday(date);
            double eq = (stat.capital + stat.position * currentValue);
            stats.StartDay(date);
            stats.UpdateEquity(eq, date);
            UpdateEquityFile(eq, "equity.csv", (stat.heldType.toString() /*+ Integer.toString(stat.portion)*/));
            UpdateEquityFile(xivPos * dataXIV.adjCloses[i], "vix.csv", null);
            //UpdateEquityFile(spyPos * dataSPY.adjCloses[i], "spy.csv", null);

            stats.EndDay();

            if (i < 1) {
                continue;
            }

            NewStatus newStat = CalcStratQST(ratioData, i);

            // Sell all
            if (stat.heldType != Signal.None) {
                double heldValue;
                if (stat.heldType == Signal.XIV) {
                    heldValue = dataXIV.adjCloses[i - 1];
                } else {
                    heldValue = dataVXX.adjCloses[i - 1];
                }

                stat.capital += stat.position * heldValue;
                stat.position = 0;
                stat.heldType = Signal.None;
            }

            //Buy new
            if (newStat.heldType != Signal.None) {
                double newValue;
                if (newStat.heldType == Signal.XIV) {
                    newValue = dataXIV.adjCloses[i - 1];
                } else {
                    newValue = dataVXX.adjCloses[i - 1];
                }

                int newPos = (int) (newStat.ratio * stat.capital / newValue);
                stat.capital -= newPos * newValue;
                stat.position = newPos;
                stat.heldType = newStat.heldType;

            }

            /*if (stat.heldType != selectedSignal) {
                if (stat.heldType != Signal.None) {

                    //logger.log(BTLogLvl.BACKTEST, "Change from " + stat.heldType + " to " + selectedSignal + ", por: " + targetPortion);
                    double heldValue;
                    if (stat.heldType == Signal.XIV) {
                        heldValue = dataXIV.adjCloses[i - 1];
                    } else {
                        heldValue = dataVXX.adjCloses[i - 1];
                    }

                    stat.capital += stat.position * heldValue;
                    stat.position = 0;
                    stat.heldType = Signal.None;
                    stat.portion = 0;

                    if (selectedSignal != Signal.None) {

                        int newPos = (int) (onePortionValue * targetPortion / selectedValue);
                        stat.capital -= newPos * selectedValue;
                        stat.position = newPos;
                        stat.heldType = selectedSignal;
                        stat.portion = targetPortion;

                    }
                } else {
                    //logger.log(BTLogLvl.BACKTEST, "Buy new " + selectedSignal + ", por: " + targetPortion);
                    int newPos = (int) (onePortionValue * targetPortion / selectedValue);
                    stat.capital -= newPos * selectedValue;
                    stat.position = newPos;
                    stat.heldType = selectedSignal;
                    stat.portion = targetPortion;
                }
            } else if (stat.portion != targetPortion) {
                //logger.log(BTLogLvl.BACKTEST, "Modify " + selectedSignal + ", por: " + targetPortion);
                int newPos = (int) (onePortionValue * targetPortion / selectedValue);
                int diffPos = newPos - stat.position;

                if (diffPos > 0) {
                    stat.capital -= diffPos * selectedValue;
                } else if (diffPos < 0) {
                    stat.capital -= diffPos * selectedValue;
                }

                stat.position = newPos;
                stat.heldType = selectedSignal;
                stat.portion = targetPortion;
            }*/
        }

        stats.LogStats(settings);
        stats.SaveEquityToCsv();
        logger.log(BTLogLvl.BACKTEST, "END");
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
