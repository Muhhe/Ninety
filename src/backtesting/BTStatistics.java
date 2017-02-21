/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
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

    public class TradeStats {

        public int profit = 0;
        public int totalSells = 0;
        public int profitSells = 0;

        public double highestequity = 0;
        public double highestDDproc = 0;
        public double highestDD = 0;
        public LocalDate dateOfHighestDD = LocalDate.MIN;
        public int fees = 0;

        public int year;
        public int days = 0; //TODO

        public TradeStats(int year) {
            this.year = year;
        }
    }

    public double capital = 0;
    public double equity = 0;

    List<EquityInTime> equityList = new ArrayList<>();
    private LocalDate currentDate = LocalDate.MIN;

    //public TradeStats globalStats = new TradeStats();
    public Map<Integer, TradeStats> yearlyStats = new HashMap<>();

    public BTStatistics(double capital) {
        this.capital = capital;
        this.equity = capital;
    }

    public void StartDay(LocalDate date, boolean reinvest) {
        EquityInTime eq = new EquityInTime();
        eq.date = date;
        eq.equity = equity;

        equityList.add(eq);

        if (currentDate.isEqual(LocalDate.MIN)
            || currentDate.getYear() != date.getYear()) {
            NewYear(date.getYear());
            if (reinvest) {
                capital = equity;
            }
        }

        currentDate = date;
    }

    private void NewYear(int year) {
        yearlyStats.put(year, new TradeStats(year));
    }

    private TradeStats GetThisYearStats() {
        return yearlyStats.get(currentDate.getYear());
    }

    public void AddSell(double profit, LocalDate date) {
        profit -= 1;
        GetThisYearStats().fees++;
        equity += profit;

        GetThisYearStats().profit += profit;

        GetThisYearStats().totalSells++;
        if (profit > 0) {
            GetThisYearStats().profitSells++;

            if (GetThisYearStats().highestequity < equity) {
                GetThisYearStats().highestequity = equity;
            }
        } else {
            //double dd = ((highestProfit - totalProfit) / (capital + highestProfit)) * 100;
            double dd = ((GetThisYearStats().highestequity - equity) / (capital)) * 100;

            if (GetThisYearStats().highestDDproc < dd) {
                GetThisYearStats().highestDD = GetThisYearStats().highestequity - equity;
                GetThisYearStats().highestDDproc = dd;
                GetThisYearStats().dateOfHighestDD = date;
            }
        }
    }

    public void addBuy() {
        equity -= 1;
        GetThisYearStats().profit -= 1;
        GetThisYearStats().fees++;
    }

    public void LogStats() {

        for (TradeStats thisYearStat : yearlyStats.values()) {
            logger.log(BTLogLvl.BACKTEST, "For year " + thisYearStat.year);
            logger.log(BTLogLvl.BACKTEST, "Profit = " + TradeFormatter.toString(thisYearStat.profit) + " = " + TradeFormatter.toString(thisYearStat.profit / capital * 100) + '%');
        }

        logger.log(BTLogLvl.BACKTEST, "TestCompleted. Profit = " + TradeFormatter.toString(equity - capital)
                + ", succesful = " + TradeFormatter.toString((double) GetThisYearStats().profitSells / (double) GetThisYearStats().totalSells * 100) + "%");
        logger.log(BTLogLvl.BACKTEST, "Highest DD = " + TradeFormatter.toString(GetThisYearStats().highestDD) + "$, " + TradeFormatter.toString(GetThisYearStats().highestDDproc)
                + "%, date = " + GetThisYearStats().dateOfHighestDD.toString());
        logger.log(BTLogLvl.BACKTEST, "Paid on fees = " + TradeFormatter.toString(GetThisYearStats().fees) + "$");
    }
}
