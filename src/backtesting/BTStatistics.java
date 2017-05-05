/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import strategy90.StatusDataForNinety;
import tradingapp.TradeFormatter;

/**
 *
 * @author Muhe
 */
public class BTStatistics {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public class EquityInTime {

        LocalDate date;
        double equity;
    }

    public class TradeYearlyStats {

        public int profit = 0;
        public int totalSells = 0;
        public int profitSells = 0;

        public double highestEquity = 0;
        public double highestDDproc = 0;
        public double highestDD = 0;
        public LocalDate dateOfHighestDD = LocalDate.MIN;
        public double fees = 0;

        public int year;
        public int days = 0;
        public double startCapital = 0;

        public TradeYearlyStats(int year) {
            this.year = year;
        }
    }

    public boolean reinvest;

    public double startCapital = 0;
    public double capital = 0;
    public double equity = 0;

    List<EquityInTime> equityList = new ArrayList<>();
    private LocalDate currentDate = LocalDate.MIN;

    public List<TradeYearlyStats> yearlyStats = new ArrayList<>();

    public BTStatistics(double capital, boolean reinvest) {
        this.startCapital = capital;
        this.capital = capital;
        this.equity = capital;
        this.reinvest = reinvest;
    }

    public void StartDay(LocalDate date) {

        if (currentDate.isEqual(LocalDate.MIN)
                || currentDate.getYear() != date.getYear()) {
            if (reinvest) {
                capital = equity;
            }
            NewYear(date.getYear());
        }

        GetThisYearStats().days++;

        currentDate = date;
    }

    public void EndDay() {
        EquityInTime eq = new EquityInTime();
        eq.date = currentDate;
        eq.equity = equity;

        equityList.add(eq);
    }

    private void NewYear(int year) {
        TradeYearlyStats yearStats = new TradeYearlyStats(year);
        yearStats.startCapital = capital;
        yearlyStats.add(yearStats);
    }

    private TradeYearlyStats GetThisYearStats() {
        return yearlyStats.get(yearlyStats.size() - 1);
    }

    public void UpdateEquity(double newEq, LocalDate date) {

        GetThisYearStats().profit += newEq - equity;

        equity = newEq;

        if (GetThisYearStats().highestEquity < equity) {
            GetThisYearStats().highestEquity = equity;
        }

        double dd = ((GetThisYearStats().highestEquity - equity) / (GetThisYearStats().highestEquity)) * 100;

        if (GetThisYearStats().highestDDproc < dd) {
            GetThisYearStats().highestDD = GetThisYearStats().highestEquity - equity;
            GetThisYearStats().highestDDproc = dd;
            GetThisYearStats().dateOfHighestDD = date;
        }
    }

    public void AddSell(double profit, int position, LocalDate date) {
        UpdateEquity(equity + profit, date);

        double fee = StatusDataForNinety.GetOrderFee(position);

        GetThisYearStats().profit -= fee;
        equity -= fee;

        GetThisYearStats().fees += fee;

        GetThisYearStats().totalSells++;

        if (profit > 0) {
            GetThisYearStats().profitSells++;
        }
    }

    public void addBuy(int position) {
        double fee = StatusDataForNinety.GetOrderFee(position);
        equity -= fee;
        GetThisYearStats().profit -= fee;
        GetThisYearStats().fees += fee;
    }

    public void LogStats(BTSettings settings) {

        logger.log(BTLogLvl.BT_STATS, "Backtest completed with settings | " + settings.toString());

        double totalProfit = 0;
        long totalDays = 0;
        double fees = 0;
        double highestDDproc = 0;
        LocalDate dateOfHighestDD = LocalDate.MIN;
        int totalSells = 0;
        int profitSells = 0;

        for (TradeYearlyStats thisYearStat : yearlyStats) {

            double profit = thisYearStat.profit;
            double profitPercent = thisYearStat.profit / thisYearStat.startCapital * 100;

            logger.log(BTLogLvl.BT_STATS, thisYearStat.year + " days: " + thisYearStat.days
                    + " | profit = " + TradeFormatter.toString(profit) + "$ = " + TradeFormatter.toString(profitPercent)
                    + "% | max DD = " + TradeFormatter.toString(thisYearStat.highestDDproc) + "% (" + thisYearStat.dateOfHighestDD
                    + ") "/*| fees = " + TradeFormatter.toString(thisYearStat.fees)
                    + "$ | closed trades = " + thisYearStat.totalSells
                    + " | successful = " + TradeFormatter.toString((double) thisYearStat.profitSells / (double) thisYearStat.totalSells * 100.0)
                    + "%"*/);

            totalProfit += profit;
            totalDays += thisYearStat.days;
            fees += thisYearStat.fees;

            if (thisYearStat.highestDDproc > highestDDproc) {
                highestDDproc = thisYearStat.highestDDproc;
                dateOfHighestDD = thisYearStat.dateOfHighestDD;
            }

            totalSells += thisYearStat.totalSells;
            profitSells += thisYearStat.profitSells;
        }

        double profitPercent = totalProfit / startCapital * 100;

        logger.log(BTLogLvl.BT_STATS, "Total stats"
                + " | profit = " + TradeFormatter.toString(totalProfit) + "$ = " + TradeFormatter.toString(profitPercent)
                + "% | max DD = " + TradeFormatter.toString(highestDDproc) + "% (" + dateOfHighestDD
                + ") | fees = " + TradeFormatter.toString(fees)
                + "$ | closed trades = " + totalSells
                + " | successful = " + TradeFormatter.toString((double) profitSells / (double) totalSells * 100.0)
                + "%");

        double avgProfitPercent = profitPercent / ((double) totalDays / 252.0);
        logger.log(BTLogLvl.BT_STATS, "Average yearly profit: " + TradeFormatter.toString(avgProfitPercent) + "%");
    }

    public void SaveEquityToCsv() {

        logger.log(BTLogLvl.BACKTEST, "Saving equity to CSV");

        File file = new File("backtest/cache/_equity.csv");
        File directory = new File(file.getParentFile().getAbsolutePath());
        directory.mkdirs();
        BufferedWriter output = null;
        try {
            file.delete();
            file.createNewFile();
            output = new BufferedWriter(new FileWriter(file));

            for (BTStatistics.EquityInTime equityInTime : equityList) {
                double profit = equityInTime.equity;
                LocalDate date = equityInTime.date;

                output.write(date.toString());
                output.write(",");
                output.write(TradeFormatter.toString(profit));
                output.newLine();
            }

        } catch (IOException ex) {
            logger.warning("Cannot create equity CSV");
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
