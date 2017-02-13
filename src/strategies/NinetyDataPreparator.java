/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import communication.IBBroker;
import java.time.LocalDate;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class NinetyDataPreparator implements Runnable {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final StockDataForNinety stockData;
    private final IBBroker broker;

    public NinetyDataPreparator(StockDataForNinety stockData, IBBroker broker) {
        this.stockData = stockData;
        this.broker = broker;
    }

    @Override
    public void run() {
        if (!broker.connect() ) {
            logger.severe("Cannot connect to IB");
        }
        logger.fine("Subscribing real-time data");
        stockData.SubscribeRealtimeData(broker);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }

        stockData.PrepareHistData();
        stockData.UpdateDataWithActValuesIB(broker);

        stockData.UnSubscribeRealtimeData(broker);

        stockData.CalculateIndicators();

        broker.disconnect();
    }
}
