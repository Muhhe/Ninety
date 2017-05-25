/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyVXVMT;

import communication.IBroker;
import communication.OrderStatus;
import communication.TradeOrder;
import static java.lang.Double.max;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import tradingapp.MailSender;
import tradingapp.TradeFormatter;

/**
 *
 * @author Muhe
 */
public class VXVMTRunner {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final VXVMTStatus status;
    private final IBroker broker;

    public VXVMTRunner(VXVMTStatus status, IBroker broker) {
        this.status = status;
        this.broker = broker;
    }

    private TradeOrder GetSellAllOrder(VXVMTIndicators indicators) {
        if ((status.heldType == VXVMTSignal.Type.None) || (status.heldPosition == 0)) {
            return null;
        }

        TradeOrder order = new TradeOrder();
        order.orderType = TradeOrder.OrderType.SELL;
        if (status.heldType == VXVMTSignal.Type.VXX) {
            order.expectedPrice = indicators.actVXXvalue;
            order.tickerSymbol = "VXX";
        } else {
            order.expectedPrice = indicators.actXIVvalue;
            order.tickerSymbol = "XIV";
        }

        order.position = status.heldPosition;

        return order;
    }

    private TradeOrder GetBuyOrder(VXVMTSignal.Type ticker, int position, VXVMTIndicators indicators) {
        if ((ticker == VXVMTSignal.Type.None) || (position == 0)) {
            logger.warning("Invalid trade order request.");
            return null;
        }

        TradeOrder order = new TradeOrder();
        order.orderType = position > 0 ? TradeOrder.OrderType.BUY : TradeOrder.OrderType.SELL;
        if (ticker == VXVMTSignal.Type.VXX) {
            order.expectedPrice = indicators.actVXXvalue;
            order.tickerSymbol = "VXX";
        } else {
            order.expectedPrice = indicators.actXIVvalue;
            order.tickerSymbol = "XIV";
        }

        order.position = abs(position);

        return order;
    }

    private int GetDesiredPosition(VXVMTSignal signal, VXVMTIndicators indicators) {
        double value = 0;

        if (signal.type == VXVMTSignal.Type.VXX) {
            value = indicators.actVXXvalue;
        } else {
            value = indicators.actXIVvalue;
        }

        return (int) (status.GetEquity(indicators.actXIVvalue, indicators.actVXXvalue) / value * signal.exposure);
    }

    private List<TradeOrder> PrepareOrders(VXVMTSignal signal, VXVMTIndicators indicators) {
        logger.info("Signal - type: " + signal.type + ", exposure: " + TradeFormatter.toString(signal.exposure));

        List<TradeOrder> tradeOrders = new ArrayList<TradeOrder>();
        if (signal.type != status.heldType) {
            TradeOrder sellOrder = GetSellAllOrder(indicators);
            if (sellOrder != null) {
                tradeOrders.add(sellOrder);
            }

            if (signal.type != VXVMTSignal.Type.None) {
                TradeOrder buyOrder = GetBuyOrder(signal.type, GetDesiredPosition(signal, indicators), indicators);
                if (buyOrder != null) {
                    tradeOrders.add(buyOrder);
                }
            }
        } else if (signal.type != VXVMTSignal.Type.None) {
            int desiredPosition = GetDesiredPosition(signal, indicators);
            int diffInPosition = desiredPosition - status.heldPosition;
            if (abs(diffInPosition) > 100) {
                TradeOrder order = GetBuyOrder(signal.type, diffInPosition, indicators);
                if (order != null) {
                    tradeOrders.add(order);
                }
            }
        }

        return tradeOrders;
    }

    public void Run() {
        if (!broker.connect()) {
            return;
        }
        
        logger.info("Subscribing data. (10 sec wait)");
        broker.SubscribeRealtimeData("XIV");
        broker.SubscribeRealtimeData("VXX");
        broker.SubscribeRealtimeData("VXV", IBroker.SecType.IND);
        broker.SubscribeRealtimeData("VXMT", IBroker.SecType.IND);
        
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
        }
        
        VXVMTIndicators indicators = VXVMTDataPreparator.LoadData(broker);
        
        status.PrintStatus(indicators.actXIVvalue, indicators.actVXXvalue);
        
        RunStrategy(indicators);
        
        status.PrintStatus(indicators.actXIVvalue, indicators.actVXXvalue);
        
        double equity = status.GetEquity(indicators.actXIVvalue, indicators.actVXXvalue);
        double diff = (equity - status.closingEquity);
        double prc = diff / status.closingEquity * 100.0;
        
        MailSender.AddLineToMail("Held '" + status.heldType + "', position: " + status.heldPosition);
        MailSender.AddLineToMail("Equity - " + TradeFormatter.toString(status.GetEquity(indicators.actXIVvalue, indicators.actVXXvalue)));
        MailSender.AddLineToMail("Today's profit/loss - " + TradeFormatter.toString(diff) + " = " + TradeFormatter.toString(prc));
        
        broker.disconnect();
        
        status.UpdateEquity(indicators.actXIVvalue, indicators.actVXXvalue);
        status.SaveTradingStatus();
    }

    public void RunStrategy(VXVMTIndicators indicators) {
        VXVMTSignal signal = VXVMTStrategy.CalculateFinalSignal(indicators);

        List<TradeOrder> orders = PrepareOrders(signal, indicators);
        for (TradeOrder tradeOrder : orders) {
            broker.PlaceOrder(tradeOrder);
        }

        if (!broker.waitUntilOrdersClosed(60)) {
            logger.warning("Some orders were not closed on time.");
        }
        
        MailSender.AddLineToMail("Today's signal - type: " + signal.type + ", exposure: " + TradeFormatter.toString(signal.exposure));
        
        ProcessSubmittedOrders();
        
    }
    
    public static double GetOrderFee(int position) {
        return max(1.0, position * 0.005);
    }

    private void ProcessSubmittedOrders() {

        for (Iterator<Map.Entry<Integer, OrderStatus>> it = broker.GetOrderStatuses().entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, OrderStatus> entry = it.next();
            OrderStatus order = entry.getValue();

            if (order.status != OrderStatus.Status.FILLED) {
                logger.severe("Order NOT closed - " + order.toString());
                continue;
            }

            if (order.order.orderType == TradeOrder.OrderType.SELL) {
                status.freeCapital += order.filled * order.fillPrice;
                double fee = GetOrderFee(order.filled);
                status.freeCapital -= fee;
                status.fees += fee;

                status.heldPosition -= order.filled;

                if (status.heldPosition == 0) {
                    status.heldType = VXVMTSignal.Type.None;
                } else {
                    status.heldType = VXVMTSignal.typeFromString(order.order.tickerSymbol);
                }
            }

            if (order.order.orderType == TradeOrder.OrderType.BUY) {
                status.freeCapital -= order.filled * order.fillPrice;
                double fee = GetOrderFee(order.filled);
                status.freeCapital -= fee;
                status.fees += fee;
                
                status.heldPosition += order.filled;
                status.heldType = VXVMTSignal.typeFromString(order.order.tickerSymbol);
            }

            logger.info("Order closed - " + order.toString());
            MailSender.AddLineToMail("Filled order - " + order.toString());

            it.remove();
        }
    }
}
