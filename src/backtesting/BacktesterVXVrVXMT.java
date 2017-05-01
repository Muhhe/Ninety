/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import data.CloseData;
import data.DataGetterHistCBOE;
import data.DataGetterHistYahoo;
import data.IDataGetterHist;
import data.IndicatorCalculator;
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

    static public CloseData getRatioData() {
        IDataGetterHist getter = new DataGetterHistCBOE();

        LocalDate startDate = LocalDate.of(2010, Month.NOVEMBER, 30);

        logger.info("Loading VXV");
        CloseData dataVXV = getter.readAdjCloseData(startDate, LocalDate.now(), "VXV");
        logger.info("Loading VXMT");
        CloseData dataVXMT = getter.readAdjCloseData(startDate, LocalDate.now(), "VXMT");

        if (dataVXV.adjCloses.length != dataVXMT.adjCloses.length) {
            logger.warning("data not equal");
        }

        int ratVN1 = 0;
        int dataLength = dataVXV.adjCloses.length;
        double[] ratio = new double[dataLength];
        LocalDate[] dates = new LocalDate[dataLength];
        for (int i = 0; i < dataLength; i++) {
            ratio[dataLength - 1 - i] = dataVXV.adjCloses[i] / dataVXMT.adjCloses[i];
            dates[dataLength - 1 - i] = dataVXV.dates[i];

            if (ratio[dataLength - 1 - i] > 1) {
                ratVN1++;
            }
        }

        logger.log(BTLogLvl.BACKTEST, "Ration above 1 " + ratVN1 + " from " + dataLength);

        CloseData dataRatio = new CloseData(0);
        dataRatio.adjCloses = ratio;
        dataRatio.dates = dates;
        return dataRatio;
    }

    static private class Status {

        double capital = 0;
        Signal heldType = Signal.None;
        int position = 0;
        int portion = 0;
    }

    static public void runBacktest(BTSettings settings) {

        IDataGetterHist getter = new DataGetterHistYahoo();
        LocalDate startDate = LocalDate.of(2010, Month.NOVEMBER, 30);
        CloseData dataVXX = getter.readAdjCloseData(startDate, LocalDate.now(), "VXX");
        CloseData dataXIV = getter.readAdjCloseData(startDate, LocalDate.now(), "XIV");
        
        CloseData dataSPY = getter.readAdjCloseData(startDate, LocalDate.now(), "SPY");

        CloseData ratioData = getRatioData();

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
        int spyPos = (int) (settings.capital / dataSPY.adjCloses[startInx]);

        for (int i = startInx; i >= 0; i--) {

            double sma60 = IndicatorCalculator.SMA(60, ratioData.adjCloses, i);
            double sma125 = IndicatorCalculator.SMA(125, ratioData.adjCloses, i);
            double sma150 = IndicatorCalculator.SMA(150, ratioData.adjCloses, i);

            if (!dataXIV.dates[i].equals(ratioData.dates[i])) {
                logger.severe("DatesNotEqual!!!!!!!!!!");
            }

            int voteForXIV = 0;
            int voteForVXX = 0;

            double actRatio = ratioData.adjCloses[i];

            // Jak to teda pocitat??? (actRatio < sma60 && sma60 < 1) nebo (actRatio < sma60 && actRatio < 1) ???
            if (actRatio < sma60 && sma60 < 1) {
            //if (actRatio < sma60 && actRatio < 1) {
                voteForXIV++;
            } else if (actRatio > sma60 && sma60 > 1) {
            //} else if (actRatio > sma60 && actRatio > 1) {
                voteForVXX++;
            }

            if (actRatio < sma125 && sma125 < 1) {
            //if (actRatio < sma125 && actRatio < 1) {
                voteForXIV++;
            } else if (actRatio > sma125 && sma125 > 1) {
            //} else if (actRatio > sma125 && actRatio > 1) {
                voteForVXX++;
            }

            if (actRatio < sma150 && sma150 < 1) {
            //if (actRatio < sma150 && actRatio < 1) {
                voteForXIV++;
            } else if (actRatio > sma150 && sma150 > 1) {
            //} else if (actRatio > sma150 && actRatio > 1) {
                voteForVXX++;
            }

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
            UpdateEquityFile(eq, "equity.csv", (stat.heldType.toString() + Integer.toString(stat.portion)));
            UpdateEquityFile(xivPos * dataXIV.adjCloses[i], "vix.csv", null);
            UpdateEquityFile(spyPos * dataSPY.adjCloses[i], "spy.csv", null);

            stats.EndDay();

            if (i < 1) {
                continue;
            }

            Signal selectedSignal;
            int targetPortion = 0;
            if (voteForVXX > voteForXIV) {
                selectedSignal = Signal.VXX;
                targetPortion = voteForVXX;
            } else {
                selectedSignal = Signal.XIV;
                targetPortion = voteForXIV;
            }

            if (targetPortion == 0) {
                selectedSignal = Signal.None;
            }

            double onePortionValue;
            if (settings.reinvest) {
                onePortionValue = eq / 3.0;
            } else {
                onePortionValue = settings.capital / 3.0;
            }

            double selectedValue;
            if (selectedSignal == Signal.XIV) {
                selectedValue = dataXIV.adjCloses[i - 1];
            } else {
                selectedValue = dataVXX.adjCloses[i - 1];
            }

            if (stat.heldType != selectedSignal) {
                if (stat.heldType != Signal.None) {

                    logger.log(BTLogLvl.BACKTEST, "Change from " + stat.heldType + " to " + selectedSignal + ", por: " + targetPortion);

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
                    logger.log(BTLogLvl.BACKTEST, "Buy new " + selectedSignal + ", por: " + targetPortion);
                    int newPos = (int) (onePortionValue * targetPortion / selectedValue);
                    stat.capital -= newPos * selectedValue;
                    stat.position = newPos;
                    stat.heldType = selectedSignal;
                    stat.portion = targetPortion;
                }
            } else if (stat.portion != targetPortion) {
                logger.log(BTLogLvl.BACKTEST, "Modify " + selectedSignal + ", por: " + targetPortion);
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
            }
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
