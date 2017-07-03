/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategy90;

import communication.IBroker;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class NinetyDataPreparator implements Runnable {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final StockDataForNinety stockData;
    private final IBroker broker;

    public NinetyDataPreparator(StockDataForNinety stockData, IBroker broker) {
        this.stockData = stockData;
        this.broker = broker;
    }

    @Override
    public void run() {
        if (!broker.connect() ) {
            logger.severe("Cannot connect to IB");
            return;
        }
        logger.fine("Subscribing real-time data");
        stockData.SubscribeRealtimeData(broker);
        logger.fine("Waiting for real-time data - 20 sec");
        try {
            Thread.sleep(20000);
        } catch (InterruptedException ex) {
        }

        stockData.PrepareData();
        
        stockData.UnSubscribeRealtimeData(broker);

        broker.disconnect();
    }
}
