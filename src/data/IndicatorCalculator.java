/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class IndicatorCalculator {
    private final static Logger logger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );
    
    public double SMA(int count, double[] data) {
        
        if (data.length < count) {
            logger.log(Level.SEVERE, "SMA - not enough data: " + count + " vs " + data.length);
        }
        
        double total = 0;
        for (int i = 0; i < count; i++) {
            total += data[i];
        }
        
        return total / count;
    }
    
    //TODO predelat pocita to pravdepodobne obracene !!!!
    public double RSI(double[] closeArr) {

        if (closeArr.length < 14) {
            logger.log(Level.SEVERE, "RSI - not enough data: 14");
        }
        
        double[] ups = new double[14];
        double[] downs = new double[14];
        double[] avgUps = new double[14];
        double[] avgDowns = new double[14];

        int k = 1;
        for (int i = 12; i >= 0; i--) {
            if (closeArr[i] > closeArr[i + 1]) {
                ups[k] = closeArr[i] - closeArr[i + 1];
            } else {
                ups[k] = 0;
            }
            k++;
        }

        avgUps[1] = ups[1];
        for (int i = 2; i < 14; i++) {
            double a = (avgUps[i - 1] + ups[i]);
            avgUps[i] = (avgUps[i - 1] + ups[i]) / 2.0f;
        }

        k = 1;
        for (int i = 12; i >= 0; i--) {
            if (closeArr[i] < closeArr[i + 1]) {
                downs[k] = closeArr[i + 1] - closeArr[i];
            } else {
                downs[k] = 0;
            }
            k++;
        }

        avgDowns[1] = downs[1];
        for (int i = 2; i < 14; i++) {
            avgDowns[i] = (avgDowns[i - 1] + downs[i]) / 2.0f;
        }

        if (avgDowns[14 - 1] == 0) {
            return 100.0;
        }
        
        double ret = 100.f - (100.0f / (1.0f + (avgUps[14 - 1] / avgDowns[14 - 1])));

        return ret;
    }
}
