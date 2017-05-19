/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategy90;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;
import tradingapp.FilePaths;

/**
 *
 * @author Muhe
 */
public class TickersToTrade {
    private final static Logger logger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );

    private static String[] tickers = new String[0];

    public static void LoadTickers() {
        try (BufferedReader br = new BufferedReader(new FileReader(FilePaths.tickerListPathFile))) {
            String line = br.readLine();
            tickers = line.split(",");
        } catch (FileNotFoundException ex) {
            logger.severe("Cannot load tickers to trade: file not found - " + ex.getMessage());
        } catch (IOException ex) {
            logger.severe("Cannot load tickers to trade: error reading file - " + ex.getMessage());
        }
        
        logger.fine("Loaded " + tickers.length + " ticker symbols.");
        if (tickers.length < 80) {
            logger.warning("Only " + tickers.length + " tickers loaded.");
        }
    }
    
    public static final String[] GetTickers() {
        if (tickers.length == 0) {
            LoadTickers();
        }
        
        return tickers;
    }
}
