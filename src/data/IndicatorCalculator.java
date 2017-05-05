/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class IndicatorCalculator {
    private final static Logger logger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );
    
    public static double SMA(int count, double[] data) {
        return SMA(count, data, 0);
    }
    
    public static double SMA(int count, double[] data, int offset) {
        
        if (data.length < count + offset) {
            logger.severe("SMA - not enough data: " + (count + offset) + " vs " + data.length);
            return 0;
        }
        
        double total = 0;
        for (int i = 0; i < count; i++) {
            total += data[i + offset];
        }
        
        return total / count;
    }
    
    public static double EMA(int count, double[] data) {
        return EMA(count, data, 0);
    }
    
    public static double EMA(int count, double[] data, int offset) {
        
        if (data.length < 2*count + offset - 1) {
            logger.severe("EMA - not enough data: " + (2*count + offset - 1) + " vs " + data.length);
            return 0;
        }
        
        double k = 2.0 /(double)(count + 1);
        double lastEMA = SMA(count, data, offset + count - 1);
        
        for (int i = count + offset - 2; i >= offset; i--) {
            lastEMA = (data[i] * k) + lastEMA * (1 - k);
        }
        
        return lastEMA;
    }
    
    public static double RSI(double[] values) {
        if (values == null) {
            logger.severe("RSI - null values");
            return Double.MAX_VALUE;
        }
        if (values.length < 14) {
            logger.severe("RSI - not enough data: " + values.length + "/14");
            return Double.MAX_VALUE;
        }
        
        double[] ups = new double[14];
        double[] downs = new double[14];
        double[] avgUps = new double[14];
        double[] avgDowns = new double[14];

        int k = 1;
        for (int i = 12; i >= 0; i--) {
            if (values[i] > values[i + 1]) {
                ups[k] = values[i] - values[i + 1];
            } else {
                ups[k] = 0;
            }
            k++;
        }

        avgUps[1] = ups[1];
        for (int i = 2; i < 14; i++) {
            avgUps[i] = (avgUps[i - 1] + ups[i]) / 2.0f;
        }

        k = 1;
        for (int i = 12; i >= 0; i--) {
            if (values[i] < values[i + 1]) {
                downs[k] = values[i + 1] - values[i];
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
