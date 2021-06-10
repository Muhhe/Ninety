/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communication;

import data.CloseData;
import data.OHLCData;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import static tradingapp.MainWindow90.LOGGER_COMM_NAME;

/**
 *
 * @author Muhe
 */
public class HistoricalDataIB {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private final static Logger loggerComm = Logger.getLogger(LOGGER_COMM_NAME);

    private final Map<String, OHLCData> dataMapByTicker = new HashMap<>(100);
    private final Map<Integer, OHLCData> dataMapById = new HashMap<>(100);
    private final Map<Integer, Integer> counterMapById = new HashMap<>(100);

    void CreateNew(String ticker, int orderId, int count) {
        loggerComm.info("Created new hist data " + ticker + ", id: " + orderId);
        OHLCData data = new OHLCData(count);
        dataMapByTicker.put(ticker, data);
        dataMapById.put(orderId, data);
        counterMapById.put(orderId, count);
    }

    public synchronized boolean UpdateValue(int orderID, String date, double open, double high, double low, double close) {
        
        if (date.startsWith("finished")) {
            loggerComm.info("Finished " + orderID);
            return false; //todo
        }
        
        OHLCData data = dataMapById.get(orderID);
        Integer counter = counterMapById.get(orderID);
        if (data == null || counter == null) {
            loggerComm.warning("Historical data put: cannot find data for order ID " + orderID);
            return true;
        }

        if (counter == 0) {
            loggerComm.finer("Historical data updated for id: " + orderID);
            return true;
        }

        counter--;

        counterMapById.put(orderID, counter);

        data.opens[counter] = open;
        data.highs[counter] = high;
        data.lows[counter] = low;
        data.adjCloses[counter] = close;

        try {
            data.dates[counter] = LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (Exception e) {
            loggerComm.severe("Historical data date parse failed for id: " + orderID + ", :" + e);
        }

        if (counter == 0) {
            loggerComm.finer("Historical data updated for id: " + orderID);
            return true;
        }

        return false;
    }

    public synchronized CloseData GetCloseData(String ticker) {
        OHLCData data = dataMapByTicker.get(ticker);

        if (data == null) {
            logger.fine("Historical data get: cannot find data for ticker " + ticker);
            return null;
        }

        return new CloseData(data.adjCloses, data.dates);
    }

    public synchronized OHLCData GetOHLCData(String ticker) {
        OHLCData data = dataMapByTicker.get(ticker);

        if (data == null) {
            logger.fine("Historical data get: cannot find data for ticker " + ticker);
            return null;
        }

        return data;
    }

    public synchronized void ClearMaps() {
        dataMapById.clear();
        dataMapByTicker.clear();
        counterMapById.clear();
    }

    public synchronized Set<Integer> GetAllOrderIds() {
        return dataMapById.keySet();
    }

}
