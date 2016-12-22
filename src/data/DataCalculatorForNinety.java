/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class DataCalculatorForNinety {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    //public HistoricData[] data;
    public Map<String, HistoricData> stockMap = new HashMap<String, HistoricData>(100);

    String[] getSP100() {
        String[] tickers = {
            "AAPL", "ABBV", "ABT", "ACN", "AGN", "AIG", "ALL", "AMGN", "AMZN",
            "AXP", "BA", "BAC", "BIIB", "BK", "BLK", "BMY", "C", "CAT", "CELG", "CL", "CMCSA",
            "COF", "COP", "COST", "CSCO", "CVS", "CVX", "DD", "DHR", "DIS", "DOW", "DUK",
            "EMR", "EXC", "F", "FB", "FDX", "FOX", "GD", "GE",
            "GILD", "GM", "GOOG", "GS", "HAL", "HD", "HON", "IBM", "INTC",
            "JNJ", "JPM", "KMI", "KO", "LLY", "LMT", "LOW", "MA", "MCD", "MDLZ", "MDT",
            "MET", "MMM", "MO", "MON", "MRK", "MS", "MSFT", "NEE", "NKE", "ORCL", "OXY",
            "PCLN", "PEP", "PFE", "PG", "PM", "PYPL", "QCOM", "RTN", "SBUX", "SLB",
            "SO", "SPG", "T", "TGT", "TWX", "TXN", "UNH", "UNP", "UPS", "USB",
            "UTX", "V", "VZ", "WBA", "WFC", "WMT", "XOM"};

        return tickers;
    }

    public void loadHistoric(Date date) {
        YahooDataGetter yah = new YahooDataGetter();
        String[] tickers = getSP100();

        logger.info("Starting to load historic data");

        for (String ticker : tickers) {
            yah.readData(date, 200, ticker, 0);
            stockMap.put(ticker, yah.data);
        }

        logger.info("Finished to load historic data");
    }

    public void updateDataWithActualValues(Date date) {
        if (stockMap.isEmpty()) {
            logger.severe("updateDataWithActualValues - stockMap.isEmpty");
        }

        logger.info("Starting to load actual data");

        GoogleActDataGetter goog = new GoogleActDataGetter();

        try {
            String[] tickerSymbols = getSP100();
            Map<String, Double> valuesMap = goog.readActualData(tickerSymbols);

            if (tickerSymbols.length != valuesMap.size()) {
                logger.warning("Not all actual data has been loaded! Missing " + (tickerSymbols.length - valuesMap.size()) + " stock(s).");
            }

            for (Iterator<Map.Entry<String, HistoricData>> it = stockMap.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, HistoricData> entry = it.next();

                Double valueRef = valuesMap.get(entry.getKey());
                if (valueRef == null) {
                    logger.warning("Cannot load actual data for: " + entry.getKey() + "! This stock will not be used.");
                    it.remove();
                    continue;
                }

                entry.getValue().adjClose[0] = valueRef;
            }

        } catch (IOException | NumberFormatException ex) {
            logger.warning("Failed to load actual data from google at once. Exception: " + ex.getMessage());
            logger.info("Loading one at a time...");
            for (Iterator<Map.Entry<String, HistoricData>> it = stockMap.entrySet().iterator(); it.hasNext();) {
                HistoricData histData = it.next().getValue();
                try {
                    histData.adjClose[0] = goog.readActualData(histData.tickerSymbol);
                } catch (IOException | NumberFormatException ex2) {
                    logger.warning("Cannot load actual data for: " + histData.tickerSymbol + ", exception: " + ex2.getMessage());
                    it.remove();
                }
            }
        }

        logger.info("Finished to load actual data");
    }

    public void computeIndicators() {
        IndicatorCalculator calc = new IndicatorCalculator();

        logger.info("Starting to compute indicators");

        for (HistoricData histData : stockMap.values()) {
            histData.sma200 = calc.SMA(200, histData.adjClose);
            histData.sma5 = calc.SMA(5, histData.adjClose);
            histData.rsi2 = calc.RSI(histData.adjClose);
        }

        logger.info("Finished to compute indicators");
    }
}
