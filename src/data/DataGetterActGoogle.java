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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class DataGetterActGoogle {
    
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private class DataGSON {
        private String t;
        private String l;
    }
    
    /*public class StockValue {
        public String tickerSymbol;
        public double value;
    }*/
    
    public static double readActualData(String tickerSymbol) throws IOException, NumberFormatException {
        StringBuilder urlBuilder = new StringBuilder();

        urlBuilder.append("http://finance.google.com/finance/info?client=ig&q=");

        urlBuilder.append(tickerSymbol);

        URL urlGoogle = new URL(urlBuilder.toString());
        BufferedReader in = new BufferedReader(
                new InputStreamReader(urlGoogle.openStream()));

        in.skip(3);

        Gson gson = new GsonBuilder().create();
        JsonReader reader = new JsonReader(in);
        reader.setLenient(true);
        DataGSON[] p = gson.fromJson(reader, DataGSON[].class);

        in.close();

        return Double.parseDouble(p[0].l.replaceAll(",", ""));
    }
    
    public static  Map<String, Double> readActualData(String[] tickerSymbols) throws IOException, NumberFormatException {
        StringBuilder urlBuilder = new StringBuilder();

        urlBuilder.append("http://finance.google.com/finance/info?client=ig&q=");

        for (String tickerSymbol : tickerSymbols) {
            urlBuilder.append(tickerSymbol);
            urlBuilder.append(",");
        }
        urlBuilder.deleteCharAt(urlBuilder.length() - 1); //remove last ,

        URL urlGoogle = new URL(urlBuilder.toString());
        BufferedReader in = new BufferedReader(new InputStreamReader(urlGoogle.openStream()));

        in.skip(3);

        Gson gson = new GsonBuilder().create();
        JsonReader reader = new JsonReader(in);
        reader.setLenient(true);
        DataGSON[] dataFromGSON = gson.fromJson(reader, DataGSON[].class);

        in.close();
        
        Map<String, Double> valuesMap;
        valuesMap = new HashMap(dataFromGSON.length);
        
        for (DataGSON entryGSON : dataFromGSON) {
            String tickerSymbol = entryGSON.t;
            String strValue = entryGSON.l.replaceAll(",", "");
            valuesMap.put(tickerSymbol, Double.parseDouble(strValue));
        }

        return valuesMap;
    }
}
