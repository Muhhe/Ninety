/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import strategies.StatusDataForNinety;
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

        public double highestequity = 0;
        public double highestDDproc = 0;
        public double highestDD = 0;
        public LocalDate dateOfHighestDD = LocalDate.MIN;
        public double fees = 0;

        public int year;
        public int days = 0; //TODO
        public double startCapital = 0;

        public TradeYearlyStats(int year) {
            this.year = year;
        }
    }
    
    public boolean reinvest;

    public double capital = 0;
    public double equity = 0;

    List<EquityInTime> equityList = new ArrayList<>();
    private LocalDate currentDate = LocalDate.MIN;
    private LocalDate startDate = LocalDate.MIN;
    private LocalDate endDate = LocalDate.MIN;

    //public TradeStats globalStats = new TradeStats();
    public List<TradeYearlyStats> yearlyStats = new ArrayList<>();

    public BTStatistics(double capital, boolean reinvest) {
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
        
        if (currentDate.isEqual(LocalDate.MIN)) {
            startDate = date;
        }
        endDate = date;

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

    public void AddSell(double profit, int position, LocalDate date) {
        double fee = StatusDataForNinety.GetOrderFee(position);
        
        profit -= fee;
        equity += profit;
        
        GetThisYearStats().fees += fee;
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

    public void addBuy(int position) {
        double fee = StatusDataForNinety.GetOrderFee(position);
        equity -= fee;
        GetThisYearStats().profit -= fee;
        GetThisYearStats().fees += fee;
    }

    public void LogStats() {
        
        logger.log(BTLogLvl.BACKTEST, "TestCompleted.");

        double totalProfit = 0;
        long totalDays = 0;
        double fees = 0;
        double highestDDproc = 0;
        LocalDate dateOfHighestDD = LocalDate.MIN;
        int totalSells = 0;
        int profitSells = 0;
        
        for (TradeYearlyStats thisYearStat : yearlyStats) {
            
            double profit = thisYearStat.profit;
            double profitPercent = thisYearStat.profit / capital * 100;
            
            logger.log(BTLogLvl.BACKTEST, thisYearStat.year + " days: " + thisYearStat.days + 
                    " | profit = " + TradeFormatter.toString(profit) + "$ = " + TradeFormatter.toString(profitPercent) + 
                    "% | max DD = " + TradeFormatter.toString(thisYearStat.highestDDproc) + "% (" + thisYearStat.dateOfHighestDD +
                    ") | fees = " + TradeFormatter.toString(thisYearStat.fees) + 
                    "$ | closed trades = " + thisYearStat.totalSells + 
                    " | successful = " + TradeFormatter.toString((double)thisYearStat.profitSells / (double)thisYearStat.totalSells * 100.0) + 
                    "%");
            
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
        
        double profitPercent = totalProfit / capital * 100;
        
        logger.log(BTLogLvl.BACKTEST, "Total stats" + 
                    " | profit = " + TradeFormatter.toString(totalProfit) + "$ = " + TradeFormatter.toString(profitPercent) + 
                    "% | max DD = " + TradeFormatter.toString(highestDDproc) + "% (" + dateOfHighestDD +
                    ") | fees = " + TradeFormatter.toString(fees) + 
                    "$ | closed trades = " + totalSells + 
                    " | successful = " + TradeFormatter.toString((double)profitSells / (double)totalSells * 100.0) + 
                    "%");
        
        double avgProfitPercent = profitPercent / ((double)totalDays / 252.0);
        logger.log(BTLogLvl.BACKTEST, "Average yearly profit: " + TradeFormatter.toString(avgProfitPercent) + "%");

        /*logger.log(BTLogLvl.BACKTEST, "TestCompleted. Profit = " + TradeFormatter.toString(equity - capital)
                + ", succesful = " + TradeFormatter.toString((double) GetThisYearStats().profitSells / (double) GetThisYearStats().totalSells * 100) + "%");
        logger.log(BTLogLvl.BACKTEST, "Highest DD = " + TradeFormatter.toString(GetThisYearStats().highestDD) + "$, " + TradeFormatter.toString(GetThisYearStats().highestDDproc)
                + "%, date = " + GetThisYearStats().dateOfHighestDD.toString());
        logger.log(BTLogLvl.BACKTEST, "Paid on fees = " + TradeFormatter.toString(GetThisYearStats().fees) + "$");*/
    }
}
