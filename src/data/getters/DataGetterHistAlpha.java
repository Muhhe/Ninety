/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.getters;

import data.CloseData;
import java.io.BufferedReader;
import java.io.IOException;
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
public class DataGetterHistAlpha implements IDataGetterHist {
    
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    public DataGetterHistAlpha() {
    }

    @Override
    public String getName() {
        return "Alpha Vantage loader";
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
        StringBuilder urlBuilder = new StringBuilder();
        
        //https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol=AAPL&outputsize=full&apikey=7LIZ0GS31BCJYD6M&datatype=csv
        urlBuilder.append("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol=");
        urlBuilder.append(tickerSymbol);
        urlBuilder.append("&outputsize=full&apikey=7LIZ0GS31BCJYD6M&datatype=csv");
        
        URL url;
        try {
            url = new URL(urlBuilder.toString());

            BufferedReader br = new BufferedReader( new InputStreamReader(url.openStream()));

            br.readLine(); // skip first line

            ArrayList<Double> arrCloseVals = new ArrayList<>();
            ArrayList<LocalDate> arrDates = new ArrayList<>();

            int totalCount = 0;
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                
                if (tokens.length < 6) {
                    continue;
                }

                double adjClose = Double.parseDouble(tokens[5]);
                LocalDate parsedDate = LocalDate.parse(tokens[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                
                if (parsedDate.compareTo(endDate) > 0) {
                    continue;
                }
                if (parsedDate.compareTo(startDate) < 0) {
                    break;
                }

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

            CloseData retData = new CloseData(0);

            retData.adjCloses = arrCloseVals.stream().mapToDouble(Double::doubleValue).toArray();

            retData.dates = new LocalDate[arrDates.size()];
            retData.dates = arrDates.toArray(retData.dates);

            return retData;

        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to read data from " + getName() + " - '" + tickerSymbol + "' " + ex);
        }
        
        return null;
    }
}
