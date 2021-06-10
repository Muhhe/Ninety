/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import java.time.LocalDate;

/**
 *
 * @author Muhe
 */
public class BTSettings {
    public LocalDate startDate;
    public LocalDate endDate;
    
    public double capital;
    public double leverage;
    
    public boolean reinvest;
    
    public int loglvl;

    public BTSettings(LocalDate startDate, LocalDate endDate, double capital, double leverage, boolean reinvest, int loglvl) {
        this.reinvest = reinvest;
        this.leverage = leverage;
        this.capital = capital;
        this.endDate = endDate;
        this.startDate = startDate;
        this.loglvl = loglvl;
    }

    @Override
    public String toString() {
        return "Start: " + startDate + " | End: " + endDate + " | Capital: " + capital + " | leverage: " + leverage + " | reinvest: " + reinvest + " | loglvl: " + loglvl;
    }
    
}
