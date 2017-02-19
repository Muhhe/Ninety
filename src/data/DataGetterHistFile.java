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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.logging.Logger;
import tradingapp.FilePaths;

/**
 *
 * @author Muhe
 */
public class DataGetterHistFile implements IDataGetterHist {
    
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private String path;

    public DataGetterHistFile(String path) {
        this.path = path;
    }

    @Override
    public String getName() {
        return "File loader";
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
        try (BufferedReader br = new BufferedReader(new FileReader(path + tickerSymbol + ".csv"))) {

            ArrayList<Double> arrCloseVals = new ArrayList<>();
            ArrayList<LocalDate> arrDates = new ArrayList<>();

            int totalCount = 0;
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");

                double adjClose = Double.parseDouble(tokens[1]);
                LocalDate parsedDate = LocalDate.parse(tokens[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                if (totalCount == 0 && skipFirstIndex) {
                    arrDates.add(LocalDate.MIN);
                    arrCloseVals.add(0.0);
                    totalCount++;
                    continue;
                }
   
                arrDates.add(parsedDate);
                arrCloseVals.add(adjClose);
                
                totalCount++;
                if (totalCount == daysToRead) {
                    break;
                }
            }

            CloseData retData = new CloseData(0);

            retData.adjCloses = arrCloseVals.stream().mapToDouble(Double::doubleValue).toArray();

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
