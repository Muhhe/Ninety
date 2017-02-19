/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import communication.IBroker;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class DataGetterActFile implements IDataGetterAct {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private String path;

    public DataGetterActFile(String path) {
        this.path = path;
    }
    
    @Override
    public String getName() {
        return "File Loader";
    }
    
    @Override
    public double readActualData(String tickerSymbol) {
        
        try (BufferedReader br = new BufferedReader(new FileReader(path + tickerSymbol + ".csv"))) {

            String line = br.readLine();
            
            String[] tokens = line.split(",");

            return Double.parseDouble(tokens[1]);

        } catch (FileNotFoundException ex) {
            logger.severe("Cannot load close data: file not found - " + ex.getMessage());
        } catch (IOException ex) {
            logger.severe("Cannot load close data: error reading file - " + ex.getMessage());
        }
        
        return 0;
    }
    
    @Override
    public  Map<String, Double> readActualData(String[] tickerSymbols) {
                
        Map<String, Double> map = new HashMap<>(tickerSymbols.length);
        for (String tickerSymbol : tickerSymbols) {
            double actData = readActualData(tickerSymbol);
            map.put(tickerSymbol, actData);
        }
        
        return map;
    }
}
