/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 *
 * @author E0375631
 */
public class TradeFormatter {
    private static TradeFormatter instance = new TradeFormatter();
    
    private final DecimalFormat decim = new DecimalFormat("0.00#", DecimalFormatSymbols.getInstance(Locale.US));
    
    protected TradeFormatter() {
        // Exists only to defeat instantiation.
    }

    public static TradeFormatter getInstance() {
        if (instance == null) {
            instance = new TradeFormatter();
        }
        return instance;
    }
    
    static public String toString(double d) {
        return instance.decim.format(d);
    }
    
}
