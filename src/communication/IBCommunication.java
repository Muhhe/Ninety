/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communication;

import com.ib.controller.ApiController;
import com.ib.controller.Formats;
import com.ib.controller.NewContract;
import com.ib.controller.NewOrder;
import com.ib.controller.NewOrderState;
import com.ib.controller.OrderStatus;
import com.ib.controller.OrderType;
import com.ib.controller.Types;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import tradingapp.TradeOrder;

/**
 *
 * @author Muhe
 */
public class IBCommunication implements ApiController.IConnectionHandler {
    
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static int m_orderIdCounter = 1;

    private final ApiController m_controller;
    
    public IBCommunication() {
        m_controller = new ApiController(this, new CommLogger(), new CommLogger());
    }
    
    public void connect(int port) {
        logger.info("Connecting to IB");
        m_controller.connect("127.0.0.1", port, 1);
    }
    
    public void PlaceOrder(TradeOrder tradeOrder) {
        NewContract contract = new NewContract();
        contract.symbol(tradeOrder.tickerSymbol);
        contract.exchange("SMART");
        contract.secType(Types.SecType.CFD);
        contract.currency("USD");
        
        NewOrder order = new NewOrder();
        //order.orderId(m_orderIdCounter++);
        order.orderId(0);
        
        if (tradeOrder.orderType == TradeOrder.OrderType.BUY) {
            order.action(Types.Action.BUY);
        } else {
            order.action(Types.Action.SELL);
        }
        order.totalQuantity(tradeOrder.position);
        order.orderType(OrderType.MKT);
        order.tif(Types.TimeInForce.DAY);
        
        m_controller.placeOrModifyOrder(contract, order, new ApiController.IOrderHandler() {
            @Override
            public void orderState(NewOrderState orderState) {
                logger.info(orderState.warningText());
            }

            @Override
            public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
                if (status == status.Filled) {
                    logger.info("Order filled: " + filled + " for price " + avgFillPrice);
                } else {
                    logger.severe("Order not fully filled: " + filled + ", remaining: " + remaining + " for price " + avgFillPrice);
                    logger.severe("Held: " + whyHeld);
                }
            }

            @Override
            public void handle(int errorCode, String errorMsg) {
                logger.info("Order code and message: " + errorCode + errorMsg);
            }
        });
    }

    @Override
    public void connected() {
        logger.info("Connected to IB");
        
        //m_controller.nextValidId(m_orderIdCounter);
        
        m_controller.reqCurrentTime(new ApiController.ITimeHandler() {
            @Override
            public void currentTime(long time) {
                
                LocalDateTime localNow = LocalDateTime.now();
                ZoneId currentZone = ZoneId.systemDefault();
                ZoneId usZone = ZoneId.of("America/New_York");
                //ZonedDateTime zonedUS = localNow.atZone(usZone);
                ZonedDateTime zonedPRG = ZonedDateTime.of(localNow, currentZone);
                ZonedDateTime zonedUS = zonedPRG.withZoneSameInstant(usZone);
                
                logger.info("Prague time: " + zonedPRG.toString());
                logger.info("New york time: " + zonedUS.toString());

                Calendar calendar = new GregorianCalendar();
                calendar.setTimeInMillis(time * 1000);
                TimeZone tzNY = TimeZone.getTimeZone("America/New_York");
                //TimeZone tzPRG = TimeZone.getTimeZone("America/New_York");
                
                //calendar.setTimeZone(tzPRG);
                logger.info("Prague time: " + calendar.getTime().toString());
                calendar.setTimeZone(tzNY);
                logger.info("New york time: " + calendar.getTime().toString());

                //show("Server date/time is " + Formats.fmtDate(time * 1000));
                //logger.info("New york time: " + Formats.fmtDate(calendar.getTimeInMillis()));
            }
        });
    }
    
    public void SellAllPositions() {
        
        class OrderContract {
            NewOrder order;
            NewContract contract;

            OrderContract(NewOrder o, NewContract c) {
                this.order =  o;
                this.contract = c;
            }
        }
        //List<NewOrder> orders = new ArrayList<NewOrder>();
        Map<String, OrderContract> orders = new ConcurrentHashMap<String, OrderContract>();
        
        m_controller.reqPositions(new ApiController.IPositionHandler() {
            @Override
            public void position(String account, NewContract contract, int position, double avgCost) {
                if (position == 0) {
                    return;
                }
                NewOrder order = new NewOrder();
                order.orderId(0);

                order.action(position > 0 ? Types.Action.SELL : Types.Action.BUY);
                order.totalQuantity(Math.abs(position));
                order.orderType(OrderType.MKT);
                order.tif(Types.TimeInForce.DAY);
                
                contract.exchange("SMART");
                contract.secType(Types.SecType.STK);
                contract.currency("USD");
                
                if (!orders.containsKey(contract.symbol())) {
                    orders.put(contract.symbol(), new OrderContract(order, contract));
                }
                
                //logger.info("Sell ORDER: " + contract.symbol() + ", postion: " + position);

                /*m_controller.placeOrModifyOrder(contract, order, new ApiController.IOrderHandler() {
                    @Override
                    public void orderState(NewOrderState orderState) {
                    }

                    @Override
                    public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
                        if (status == status.Filled) {
                            logger.info("Order filled: " + filled + " for price " + avgFillPrice);
                        } else {
                            logger.severe("Order not fully filled: " + filled + ", remaining: " + remaining + " for price " + avgFillPrice);
                            logger.severe("Held: " + whyHeld);
                        }
                    }

                    @Override
                    public void handle(int errorCode, String errorMsg) {
                    }
                });*/
            }

            @Override
            public void positionEnd() {
                
                for (OrderContract value : orders.values()) {

                    logger.info("Sell ORDER: " + value.contract.symbol() + ", postion: " + value.order.totalQuantity());
                    
                    m_controller.placeOrModifyOrder(value.contract, value.order, new ApiController.IOrderHandler() {
                        @Override
                        public void orderState(NewOrderState orderState) {
                        }

                        @Override
                        public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
                            if (status == status.Filled) {
                                logger.info("Order filled: " + filled + " for price " + avgFillPrice);
                            } else {
                                logger.severe("Order not fully filled: " + filled + ", remaining: " + remaining + " for price " + avgFillPrice);
                                logger.severe("Held: " + whyHeld);
                            }
                        }

                        @Override
                        public void handle(int errorCode, String errorMsg) {
                        }
                    });
                }
            }
        });
    }

    @Override
    public void disconnected() {
        logger.info("Disconnected from IB");
    }

    @Override
    public void accountList(ArrayList<String> list) {
        logger.info("accountList:");
        for (String string : list) {
            logger.info(string);
        }
    }

    @Override
    public void error(Exception e) {
        logger.info("Comm error: " + e.getMessage());
    }

    @Override
    public void message(int id, int errorCode, String errorMsg) {
        logger.info("Comm message id: " + id + ", errorCode: " + errorCode + ", msg :" + errorMsg);
    }

    @Override
    public void show(String string) {
        logger.info("Comm show: " + string);
    }
    
}
