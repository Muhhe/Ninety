/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class StockPurchase {
    
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    public double priceForOne = 0;
    public int position = 0;
    public int portions = 0;
    public Date date = new Date();
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Stock purchase - priceForOne;").append(priceForOne);
        str.append("; position;").append(position);
        str.append("; portions;").append(portions);
        str.append("; date;").append(date).append("\r\n");
        
        return str.toString();
    }

    void LoadFromStrings(String[] strs) {
        priceForOne = Double.parseDouble(strs[1]);
        position = Integer.parseInt(strs[3]);
        portions = Integer.parseInt(strs[5]);
        
        /*SimpleDateFormat sdf = new SimpleDateFormat();
        try {
            date = sdf.parse(strs[7]);
        } catch (ParseException ex) {
            logger.severe("Failed to load StockPurchase date from string: " + ex);
        }*/
    }
}
