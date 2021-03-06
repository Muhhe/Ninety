/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.getters;

import data.CloseData;
import data.OHLCData;
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
public class DataGetterHistYahoo implements IDataGetterHist {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @Override
    public String getName() {
        return "Yahoo";
    }
    
    @Override
    public CloseData readAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol, boolean skipFirstIndex) {
        return readAdjCloseData(startDate, endDate, tickerSymbol, -1, skipFirstIndex);
    }
    
    @Override
    public CloseData readAdjCloseData(LocalDate lastDate, String tickerSymbol, int daysToRead, boolean skipFirstIndex) {
        int daysBackNecessary = (int) ((daysToRead * (7.0/5.0)) + 20);
        return readAdjCloseData(lastDate.minusDays(daysBackNecessary), lastDate, tickerSymbol, daysToRead, skipFirstIndex);
    }
    
    @Override
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

        urlBuilder.append("https://ichart.yahoo.com/table.csv?s=");
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

            br.readLine(); // skip first line
                
            while ((line = br.readLine()) != null) {

                String[] dateLine = line.split(cvsSplitBy);

                double adjClose = Double.parseDouble(dateLine[6]);
                LocalDate parsedDate = LocalDate.parse(dateLine[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));

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
            
            if (daysToRead != -1 && daysToRead != totalCount) {
                logger.warning("Loading " + tickerSymbol + " from " + getName() + " failed. Read only " + totalCount + " out of " + daysToRead);
                return null;
            }

        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to read data from Yahoo - '" + tickerSymbol + "'" + ex);
            return null;
        }
        CloseData retData = new CloseData(0);
        
        retData.adjCloses = arrCloseVals.stream().mapToDouble(Double::doubleValue).toArray();
        
        retData.dates = new LocalDate[arrDates.size()];
        retData.dates = arrDates.toArray(retData.dates);
        
        return retData;
    }
    
    @Override
    public OHLCData readAdjOHLCData(LocalDate startDate, LocalDate endDate, String tickerSymbol, boolean skipFirstIndex) {
        return readAdjOHLCData(startDate, endDate, tickerSymbol, -1, skipFirstIndex);
    }

    @Override
    public OHLCData readAdjOHLCData(LocalDate lastDate, String tickerSymbol, int daysToRead, boolean skipFirstIndex) {
        int daysBackNecessary = (int) ((daysToRead * (7.0 / 5.0)) + 20);
        return readAdjOHLCData(lastDate.minusDays(daysBackNecessary), lastDate, tickerSymbol, daysToRead, skipFirstIndex);
    }
    
    @Override
    public OHLCData readAdjOHLCData(LocalDate startDate, LocalDate endDate, String tickerSymbol, int daysToRead, boolean skipFirstIndex){
        return null;
    }
}
