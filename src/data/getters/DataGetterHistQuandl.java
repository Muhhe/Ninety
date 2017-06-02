/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.getters;

import data.CloseData;
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
public class DataGetterHistQuandl implements IDataGetterHist {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    @Override
    public String getName() {
        return "Quandl";
    }
    
    public CloseData readAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol) {
        return readAdjCloseData(startDate, endDate, tickerSymbol, -1, false);
    }
    
    @Override
    public CloseData readAdjCloseData(LocalDate lastDate, String tickerSymbol, int daysToRead, boolean skipFirstIndex) {
        int daysBackNecessary = (int) ((daysToRead * (7.0/5.0)) + 20);
        return readAdjCloseData(lastDate.minusDays(daysBackNecessary), lastDate, tickerSymbol, daysToRead, skipFirstIndex);
    }
    
    public CloseData readAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol, int daysToRead, boolean skipFirstIndex) {
        
        ArrayList<Double> arrCloseVals = new ArrayList<>();
        ArrayList<LocalDate> arrDates = new ArrayList<>();
        
        int startYear = startDate.getYear();
        int startMonth = startDate.getMonth().getValue();
        int startDay = startDate.getDayOfMonth();
        
        int endYear = endDate.getYear();
        int endMonth = endDate.getMonth().getValue();
        int endDay = endDate.getDayOfMonth();

        StringBuilder urlBuilder = new StringBuilder();
        
        //https://www.quandl.com/api/v3/datasets/WIKI/AAPL.csv?start_date=2016-05-01&end_date=2017-07-01&order=des
        
        urlBuilder.append("https://www.quandl.com/api/v3/datasets/WIKI/");
        urlBuilder.append(tickerSymbol);
        urlBuilder.append(".csv?start_date=");
        urlBuilder.append(startYear);
        urlBuilder.append("-");
        urlBuilder.append(startMonth);
        urlBuilder.append("-");
        urlBuilder.append(startDay);
        urlBuilder.append("&end_date=");
        urlBuilder.append(endYear);
        urlBuilder.append("-");
        urlBuilder.append(endMonth);
        urlBuilder.append("-");
        urlBuilder.append(endDay);
        urlBuilder.append("&order=des");
        
        String line;
        String cvsSplitBy = ",";
        int totalCount = 0;

        URL urlQuandl;
        try {
            urlQuandl = new URL(urlBuilder.toString());

            BufferedReader br = new BufferedReader( new InputStreamReader(urlQuandl.openStream()));

            br.readLine(); // skip first line
                
            while ((line = br.readLine()) != null) {

                String[] dateLine = line.split(cvsSplitBy);

                LocalDate parsedDate = LocalDate.parse(dateLine[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                double adjClose = Double.parseDouble(dateLine[11]);
                
                if (totalCount == 0 && skipFirstIndex) {
                    arrDates.add(LocalDate.MIN);
                    arrCloseVals.add(0.0);
                    totalCount++;
                }
                
                arrDates.add(parsedDate);
                arrCloseVals.add(adjClose);
                
                totalCount++;
                if (totalCount == daysToRead) {
                    break;
                }
            }

        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to read data from Quandl - '" + tickerSymbol + "'" + ex);
            return null;
        }
        CloseData retData = new CloseData(0);
        
        retData.adjCloses = arrCloseVals.stream().mapToDouble(Double::doubleValue).toArray();
        
        retData.dates = new LocalDate[arrDates.size()];
        retData.dates = arrDates.toArray(retData.dates);
        
        return retData;
    }

    /*public static CloseData readData(LocalDate lastDate, int daysToRead, String tickerSymbol) {
        
        CloseData data = new CloseData(daysToRead);
        
        int year = lastDate.getYear();
        int month = lastDate.getMonth().getValue();
        int day = lastDate.getDayOfMonth();

        StringBuilder urlBuilder = new StringBuilder();
        
        //https://www.quandl.com/api/v3/datasets/WIKI/AAPL.csv?start_date=2016-05-01&end_date=2017-07-01&order=des
        
        urlBuilder.append("https://www.quandl.com/api/v3/datasets/WIKI/");
        urlBuilder.append(tickerSymbol);
        urlBuilder.append(".csv?start_date=");
        urlBuilder.append(year-1);
        urlBuilder.append("-");
        urlBuilder.append(month);
        urlBuilder.append("-");
        urlBuilder.append(day);
        urlBuilder.append("&end_date=");
        urlBuilder.append(year);
        urlBuilder.append("-");
        urlBuilder.append(month);
        urlBuilder.append("-");
        urlBuilder.append(day);
        urlBuilder.append("&order=des");

        String line;
        String cvsSplitBy = ",";

        URL urlQuandl;
        try {
            urlQuandl = new URL(urlBuilder.toString());

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(urlQuandl.openStream()));

            br.readLine(); // skip first line
                
            int totalCount = 1;
            
            while ((line = br.readLine()) != null) {

                String[] dateLine = line.split(cvsSplitBy);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                LocalDate parsedDate = LocalDate.parse(dateLine[0], formatter);
                
                double adjClose = Double.parseDouble(dateLine[11]);

                data.adjCloses[totalCount] = adjClose;
                data.dates[totalCount] = parsedDate;
                totalCount++;
                if (totalCount == daysToRead) {
                    break;
                }
            }

        } catch (IOException | NumberFormatException ex) {
            logger.log(Level.SEVERE, "Failed to read data from Quandl - '" + tickerSymbol + "'", ex);
            return null;
        }
        
        return data;
    }*/
}
