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
import communication.IBroker;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class DataGetterActGoogle implements IDataGetterAct {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private class DataGSON {

        private String t;
        private String l;
    }
    
    @Override
    public String getName() {
        return "Google";
    }

    @Override
    public double readActualData(String tickerSymbol) {
        try {
            StringBuilder urlBuilder = new StringBuilder();

            urlBuilder.append("http://finance.google.com/finance/info?client=ig&q=");

            urlBuilder.append(tickerSymbol);

            URL urlGoogle = new URL(urlBuilder.toString());
            DataGSON[] entryArray;
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(urlGoogle.openStream()))) {
                in.skip(3);
                Gson gson = new GsonBuilder().create();
                JsonReader reader = new JsonReader(in);
                reader.setLenient(true);
                entryArray = gson.fromJson(reader, DataGSON[].class);
            }

            return Double.parseDouble(entryArray[0].l.replaceAll(",", ""));

        } catch (IOException | NumberFormatException ex) {
            logger.warning("Failed to load actual data from google at once. Exception: " + ex.getMessage());
            return 0;
        }
    }

    @Override
    public Map<String, Double> readActualData(String[] tickerSymbols) {
        try {
            StringBuilder urlBuilder = new StringBuilder();

            urlBuilder.append("http://finance.google.com/finance/info?client=ig&q=");

            for (String tickerSymbol : tickerSymbols) {
                urlBuilder.append(tickerSymbol);
                urlBuilder.append(",");
            }
            urlBuilder.deleteCharAt(urlBuilder.length() - 1); //remove last ,

            URL urlGoogle = new URL(urlBuilder.toString());
            DataGSON[] dataFromGSON;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(urlGoogle.openStream()))) {
                in.skip(3);
                Gson gson = new GsonBuilder().create();
                JsonReader reader = new JsonReader(in);
                reader.setLenient(true);
                dataFromGSON = gson.fromJson(reader, DataGSON[].class);
            }

            Map<String, Double> valuesMap = new HashMap<>(dataFromGSON.length);

            for (DataGSON entryGSON : dataFromGSON) {
                String tickerSymbol = entryGSON.t;
                String strValue = entryGSON.l.replaceAll(",", "");
                valuesMap.put(tickerSymbol, Double.parseDouble(strValue));
            }

            return valuesMap;

        } catch (IOException | NumberFormatException ex) {
            logger.warning("Failed to load actual data from google at once. Exception: " + ex.getMessage());
            return null;
        }
    }
}
