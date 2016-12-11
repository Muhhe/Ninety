/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class YahooDataGetter {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public HistoricData data;

    public void readData(Date lastDate, int daysToRead, String tickerSymbol, double actValue) {
        
        data = new HistoricData(daysToRead);
        
        data.tickerSymbol = tickerSymbol;
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(lastDate);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        //StringBuilder textBuilder = new StringBuilder();
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

        //float totalClose = 0;
        //int totalCount = 0;
        //double[] closeArr = new double[daysToRead];

        URL urlYahoo;
        try {
            urlYahoo = new URL(urlBuilder.toString());

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(urlYahoo.openStream()));

            line = br.readLine(); // skip first line
            
            data.adjClose[0] = actValue;
            data.date[0] = lastDate;
                
            int totalCount = 1;
            
            while ((line = br.readLine()) != null) {

                String[] dateLine = line.split(cvsSplitBy);

                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                Date date = new Date();
                try {
                    date = formatter.parse(dateLine[0]);
                } catch (ParseException ex) {
                    logger.log(Level.SEVERE, "Failed to parse date from Yahoo: {0}", dateLine[0]);
                }
                
                double adjClose = Double.parseDouble(dateLine[6]);
                

                data.adjClose[totalCount] = adjClose;
                data.date[totalCount++] = date;
                if (totalCount == daysToRead) {
                    break;
                }
            }

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to read data from Yahoo.");
            logger.log(Level.SEVERE, null, ex);
        }

    }
}
