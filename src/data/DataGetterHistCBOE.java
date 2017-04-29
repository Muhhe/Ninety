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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class DataGetterHistCBOE implements IDataGetterHist {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @Override
    public String getName() {
        return "CBOE";
    }
    
    @Override
    public CloseData readAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol) {
        return readAdjCloseData(startDate, endDate, tickerSymbol, -1, false);
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
        
        /*int startYear = startDate.getYear();
        int startMonth = startDate.getMonth().getValue();
        int startDay = startDate.getDayOfMonth();
        
        int endYear = endDate.getYear();
        int endMonth = endDate.getMonth().getValue();
        int endDay = endDate.getDayOfMonth();*/

        StringBuilder urlBuilder = new StringBuilder();

        urlBuilder.append("https://www.cboe.com/publish/scheduledtask/mktdata/datahouse/");
        urlBuilder.append(tickerSymbol);
        urlBuilder.append("dailyprices.csv");
        
        String line;
        String cvsSplitBy = ",";
        int totalCount = 0;

        URL url;
        try {
            url = new URL(urlBuilder.toString());

            BufferedReader br = new BufferedReader( new InputStreamReader(url.openStream()));

            br.readLine(); // skip first line
            br.readLine(); // skip second line
            br.readLine(); // skip third line
                
            while ((line = br.readLine()) != null) {

                String[] dateLine = line.split(cvsSplitBy);

                double adjClose = Double.parseDouble(dateLine[4]);
                LocalDate parsedDate = LocalDate.parse(dateLine[0], DateTimeFormatter.ofPattern("M/d/yyyy"));
                
                if (parsedDate.compareTo(startDate) < 0) {
                    continue;
                }

                if (totalCount == 0 && skipFirstIndex) {
                    arrDates.add(LocalDate.MIN);
                    arrCloseVals.add(0.0);
                    totalCount++;
                }
   
                arrDates.add(parsedDate);
                arrCloseVals.add(adjClose);
                
                totalCount++;
                //if (totalCount == daysToRead) {
                //    break;
                //}
            }
            
            if (daysToRead != -1 && daysToRead > totalCount) {
                logger.warning("Loading " + tickerSymbol + " from " + getName() + " failed. Read only " + totalCount + " out of " + daysToRead);
                return null;
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to read data from CBOE - '" + tickerSymbol + "'", ex);
            return null;
        }
        CloseData retData = new CloseData(0);
        
        List<Double> subListClose;
        List<LocalDate> subListDates;
        if (daysToRead > 0) {
            subListClose = arrCloseVals.subList(arrCloseVals.size() - daysToRead, arrCloseVals.size());
            subListDates = arrDates.subList(arrDates.size() - daysToRead, arrDates.size());
        } else {
            subListClose = arrCloseVals;
            subListDates = arrDates;
        }
            
        
        retData.adjCloses = subListClose.stream().mapToDouble(Double::doubleValue).toArray();
        
        retData.dates = new LocalDate[arrDates.size()];
        retData.dates = subListDates.toArray(retData.dates);
        
        return retData;
    }
}
