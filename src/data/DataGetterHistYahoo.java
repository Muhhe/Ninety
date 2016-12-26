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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class DataGetterHistYahoo {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static CloseData readData(LocalDate lastDate, int daysToRead, String tickerSymbol) {
        
        CloseData data = new CloseData(daysToRead);
        
        int year = lastDate.getYear();
        int month = lastDate.getMonth().getValue();
        int day = lastDate.getDayOfMonth();

        StringBuilder urlBuilder = new StringBuilder();

        urlBuilder.append("http://ichart.yahoo.com/table.csv?s=");
        urlBuilder.append(tickerSymbol);
        urlBuilder.append("&a=");
        urlBuilder.append(month);
        urlBuilder.append("&b=");
        urlBuilder.append(day);
        urlBuilder.append("&c=");
        urlBuilder.append(year - 1);
        urlBuilder.append("&d=");
        urlBuilder.append(month);
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
