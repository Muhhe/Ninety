/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyMondayBuyer;

import backtesting.BTLogLvl;
import communication.IBroker;
import communication.OrderStatus;
import communication.TradeOrder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import tradingapp.GlobalConfig;
import tradingapp.MailSender;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class MBRunner implements Runnable {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final MBData data;
    private final MBStatus status;
    private final IBroker broker;

    int offset = 0;//GlobalConfig.isBacktest ? 0 : 1;

    public MBRunner(MBData stockData, MBStatus status, IBroker broker) {
        this.data = stockData;
        this.status = status;
        this.broker = broker;
    }

    public List<TradeOrder> calculateSells() {
//        if (data.actualDataMap.isEmpty()) {
//            return new ArrayList<>();
//        }

        List<MBHeldTicker> sells = new ArrayList<>();
        for (MBHeldTicker held : status.heldTickers.values()) {
            double high = data.ohlcDataMap.get(held.ticker).highs[offset];
            double low = data.ohlcDataMap.get(held.ticker).lows[offset];

            double profitTarget = held.price * 1.15;
            double stopLoss = held.price * 0.9;
            if (high > profitTarget) {
                sells.add(held);
                logger.info("Selling: " + held.ticker + " at profit targer: " + profitTarget);
            }
            if (low < stopLoss) {
                sells.add(held);
                logger.info("Selling: " + held.ticker + ", stop loss: " + stopLoss);
            }
        }

        List<TradeOrder> tradeOrders = new ArrayList<>();
        for (MBHeldTicker held : sells) {

            TradeOrder order = new TradeOrder();
            order.orderType = TradeOrder.OrderType.SELL;
            order.position = held.position;
            order.tickerSymbol = held.ticker;
            order.expectedPrice = data.getLastKnownPrice(held.ticker);
            tradeOrders.add(order);

            //MBIndicators indicators = data.indicatorsMap.get(held.ticker);
            //logger.fine("SELL: " + held.ticker + " " + order);
            logger.log(BTLogLvl.BT_STATS, "SELL: " + held.ticker + " profit/Loss: " + (held.position * (data.getLastKnownPrice(held.ticker) - held.price)));
        }
        return tradeOrders;
    }

    public List<TradeOrder> calculateBuys() {
        List<String> buys = new ArrayList<>();

        data.indicatorsMap.entrySet().forEach((entry) -> {
            MBIndicators indicators = entry.getValue();

            //logger.info("Indicators for " + entry.getKey() + " :" + indicators.toString());
            if (IsBuyable(indicators, data.spxBT50) && !status.recentlySold.containsKey(entry.getKey())) {
                logger.info("BUY: " + entry.getKey() + ", RSI2: " + indicators.rsi2 + ", Vol: " + indicators.vol);
                buys.add(entry.getKey());
            }
        });

        //todo
        //data.PrepareActualData(buys);
        Collections.sort(buys, new Comparator<String>() {
            @Override
            public int compare(String ticker1, String ticker2) {
                MBIndicators indicators1 = data.indicatorsMap.get(ticker1);
                MBIndicators indicators2 = data.indicatorsMap.get(ticker2);
                if ((indicators1 == null) || (indicators2 == null)) {
                    logger.severe("Cannot find indicators for " + ticker1 + " and/or " + ticker2);
                    return -1;
                    //TODO: pruser
                }
                return Double.compare(indicators1.vol, indicators2.vol);
            }
        });

        int emptySlots = MBStatus.PORTIONS_NUM - status.heldTickers.size();
        List<TradeOrder> tradeOrders = new ArrayList<>();

        // for (int i = 0; i < buys.size(); i++) {
        for (String ticker : buys) {
            //String ticker = buys.get(i);
            if (emptySlots > 0 && !status.heldTickers.containsKey(ticker)) {
                emptySlots--;

                double price = data.getLastKnownPrice(ticker);

                TradeOrder order = new TradeOrder();
                order.orderType = TradeOrder.OrderType.BUY;
                order.position = (int) (status.equity / MBStatus.PORTIONS_NUM / price);
                order.tickerSymbol = ticker;
                order.expectedPrice = price;
                tradeOrders.add(order);

                MBIndicators indicators = data.indicatorsMap.get(ticker);
                //logger.fine("BUY: " + ticker + " " + order + " " + indicators.toString());

                logger.log(BTLogLvl.BT_STATS, "BUY: " + ticker + " " + order + " " + indicators.toString());
            }
        }

        return tradeOrders;
    }

    public void runSells() {

//        for (MBHeldTicker heldTicker : status.heldTickers.values()) {
//            logger.log(BTLogLvl.BT_STATS, heldTicker.toString());
//        }
        logger.info("Starting computing stocks to sell");
        List<TradeOrder> sellOrders = calculateSells();

        for (TradeOrder tradeOrder : sellOrders) {
            broker.PlaceOrder(tradeOrder);
            status.recentlySold.put(tradeOrder.tickerSymbol, LocalDate.now());
        }
        logger.info("Finished computing stocks to sell.");

        if (!broker.waitUntilOrdersClosed(20)) {
            logger.warning("Some SELL orders were not closed on time.");
        }

        ProcessSubmittedOrders();

        //data.UpdateDataWithActValues();
        if (!MBChecker.CheckHeldPositions(status, broker, 10)) {
            logger.severe("Failed check positions after sell.");
        }
    }

    public void runBuys() {
        logger.info("Starting computing stocks to buy");
        List<TradeOrder> buyOrders = calculateBuys();

        for (TradeOrder tradeOrder : buyOrders) {
            broker.PlaceOrder(tradeOrder);
        }

        logger.info("Finished computing stocks to buy.");

        if (!broker.waitUntilOrdersClosed(40)) {
            logger.warning("Some orders were not closed on time.");
        }

        ProcessSubmittedOrders();
    }
    
    

    @Override
    public void run() {

        if (TradeTimer.isFirstDoW()) {
            logger.info("Today is buying day!!!");
        } else {
            logger.info("Today is just sell day!!!");
        }

        status.PrintStatus();

        MBChecker.PerformChecks(status, data, broker);

        runSells();

        if (!MBChecker.CheckStockData(data, status)) {
            logger.severe("Currupted stock data after sell. Exiting trading!");
            status.SaveTradingStatus();
            broker.disconnect();
            return;
        }

        //data.UnSubscribeRealtimeData(broker);
        if (TradeTimer.isFirstDoW()) {
            runBuys();
        }

        broker.clearOrderMaps();

        MBChecker.CheckHeldPositions(status, broker, 30);
        MBChecker.CheckCash(status, broker);

        status.SaveTradingStatus();

        logger.info("Trading day finished");
        status.PrintStatus();
        logger.fine(broker.GetAccountSummary().toString());
    }

    public boolean IsBuyable(MBIndicators indicators, boolean spyFilter) {
        return (spyFilter && indicators.rsi2 < 20.0 && indicators.sma10x2);
    }

    public void ProcessSubmittedOrders() {

        double realizedPL = 0;

        for (Iterator<Map.Entry<Integer, OrderStatus>> it = broker.GetOrderStatuses().entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, OrderStatus> entry = it.next();
            OrderStatus order = entry.getValue();

            if (order.status != OrderStatus.Status.FILLED) {
                logger.severe("Order NOT closed - " + order.toString());
                status.UpdateHeldByOrderStatus(order);
                continue;
            }

            if (order.order.orderType == TradeOrder.OrderType.SELL) {
                MBHeldTicker held = status.heldTickers.get(order.order.tickerSymbol);
                if (held != null) {
                    realizedPL += held.CalculateProfitIfSold(order.fillPrice);
                }
            }

            logger.info("Order closed - " + order.toString());
            status.UpdateHeldByOrderStatus(order);

            it.remove();
        }

        if (realizedPL != 0) {
            MailSender.AddLineToMail("Today's realized profit/loss: " + TradeFormatter.toString(realizedPL) + "$");
            logger.info("Today's realized profit/loss: " + TradeFormatter.toString(realizedPL) + "$");
        }
    }

}
