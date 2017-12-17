/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communication;

import data.CloseData;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import static tradingapp.MainWindow90.LOGGER_COMM_NAME;

/**
 *
 * @author Muhe
 */
public class HistoricalDataIB {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private final static Logger loggerComm = Logger.getLogger(LOGGER_COMM_NAME);

    private final Map<String, CloseData> dataMapByTicker = new HashMap<>(100);
    private final Map<Integer, CloseData> dataMapById = new HashMap<>(100);
    private final Map<Integer, Integer> counterMapById = new HashMap<>(100);

    void CreateNew(String ticker, int orderId, int count) {
        CloseData data = new CloseData(count);
        dataMapByTicker.put(ticker, data);
        dataMapById.put(orderId, data);
        counterMapById.put(orderId, count);
    }

    public synchronized void UpdateValue(int orderID, String date, double closePrice) {
        CloseData data = dataMapById.get(orderID);
        Integer counter = counterMapById.get(orderID);
        if (data == null || counter == null) {
            loggerComm.warning("Historical data put: cannot find data for order ID " + orderID);
            return;
        }
        
        if (counter == 0) {
            loggerComm.finer("Historical data updated for id: " + orderID);
            return;
        }
        
        counter--;
        
        counterMapById.put(orderID, counter);
        
        data.adjCloses[counter] = closePrice;
        
        try {
            data.dates[counter] = LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (Exception e) {
        }
    }

    public synchronized CloseData GetCloseData(String ticker) {
        CloseData data = dataMapByTicker.get(ticker);

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

}
