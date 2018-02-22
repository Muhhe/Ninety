/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.getters;

import data.CloseData;
import data.OHLCData;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class DataGetterHistOHLCFile implements IDataGetterHistOHLC {
    
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private String path;

    public DataGetterHistOHLCFile(String path) {
        this.path = path;
    }

    @Override
    public String getName() {
        return "File loader";
    }
    
    @Override
    public OHLCData readAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol, boolean skipFirstIndex) {
        return readAdjCloseData(startDate, endDate, tickerSymbol, -1, skipFirstIndex);
    }
    
    @Override
    public OHLCData readAdjCloseData(LocalDate lastDate, String tickerSymbol, int daysToRead, boolean skipFirstIndex) {
        int daysBackNecessary = (int) ((daysToRead * (7.0/5.0)) + 20);
        return readAdjCloseData(lastDate.minusDays(daysBackNecessary), lastDate, tickerSymbol, daysToRead, skipFirstIndex);
    }

    @Override
    public OHLCData readAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol, int daysToRead, boolean skipFirstIndex) {
        try (BufferedReader br = new BufferedReader(new FileReader(path + tickerSymbol + ".csv"))) {

            ArrayList<Double> arrCloseVals = new ArrayList<>();
            ArrayList<Double> arrOpens = new ArrayList<>();
            ArrayList<Double> arrHighs = new ArrayList<>();
            ArrayList<Double> arrLows = new ArrayList<>();
            ArrayList<LocalDate> arrDates = new ArrayList<>();

            int totalCount = 0;
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");

                double adjClose = Double.parseDouble(tokens[5]);
                double open = Double.parseDouble(tokens[1]);
                double high = Double.parseDouble(tokens[2]);
                double low = Double.parseDouble(tokens[3]);
                LocalDate parsedDate = LocalDate.parse(tokens[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                
                if (parsedDate.compareTo(endDate) > 0) {
                    continue;
                }
                if (parsedDate.compareTo(startDate) < 0) {
                    break;
                }
                //if ((totalCount == 0) && (parsedDate.compareTo(startDate) > 1)) {
                //    logger.severe("Loading " + tickerSymbol + ": first date loaded: " + parsedDate + " expected " + startDate);
                //}

                if (totalCount == 0 && skipFirstIndex) {
                    arrDates.add(LocalDate.MIN);
                    arrCloseVals.add(0.0);
                    arrOpens.add(0.0);
                    arrHighs.add(0.0);
                    arrLows.add(0.0);
                    totalCount++;
                    continue;
                }
   
                arrDates.add(parsedDate);
                arrCloseVals.add(adjClose);
                arrOpens.add(open);
                arrHighs.add(high);
                arrLows.add(low);
                
                totalCount++;
                if (totalCount == daysToRead) {
                    break;
                }
            }

            OHLCData retData = new OHLCData(0);

            retData.adjCloses = arrCloseVals.stream().mapToDouble(Double::doubleValue).toArray();
            retData.opens = arrOpens.stream().mapToDouble(Double::doubleValue).toArray();
            retData.highs = arrHighs.stream().mapToDouble(Double::doubleValue).toArray();
            retData.lows = arrLows.stream().mapToDouble(Double::doubleValue).toArray();

            retData.dates = new LocalDate[arrDates.size()];
            retData.dates = arrDates.toArray(retData.dates);

            return retData;

        } catch (FileNotFoundException ex) {
            logger.severe("Cannot load close data: file not found - " + ex.getMessage());
        } catch (IOException ex) {
            logger.severe("Cannot load close data: error reading file - " + ex.getMessage());
        }
        
        return null;
    }
}
