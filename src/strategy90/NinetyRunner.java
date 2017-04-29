/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategy90;

import communication.IBroker;
import communication.OrderStatus;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import tradingapp.MailSender;
import tradingapp.TradeFormatter;
import tradingapp.TradeOrder;

/**
 *
 * @author Muhe
 */
public class NinetyRunner implements Runnable {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final StockDataForNinety stockData;
    private final StatusDataForNinety statusData;
    private final IBroker broker;

    public NinetyRunner(StockDataForNinety stockData, StatusDataForNinety statusData, IBroker broker) {
        this.stockData = stockData;
        this.statusData = statusData;
        this.broker = broker;
    }

    @Override
    public void run() {
        logger.info("Starting Ninety strategy");

        int connectTries = 3;

        while (connectTries-- > 0) {
            if (broker.connect()) {
                break;
            } else {
                logger.warning("Cannot connect to IB. Trying again.");
            }
        }

        if (connectTries < 0) {
            logger.warning("Failed to connect to IB. Exiting trading!");
            return;
        }

        stockData.SubscribeRealtimeData(broker);
        try {
            Thread.sleep(65000);
        } catch (InterruptedException ex) {
        }

        logger.info(broker.GetAccountSummary().toString());

        stockData.UpdateDataWithActValues();

        if (!NinetyChecker.CheckStockData(stockData, statusData)) {
            logger.severe("Currupted stock data. Exiting trading!");
            broker.disconnect();
            return;
        }

        statusData.PrintStatus();

        List<TradeOrder> sells = RunNinetySells();

        if (!broker.waitUntilOrdersClosed(20)) {
            logger.warning("Some SELL orders were not closed on time.");
        }

        ProcessSubmittedOrders();

        stockData.UpdateDataWithActValues();

        if (!NinetyChecker.CheckHeldPositions(statusData, broker)) {
            logger.severe("Failed check positions after sell.");
        }

        if (!NinetyChecker.CheckStockData(stockData, statusData)) {
            logger.severe("Currupted stock data after sell. Exiting trading!");
            statusData.SaveTradingStatus();
            broker.disconnect();
            return;
        }

        stockData.UnSubscribeRealtimeData(broker);

        RunNinetyBuys(sells);

        if (!broker.waitUntilOrdersClosed(40)) {
            logger.warning("Some orders were not closed on time.");
        }

        ProcessSubmittedOrders();
        broker.clearOrderMaps();

        NinetyChecker.CheckHeldPositions(statusData, broker);
        NinetyChecker.CheckCash(statusData, broker);

        statusData.SaveTradingStatus();

        logger.info("Trading day finished");
        statusData.PrintStatus();
        logger.fine(broker.GetAccountSummary().toString());

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

    // TODO: zlepsit design
    private void ProcessSubmittedOrders() {

        double realizedPL = 0;

        for (Iterator<Map.Entry<Integer, OrderStatus>> it = broker.GetOrderStatuses().entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, OrderStatus> entry = it.next();
            OrderStatus order = entry.getValue();

            if (order.status != OrderStatus.Status.FILLED) {
                logger.severe("Order NOT closed - " + order.toString());
                statusData.UpdateHeldByOrderStatus(order);
                continue;
            }

            if (order.order.orderType == TradeOrder.OrderType.SELL) {
                HeldStock held = statusData.heldStocks.get(order.order.tickerSymbol);
                if (held != null) {
                    realizedPL += held.CalculateProfitIfSold(order.fillPrice);
                }
            }

            logger.info("Order closed - " + order.toString());
            statusData.UpdateHeldByOrderStatus(order);

            it.remove();
        }

        if (realizedPL != 0) {
            MailSender.AddLineToMail("Today's realized profit/loss: " + TradeFormatter.toString(realizedPL) + "$");
            logger.info("Today's realized profit/loss: " + TradeFormatter.toString(realizedPL) + "$");
        }
    }

}
