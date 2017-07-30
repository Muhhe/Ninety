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

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static double sqrtTradeDays = Math.sqrt(252);

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

        if (data.length < 2 * count + offset - 1) {
            logger.severe("EMA - not enough data: " + (2 * count + offset - 1) + " vs " + data.length);
            return 0;
        }

        double k = 2.0 / (double) (count + 1);
        double lastEMA = SMA(count, data, offset + count - 1);

        for (int i = count + offset - 2; i >= offset; i--) {
            lastEMA = (data[i] * k) + lastEMA * (1 - k);
        }

        return lastEMA;
    }

    public static double RSI(double[] values) {
        return RSI(values, 14, 0);
    }
    
    public static double RSI(double[] values, int num, int offset) {
        if (values == null) {
            logger.severe("RSI - null values");
            return Double.MAX_VALUE;
        }
        if (values.length < num + offset) {
            logger.severe("RSI - not enough data: " + values.length + "/14");
            return Double.MAX_VALUE;
        }

        double[] ups = new double[num];
        double[] downs = new double[num];
        double[] avgUps = new double[num];
        double[] avgDowns = new double[num];

        int k = 1;
        for (int i = num-2; i >= 0; i--) {
            if (values[i + offset] > values[i + 1 + offset]) {
                ups[k] = values[i + offset] - values[i + 1 + offset];
            } else {
                ups[k] = 0;
            }
            k++;
        }

        avgUps[1] = ups[1];
        for (int i = 2; i < num; i++) {
            avgUps[i] = (avgUps[i - 1] + ups[i]) / 2.0f;
        }

        k = 1;
        for (int i = num-2; i >= 0; i--) {
            if (values[i + offset] < values[i + 1 + offset]) {
                downs[k] = values[i + 1 + offset] - values[i + offset];
            } else {
                downs[k] = 0;
            }
            k++;
        }

        avgDowns[1] = downs[1];
        for (int i = 2; i < num; i++) {
            avgDowns[i] = (avgDowns[i - 1] + downs[i]) / 2.0f;
        }

        if (avgDowns[num - 1] == 0) {
            return 100.0;
        }

        double ret = 100.f - (100.0f / (1.0f + (avgUps[num - 1] / avgDowns[num - 1])));

        return ret;
    }

    public static double StandardDeviation(int count, double[] data) {
        return StandardDeviation(count, data, 0);
    }

    public static double StandardDeviation(int count, double[] data, int offset) {

        if (data.length < count + offset) {
            logger.severe("SMA - not enough data: " + (count + offset) + " vs " + data.length);
            return 0;
        }

        double sma = SMA(count, data, offset);

        double sqDistSum = 0;
        for (int i = 0; i < count; i++) {
            double sqDists = data[i + offset] - sma;

            sqDistSum += sqDists * sqDists;
        }

        sqDistSum /= count;

        return Math.sqrt(sqDistSum);
    }
    
    public static double Volatility(int count, double[] data) {
        return Volatility(count, data, 0);
    }

    public static double Volatility(int count, double[] data, int offset) {

        if (data.length < count + offset + 1) {
            logger.severe("Volatility - not enough data: " + (count + offset + 1) + " vs " + data.length);
            return 0;
        }
        double[] returns = new double[count];

        for (int i = 0; i < count; i++) {
            returns[i] = Math.log(Math.abs(data[i + offset] / data[i + offset + 1]));
            //returns[i] = (data[i + offset] / data[i + offset + 1]) - 1;
        }

        double std = StandardDeviation(count, returns);

        return std * sqrtTradeDays;
    }
}
