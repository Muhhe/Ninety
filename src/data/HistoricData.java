/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.util.Date;

/**
 *
 * @author Muhe
 */
public class HistoricData {
    
    HistoricData(int count) {
        date = new Date[count];
        adjClose = new double[count];
    }
    
    public Date[] date;
    public double[] adjClose;
    public String tickerSymbol;
    
    public double sma200;
    public double sma5;
    public double rsi2;
}
