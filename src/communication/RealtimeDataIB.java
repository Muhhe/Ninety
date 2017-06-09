/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communication;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import static tradingapp.MainWindow90.LOGGER_COMM_NAME;

/**
 *
 * @author Muhe
 */
public class RealtimeDataIB {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private final static Logger loggerComm = Logger.getLogger(LOGGER_COMM_NAME );

    private class Data {

        public String ticker;
        public int orderId;
        public double last = 0;
        public double ask = 0;
        public double bid = 0;
        public double close = 0;

        private Data(String ticker, int orderID) {
            this.ticker = ticker;
            this.orderId = orderID;
        }
    }

    private final Map<String, Data> dataMapByTicker = new HashMap<>(100);
    private final Map<Integer, Data> dataMapById = new HashMap<>(100);

    public synchronized void CreateNew(String ticker, int orderID) {
        Data data = new Data(ticker, orderID);
        dataMapByTicker.put(ticker, data);
        dataMapById.put(orderID, data);
    }

    public synchronized void UpdateValue(int orderID, int field, double price) {
        Data data = dataMapById.get(orderID);
        if (data == null) {
            loggerComm.warning("Reatime data put: cannot find data for order ID " + orderID);
            return;
        }

        switch (field) {
            case 1:
                data.bid = price;
                break;
            case 2:
                data.ask = price;
                break;
            case 4:
                if (data.last <= 0) {
                    loggerComm.finer("Actual data updated. Ticker " + data.ticker + ", orderId " + orderID + ", price " + price);
                }
                data.last = price;
                break;
            case 6://high
            case 7://low
                break;
            case 9://close
                /*if (data.close <= 0) {
                    loggerComm.finer("Close data updated. Ticker " + data.ticker + ", orderId " + orderID + ", price " + price);
                }*/
                data.close = price;
                break;
            default:
            // Sometimes 14
            //logger.finest("Reatime data put: Unknown field " + field + ", ID " + orderID);
        }
    }

    public synchronized double GetLastPrice(String ticker) {
        Data data = dataMapByTicker.get(ticker);

        if (data == null) {
            logger.fine("Reatime data get: cannot find data for ticker " + ticker);
            return 0;
        }
        
        if (data.last <= 0) {
            loggerComm.warning("Reatime data get: Close data used for ticker " + data.ticker + ", value " + data.close);
            return data.close;
        }
        
        return data.last;
    }

    public synchronized Set<Integer> GetAllOrderIds() {
        return dataMapById.keySet();
    }

    public synchronized void ClearMaps() {
        dataMapById.clear();
        dataMapByTicker.clear();
    }

    public static String GetTickPriceFieldString(int field) {
        switch (field) {
            case 1:
                return "bid";
            case 2:
                return "ask";
            case 4:
                return "last";
            case 6:
                return "high";
            case 7:
                return "low";
            case 9:
                return "close";
            default:
                return Integer.toString(field);
        }
    }
}
