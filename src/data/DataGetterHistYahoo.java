/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class DataGetterHistYahoo {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    public static CloseData readRawAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol) {
        return readRawAdjCloseData(startDate, endDate, tickerSymbol, -1);
    }
    
    public static CloseData readRawAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol, int maxCount) {
        
        ArrayList<Double> arrCloseVals = new ArrayList<>();
        ArrayList<LocalDate> arrDates = new ArrayList<>();
        
        int startYear = startDate.getYear();
        int startMonth = startDate.getMonth().getValue();
        int startDay = startDate.getDayOfMonth();
        
        int endYear = endDate.getYear();
        int endMonth = endDate.getMonth().getValue();
        int endDay = endDate.getDayOfMonth();

        StringBuilder urlBuilder = new StringBuilder();

        urlBuilder.append("http://ichart.yahoo.com/table.csv?s=");
        urlBuilder.append(tickerSymbol);
        urlBuilder.append("&a=");
        urlBuilder.append(startMonth - 1);
        urlBuilder.append("&b=");
        urlBuilder.append(startDay);
        urlBuilder.append("&c=");
        urlBuilder.append(startYear);
        urlBuilder.append("&d=");
        urlBuilder.append(endMonth - 1);
        urlBuilder.append("&e=");
        urlBuilder.append(endDay);
        urlBuilder.append("&f=");
        urlBuilder.append(endYear);
        
        String line;
        String cvsSplitBy = ",";
        int totalCount = 0;

        URL urlYahoo;
        try {
            urlYahoo = new URL(urlBuilder.toString());

            BufferedReader br = new BufferedReader( new InputStreamReader(urlYahoo.openStream()));

            line = br.readLine(); // skip first line
                
            while ((line = br.readLine()) != null) {

                String[] dateLine = line.split(cvsSplitBy);

                LocalDate parsedDate = LocalDate.parse(dateLine[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                arrDates.add(parsedDate);
                
                double adjClose = Double.parseDouble(dateLine[6]);
                arrCloseVals.add(adjClose);
                
                totalCount++;
                if (totalCount == maxCount) {
                    break;
                }
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to read data from Yahoo - '" + tickerSymbol + "'");
            logger.severe(ex.toString());
            
            return null;
        }
        CloseData retData = new CloseData(0);
        
        retData.adjCloses = arrCloseVals.stream().mapToDouble(Double::doubleValue).toArray();
        
        retData.dates = new LocalDate[arrDates.size()];
        retData.dates = arrDates.toArray(retData.dates);
        
        return retData;
    }

    public static CloseData readData(LocalDate lastDate, int daysToRead, String tickerSymbol) {
        
        CloseData data = new CloseData(daysToRead);
        
        int year = lastDate.getYear();
        int month = lastDate.getMonth().getValue();
        int day = lastDate.getDayOfMonth();

        StringBuilder urlBuilder = new StringBuilder();

        urlBuilder.append("http://ichart.yahoo.com/table.csv?s=");
        urlBuilder.append(tickerSymbol);
        urlBuilder.append("&a=");
        urlBuilder.append(month - 1);
        urlBuilder.append("&b=");
        urlBuilder.append(day);
        urlBuilder.append("&c=");
        urlBuilder.append(year - 1);
        urlBuilder.append("&d=");
        urlBuilder.append(month - 1);
        urlBuilder.append("&e=");
        urlBuilder.append(day);
        urlBuilder.append("&f=");
        urlBuilder.append(year);

        String line;
        String cvsSplitBy = ",";

        URL urlYahoo;
        try {
            urlYahoo = new URL(urlBuilder.toString());

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(urlYahoo.openStream()));

            line = br.readLine(); // skip first line
                
            int totalCount = 1;
            
            while ((line = br.readLine()) != null) {

                String[] dateLine = line.split(cvsSplitBy);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                LocalDate parsedDate = LocalDate.parse(dateLine[0], formatter);
                
                double adjClose = Double.parseDouble(dateLine[6]);

                data.adjCloses[totalCount] = adjClose;
                data.dates[totalCount] = parsedDate;
                totalCount++;
                if (totalCount == daysToRead) {
                    break;
                }
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to read data from Yahoo.");
            logger.log(Level.SEVERE, null, ex);
            
            return null;
        }
        
        return data;
    }
}
