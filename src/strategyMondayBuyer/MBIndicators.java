/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyMondayBuyer;

import tradingapp.TradeFormatter;

/**
 *
 * @author Muhe
 */
public class MBIndicators {
    public boolean sma10x2;
    public double rsi2 = 0;
    public double vol = 0;
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("sma10x2: ").append(sma10x2);
        sb.append(", rsi2: ").append(TradeFormatter.toString(rsi2));
        sb.append(", vol: ").append(TradeFormatter.toString(vol));

        return sb.toString();
    }
}
