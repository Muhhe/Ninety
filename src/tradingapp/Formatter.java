/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import java.text.DecimalFormat;

/**
 *
 * @author E0375631
 */
public class Formatter {
    private static Formatter instance = new Formatter();
    
    private final DecimalFormat decim = new DecimalFormat("0.00#");
    
    protected Formatter() {
        // Exists only to defeat instantiation.
    }

    public static Formatter getInstance() {
        if (instance == null) {
            instance = new Formatter();
        }
        return instance;
    }
    
    static public String toString(double d) {
        return instance.decim.format(d);
    }
    
}
