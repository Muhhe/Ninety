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
        if ((status == null) || (broker == null)) {
            logger.severe("Status or data is NULL!");
        }
        this.status = status;
        this.broker = broker;
    }

    private TradeOrder GetSellAllOrder(VXVMTData data) {
        if ((status.heldType == VXVMTSignal.Type.None) || (status.heldPosition == 0)) {
            return null;
        }

        TradeOrder order = new TradeOrder();
        order.orderType = TradeOrder.OrderType.SELL;
        if (status.heldType == VXVMTSignal.Type.VXX) {
            order.expectedPrice = data.indicators.actVXXvalue;
            order.tickerSymbol = "VXX";
        } else if (status.heldType == VXVMTSignal.Type.XIV) {
            order.expectedPrice = data.indicators.actXIVvalue;
            order.tickerSymbol = "XIV";
        } else if (status.heldType == VXVMTSignal.Type.GLD) {
            order.expectedPrice = data.indicators.actGLDvalue;
            order.tickerSymbol = "GLD";
        }

        order.position = status.heldPosition;

        return order;
    }

    private TradeOrder GetBuyOrder(VXVMTSignal.Type ticker, int position, VXVMTData data) {
        if ((ticker == VXVMTSignal.Type.None) || (position == 0)) {
            logger.warning("Invalid trade order request.");
            return null;
        }

        TradeOrder order = new TradeOrder();
        order.orderType = position > 0 ? TradeOrder.OrderType.BUY : TradeOrder.OrderType.SELL;
        if (ticker == VXVMTSignal.Type.VXX) {
            order.expectedPrice = data.indicators.actVXXvalue;
            order.tickerSymbol = "VXX";
        } else if (ticker == VXVMTSignal.Type.XIV) {
            order.expectedPrice = data.indicators.actXIVvalue;
            order.tickerSymbol = "XIV";
        } else if (ticker == VXVMTSignal.Type.GLD) {
            order.expectedPrice = data.indicators.actGLDvalue;
            order.tickerSymbol = "GLD";
        }

        order.position = abs(position);

        return order;
    }

    private int GetMaxPosition(VXVMTSignal.Type signalType, VXVMTData data) {
        return GetDesiredPosition(signalType, 1, data);
    }

    private int GetDesiredPosition(VXVMTSignal.Type signalType, double exposure, VXVMTData data) {
        double value = 0;

        if (signalType == VXVMTSignal.Type.VXX) {
            value = data.indicators.actVXXvalue;
        } else if (signalType == VXVMTSignal.Type.XIV) {
            value = data.indicators.actXIVvalue;
        } else if (signalType == VXVMTSignal.Type.GLD) {
            value = data.indicators.actGLDvalue;
        }

        // Budget is lowered by 1% for safety reasons (slipage etc.)
        double budget = status.GetEquity(data.indicators.actXIVvalue, data.indicators.actVXXvalue, data.indicators.actGLDvalue) * 0.99;

        return (int) (budget / value * exposure);
    }

    private int GetDesiredPosition(VXVMTSignal signal, VXVMTData data) {
        return GetDesiredPosition(signal.type, signal.exposure, data);
    }

    private List<TradeOrder> PrepareOrders(VXVMTSignal signal, VXVMTData data) {
        logger.info("Signal - type: " + signal.type + ", exposure: " + TradeFormatter.toString(signal.exposure));

        List<TradeOrder> tradeOrders = new ArrayList<TradeOrder>();
        if (signal.type != status.heldType) {
            TradeOrder sellOrder = GetSellAllOrder(data);
            if (sellOrder != null) {
                tradeOrders.add(sellOrder);
            }

            if (signal.type != VXVMTSignal.Type.None) {
                TradeOrder buyOrder = GetBuyOrder(signal.type, GetDesiredPosition(signal, data), data);
                if (buyOrder != null) {
                    tradeOrders.add(buyOrder);
                }
            }
        } else if (signal.type != VXVMTSignal.Type.None) {
            int desiredPosition = GetDesiredPosition(signal, data);
            int diffInPosition = desiredPosition - status.heldPosition;
            double diffRel = (double) diffInPosition / GetMaxPosition(signal.type, data);
            if ((abs(diffInPosition) > 100) || (abs(diffRel) > 0.1)) {
                logger.info("Target position: " + signal.type + " - " + desiredPosition);
                TradeOrder order = GetBuyOrder(signal.type, diffInPosition, data);
                if (order != null) {
                    tradeOrders.add(order);
                }
            } else {
                logger.info("Target position: " + signal.type + " - " + desiredPosition + ". Held: " + status.heldPosition
                        + ". Difference only " + TradeFormatter.toString(diffRel * 100) + "%. Not worth issuing order");
            }
        }

        return tradeOrders;
    }

    public VXVMTSignal Run(VXVMTData data) {
        if (data == null) {
            logger.severe("VXVMT run: data is null !!!");
            return null;
        }

        if (!broker.isConnected()) {
            logger.severe("Broker not connected!");
            return null;
        }

        logger.info("Subscribing data. (40 sec wait)");
        
        //logger.warning("REMOVE!");
        broker.SubscribeRealtimeData("XIV");
        broker.SubscribeRealtimeData("VXX");
        broker.SubscribeRealtimeData("VIX3M", IBroker.SecType.IND);
        broker.SubscribeRealtimeData("VXMT", IBroker.SecType.IND);

        try {
            Thread.sleep(40000);
        } catch (InterruptedException ex) {
        }

        status.PrintStatus(data.indicators.actXIVvalue, data.indicators.actVXXvalue, data.indicators.actGLDvalue);

        VXVMTDataPreparator.UpdateIndicators(broker, data);

        if (!VXVMTChecker.CheckDataIndicators(data)) {
            logger.severe("Failed data chek!");
            return null;
        }

        VXVMTSignal signal = RunStrategy(data);

        status.PrintStatus(data.indicators.actXIVvalue, data.indicators.actVXXvalue, data.indicators.actGLDvalue);

        return signal;
    }

    public VXVMTSignal RunStrategy(VXVMTData data) {
        VXVMTSignal signal = VXVMTStrategy.CalculateFinalSignal(data);

        List<TradeOrder> orders = PrepareOrders(signal, data);
        for (TradeOrder tradeOrder : orders) {
            broker.PlaceOrder(tradeOrder);
            // wait until sold order is filled (if there is one)
            broker.waitUntilOrdersClosed(10);
        }

        if (!broker.waitUntilOrdersClosed(50)) {
            logger.warning("Some orders were not closed on time.");
        }

        ProcessSubmittedOrders();
        broker.clearOrderMaps();

        return signal;
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

            logger.info("Order closed - " + order.toString());
            MailSender.AddLineToMail("Filled " + order.order.toString() + ", price: " + order.fillPrice + "$");

            if (order.order.orderType == TradeOrder.OrderType.SELL) {
                double realized = (order.fillPrice - status.avgPrice) * order.filled;
                double realizedPrc = realized / status.closingEquity * 100.0;
                
                String msg = "Profit/loss: " + TradeFormatter.toString(realized) + "$ = " + TradeFormatter.toString(realizedPrc) + "%";
                
                status.freeCapital += order.filled * order.fillPrice;
                double fee = GetOrderFee(order.filled);
                status.freeCapital -= fee;
                status.fees += fee;

                status.heldPosition -= order.filled;

                if (status.heldPosition == 0) {
                    status.heldType = VXVMTSignal.Type.None;
                    status.avgPrice = 0;
                } else {
                    status.heldType = VXVMTSignal.typeFromString(order.order.tickerSymbol);
                }
                
                logger.info(msg);
                MailSender.AddLineToMail(msg);
            }

            if (order.order.orderType == TradeOrder.OrderType.BUY) {
                status.freeCapital -= order.filled * order.fillPrice;
                double fee = GetOrderFee(order.filled);
                status.freeCapital -= fee;
                status.fees += fee;

                status.avgPrice = ((status.avgPrice * status.heldPosition) + (order.fillPrice * order.filled)) / (status.heldPosition + order.filled);

                status.heldPosition += order.filled;
                status.heldType = VXVMTSignal.typeFromString(order.order.tickerSymbol);
            }

            it.remove();
        }
    }
}
