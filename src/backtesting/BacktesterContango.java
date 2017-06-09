/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import static backtesting.BacktesterVXVMT.CreateMonthlyStatsFile;
import static backtesting.BacktesterVXVMT.UpdateEquityFile;
import static backtesting.BacktesterVXVMT.UpdateMonthlyStats;
import communication.IBroker;
import data.CloseData;
import data.getters.DataGetterHistCBOE;
import data.getters.DataGetterHistFile;
import data.getters.IDataGetterHist;
import java.io.File;
import java.time.LocalDate;
import java.util.logging.Logger;
import strategyVXVMT.VXVMTChecker;
import strategyVXVMT.VXVMTData;
import strategyVXVMT.VXVMTDataPreparator;
import strategyVXVMT.VXVMTRunner;
import strategyVXVMT.VXVMTStatus;
import test.BrokerNoIB;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class BacktesterContango {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private enum Signal {
        VXX, XIV, None
    }

    static public void runBacktest(BTSettings settings) {
        IDataGetterHist getterFile = new DataGetterHistFile("backtest/VolData/");
        CloseData dataVIXM1 = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "VIX M1");
        CloseData dataVIXM2 = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "VIX M2");

        CloseData dataVXX = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "VXX");
        CloseData dataXIV = getterFile.readAdjCloseData(settings.startDate, settings.endDate, "XIV");

        TradeTimer.LoadSpecialTradingDays();

        BTStatistics stats = new BTStatistics(settings.capital, settings.reinvest);

        File equityFile = new File("equity.csv");
        equityFile.delete();

        int startInx = dataVIXM1.dates.length - 2;

        double profitXIV = 0;
        double profitVXX = 0;

        int daysXIV = 0;
        int daysVXX = 0;

        BacktesterVXVMT.MonthlyStats monthStats = new BacktesterVXVMT.MonthlyStats();
        LocalDate lastDate = LocalDate.MIN;
        CreateMonthlyStatsFile();

        double freeCash = 10000;
        double heldPosition = 0;
        Signal heldType = Signal.None;

        double lastEquity = 10000;

        for (int i = startInx; i >= 1; i--) {

            LocalDate date = dataVIXM1.dates[i];
            TradeTimer.SetToday(date);

            logger.info("Day - " + date.toString());

            double heldValue = 0;
            if (heldType == Signal.XIV) {
                heldValue = dataXIV.adjCloses[i];
            } else {
                heldValue = dataVXX.adjCloses[i];

            }

            freeCash += heldValue * heldPosition;

            double profit = freeCash - lastEquity;
            double profitProc = profit / freeCash * 100.0;

            if (heldType == Signal.XIV) {
                profitXIV += profitProc;
            } else {
                profitVXX += profitProc;

            }

            lastEquity = freeCash;

            stats.StartDay(date);

            stats.UpdateEquity(freeCash, date);
            UpdateEquityFile(freeCash, "equity.csv", heldType.toString());

            Signal signal = Signal.None;
            if (dataVIXM1.adjCloses[i + 1] < dataVIXM2.adjCloses[i + 1]) {
                signal = Signal.XIV;
            } else if (dataVIXM1.adjCloses[i + 1] > dataVIXM2.adjCloses[i + 1]) {
                signal = Signal.VXX;
            }

            double buyValue = 0;
            if (signal == Signal.XIV) {
                buyValue = dataXIV.adjCloses[i];
                daysXIV++;
            } else {
                buyValue = dataVXX.adjCloses[i];
                daysVXX++;
            }

            /*if (signal == Signal.VXX) {
                heldType = Signal.None;
                heldPosition = 0;
            } else {*/
                heldPosition = (freeCash / buyValue);
                freeCash -= buyValue * heldPosition;
                heldType = signal;
            //}
            stats.EndDay();

            if (lastDate.getMonth() != date.getMonth() && !lastDate.isEqual(LocalDate.MIN)) {
                UpdateMonthlyStats(lastDate, monthStats);
                monthStats = new BacktesterVXVMT.MonthlyStats();
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

        //logger.log(BTLogLvl.BACKTEST, "Fees: " + status.fees);
    }
}
