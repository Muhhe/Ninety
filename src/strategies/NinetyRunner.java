/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import communication.IBBroker;
import communication.OrderStatus;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import static tradingapp.MainWindow.LOGGER_TADELOG_NAME;
import tradingapp.TradeOrder;

/**
 *
 * @author Muhe
 */
public class NinetyRunner implements Runnable {
    
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private final StockDataForNinety stockData;
    private final StatusDataForNinety statusData;
    private final IBBroker broker;

    public NinetyRunner(StockDataForNinety stockData, StatusDataForNinety statusData, IBBroker broker) {
        this.stockData = stockData;
        this.statusData = statusData;
        this.broker = broker;
    }
    
    @Override
    public void run() {
        logger.info("Starting Ninety strategy");
        
        if (!broker.connect() ) {
            logger.severe("Cannot connect to IB");
        }

        stockData.SubscribeRealtimeData(broker);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }
        
        logger.info(broker.accountSummary.toString());

        stockData.UpdateDataWithActValuesIB(broker);
        stockData.CalculateIndicators();
        stockData.CheckHistData(LocalDate.now());

        statusData.PrintStatus();

        List<TradeOrder> sells = RunNinetySells();

        if (!broker.waitUntilOrdersClosed(20)) {
            logger.severe("Some SELL orders were not closed on time.");
        }

        ProcessSubmittedOrders();
        NinetyChecker.PerformChecks(statusData, broker);

        stockData.UpdateDataWithActValuesIB(broker);
        stockData.CalculateIndicators();
        stockData.CheckHistData(LocalDate.now());

        stockData.UnSubscribeRealtimeData(broker);

        RunNinetyBuys(sells);

        if (!broker.waitUntilOrdersClosed(40)) {
            logger.severe("Some orders were not closed on time.");
        }

        ProcessSubmittedOrders();
        broker.clearOrderMaps();

        NinetyChecker.PerformChecks(statusData, broker);

        statusData.SaveHeldPositionsToXML();

        logger.info("Trading day finished");
        statusData.PrintStatus();
        logger.info(broker.accountSummary.toString());

        broker.disconnect();
    }
    
    public List<TradeOrder> RunNinetySells() {
        logger.info("Starting computing stocks to sell");

        // Selling held stocks
        List<TradeOrder> sellOrders = Ninety.ComputeStocksToSell(stockData.indicatorsMap, statusData);

        for (TradeOrder tradeOrder : sellOrders) {
            broker.PlaceOrder(tradeOrder);
        }

        logger.info("Finished computing stocks to sell.");
        
        return sellOrders;
    }
    
    private void RunNinetyBuys(List<TradeOrder> sellOrders) {
        logger.info("Starting computing stocks to buy");
        int remainingPortions = 20 - statusData.GetBoughtPortions();
        
        // Buying new stock
        TradeOrder buyOrder = Ninety.ComputeStocksToBuy(stockData.indicatorsMap, statusData, sellOrders);
        if (buyOrder != null) {
            broker.PlaceOrder(buyOrder);
            remainingPortions--;
        }
        
        // Buying more held stock
        List<TradeOrder> buyMoreOrders = Ninety.computeStocksToBuyMore(stockData.indicatorsMap, statusData, remainingPortions);

        for (TradeOrder tradeOrder : buyMoreOrders) {
            broker.PlaceOrder(tradeOrder);
        }

        logger.info("Finished computing stocks to buy.");
    }

    private void ProcessSubmittedOrders() {
        
        for (Iterator<Map.Entry<Integer, OrderStatus>> it = broker.orderStatusMap.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, OrderStatus> entry = it.next();
            OrderStatus order = entry.getValue();

            if (order.status != OrderStatus.Status.FILLED) {
                logger.severe("Order NOT closed - " + order.toString());
                statusData.UpdateHeldByOrderStatus(order);
                continue;
            }

            logger.info("Order closed - " + order.toString());
            statusData.UpdateHeldByOrderStatus(order);

            it.remove();
        }
    }

}
