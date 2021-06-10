/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import tradingapp.FilePaths;

/**
 *
 * @author Muhe
 */
public class Utils {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static CloseData GetFirstDaysInWeek(CloseData data) {
        List<LocalDate> newDates = new ArrayList<>();
        List<Double> newCloses = new ArrayList<>();

        newDates.add(data.dates[data.dates.length - 1]);
        newCloses.add(data.adjCloses[data.dates.length - 1]);

        for (int i = data.dates.length - 2; i >= 0; i--) {
            if (data.dates[i].getDayOfWeek().getValue() < data.dates[i + 1].getDayOfWeek().getValue()) {
                newDates.add(data.dates[i]);
                newCloses.add(data.adjCloses[i]);
            }
        }
        CloseData newData = new CloseData(newDates.size());

        for (int i = 0; i < newDates.size(); i++) {
            newData.adjCloses[newDates.size() - i - 1] = newCloses.get(i);
            newData.dates[newDates.size() - i - 1] = newDates.get(i);
        }
        return newData;
    }

    public static CloseData GetLastDaysInWeek(CloseData data, int offset) {
        List<LocalDate> newDates = new ArrayList<>();
        List<Double> newCloses = new ArrayList<>();

        //newDates.add(data.dates[data.dates.length-1]);
        //newCloses.add(data.adjCloses[data.dates.length-1]);
        for (int i = data.dates.length - 2; i >= offset; i--) {
            if (data.dates[i].getDayOfWeek().getValue() < data.dates[i + 1].getDayOfWeek().getValue()) {
                newDates.add(data.dates[i + 1]);
                newCloses.add(data.adjCloses[i + 1]);
            }
        }
        newDates.add(data.dates[offset]);
        newCloses.add(data.adjCloses[offset]);

        CloseData newData = new CloseData(newDates.size());

        for (int i = 0; i < newDates.size(); i++) {
            newData.adjCloses[newDates.size() - i - 1] = newCloses.get(i);
            newData.dates[newDates.size() - i - 1] = newDates.get(i);
        }
        return newData;
    }

    public static OHLCData GetLastDaysInWeek(OHLCData data, int offset) {
        List<LocalDate> newDates = new ArrayList<>();
        List<Double> newOpens = new ArrayList<>();
        List<Double> newHighs = new ArrayList<>();
        List<Double> newLows = new ArrayList<>();
        List<Double> newCloses = new ArrayList<>();

        for (int i = data.dates.length - 2; i >= offset; i--) {
            if (data.dates[i].getDayOfWeek().getValue() < data.dates[i + 1].getDayOfWeek().getValue()) {
                newDates.add(data.dates[i + 1]);
                newOpens.add(data.opens[i + 1]);
                newHighs.add(data.highs[i + 1]);
                newLows.add(data.lows[i + 1]);
                newCloses.add(data.adjCloses[i + 1]);
            }
        }
        newDates.add(data.dates[offset]);
        newOpens.add(data.opens[offset]);
        newHighs.add(data.highs[offset]);
        newLows.add(data.lows[offset]);
        newCloses.add(data.adjCloses[offset]);

        OHLCData newData = new OHLCData(newDates.size());

        for (int i = 0; i < newDates.size(); i++) {
            newData.dates[newDates.size() - i - 1] = newDates.get(i);
            newData.opens[newDates.size() - i - 1] = newOpens.get(i);
            newData.highs[newDates.size() - i - 1] = newHighs.get(i);
            newData.lows[newDates.size() - i - 1] = newLows.get(i);
            newData.adjCloses[newDates.size() - i - 1] = newCloses.get(i);
        }
        return newData;
    }

    public static String[] LoadTickers() {
        String[] tickers = null;
        try (BufferedReader br = new BufferedReader(new FileReader(FilePaths.tickerListPathFile))) {
            String line = br.readLine();
            tickers = line.split(",");
            logger.fine("Loaded " + tickers.length + " ticker symbols.");

            if (tickers.length < 80) {
                logger.warning("Only " + tickers.length + " tickers loaded.");
            }
        } catch (FileNotFoundException ex) {
            logger.severe("Cannot load tickers to trade: file not found - " + ex.getMessage());
        } catch (IOException ex) {
            logger.severe("Cannot load tickers to trade: error reading file - " + ex.getMessage());
        }

        if (tickers == null) {
            logger.severe("No tickers loaded!");
        }

        return tickers;
    }

    public static double[] reverseDouble(double a[], int maxLength) {
        if (maxLength <= 0 || maxLength > a.length) {
            maxLength = a.length;
        }
        
        double[] b = new double[maxLength];
        int j = a.length;
        for (int i = 0; i < maxLength; i++) {
            b[i] = a[j - 1 - i];
        }
        return b;
    }

    public static LocalDate[] reverseLocalDate(LocalDate a[], int maxLength) {
        if (maxLength <= 0 || maxLength > a.length) {
            maxLength = a.length;
        }
        
        LocalDate[] b = new LocalDate[maxLength];
        int j = a.length;
        for (int i = 0; i < maxLength; i++) {
            b[i] = a[j - 1 - i];
        }
        return b;
    }
}
