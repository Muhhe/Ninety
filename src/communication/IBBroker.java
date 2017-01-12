/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communication;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.Order;
import com.ib.client.TagValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import tradingapp.TradeOrder;
import tradingapp.TradingTimer;
import static tradingapp.MainWindow.LOGGER_COMM_NAME;

/**
 *
 * @author Muhe
 */
public class IBBroker extends BaseIBConnectionImpl {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private final static Logger loggerComm = Logger.getLogger(LOGGER_COMM_NAME );
    
    public final Map<Integer, OrderStatus> orderStatusMap = new ConcurrentHashMap<>();
    public final Map<Integer, OrderStatus> activeOrdersMap = new HashMap<>();
    
    public final RealtimeDataIB realtimeData = new RealtimeDataIB();
    
    public EClientSocket ibClientSocket = new EClientSocket(this);
    boolean connected = false;
    protected BlockingQueue<Integer> nextIdQueue = new LinkedBlockingQueue<>();
    protected CountDownLatch getPositionsCountdownLatch = null;
    protected CountDownLatch ordersClosedWaitCountdownLatch = null;
    protected CountDownLatch connectiongLatch = null;
    protected List<Position> positionsList = new ArrayList<>();
    private int nextOrderId = -1;
    
    public void connect() {
        if( !connected ) {
            loggerComm.info("Connecting to IB.");
            ibClientSocket.eConnect(null, 4001, 1 );
            // TODO: wait?
            connectiongLatch = new CountDownLatch(1);
            try {
                if (!connectiongLatch.await(10, TimeUnit.SECONDS)) {
                    loggerComm.severe("Cannot connect to IB");
                }
            } catch (InterruptedException ex) {
                loggerComm.log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void disconnect() {
        if( connected ) {
            loggerComm.info("Disconnecting from IB.");
            ibClientSocket.eDisconnect();
            connected = false;
        }
    }
    
    @Override
    public void connectionClosed() {
        loggerComm.info("Connection to IB closed.");
        connected = false;
    }
    
    @Override
    public void nextValidId(int orderId) {
        try {
            loggerComm.info("Puting nextValidId: " + orderId);
            nextIdQueue.put(orderId);
        } catch (Exception ex) {
            ex.printStackTrace();
            loggerComm.severe("Comm Error: nextValidId " + ex);
        }
        if (!connected) {
            connected = true;
            loggerComm.info("Connection to IB successful.");
            connectiongLatch.countDown();
            // TODO: nejakej latch?
        }
    }
    
    public synchronized int getNextOrderId() {
        if (nextOrderId == -1) {
            try {
                ibClientSocket.reqIds(1);
                Integer id = nextIdQueue.poll(5, TimeUnit.SECONDS);
                if (id == null) {
                    loggerComm.severe("Cannot get ID for order.");
                    return -1;
                }
                nextOrderId = id;
                return nextOrderId;
            } catch (InterruptedException ex) {
                logger.severe(ex.getMessage() + ex);
                return -1;
            }
        }
        return ++nextOrderId;
    }
    
    public synchronized boolean PlaceOrder(TradeOrder tradeOrder) {
        if (tradeOrder != null)
        return false;
        if (!connected) {
            loggerComm.severe("IB not connected. Cannot place order.");
            return false;
        }
        
        Contract contract = CreateContract(tradeOrder.tickerSymbol);
        
        Order ibOrder = new Order();
        if (tradeOrder.orderType == TradeOrder.OrderType.BUY) {
            ibOrder.m_action = "BUY";
        } else {
            ibOrder.m_action = "SELL";
        }
        
        ibOrder.m_orderId = getNextOrderId();
        
        if (orderStatusMap.containsKey(ibOrder.m_orderId)) {
            loggerComm.severe("Trying to use duplicate ID for order: " + tradeOrder.tickerSymbol + ", " + ibOrder.m_action);
            return false;
            //TODO: co s tim?
        }
        
        ibOrder.m_totalQuantity = tradeOrder.position;
        ibOrder.m_orderType = "MKT";
        ibOrder.m_tif = "DAY";
        
        ibClientSocket.placeOrder(ibOrder.m_orderId, contract, ibOrder);
        logger.info("Placing order - ID: " + ibOrder.m_orderId + ", Ticker :" + tradeOrder.tickerSymbol + ", " + ibOrder.m_action);
        loggerComm.info("Placing order - ID: " + ibOrder.m_orderId + ", " + tradeOrder.toString());
        
        synchronized(activeOrdersMap) {
            if (activeOrdersMap.isEmpty()) {
                loggerComm.finest("Creating new latch for ordersClosedWaitCountdownLatch");
                ordersClosedWaitCountdownLatch = new CountDownLatch(1);
            }

            OrderStatus orderStatus = new OrderStatus(tradeOrder, ibOrder.m_orderId);
            orderStatusMap.put(ibOrder.m_orderId, orderStatus);
            activeOrdersMap.put(ibOrder.m_orderId, orderStatus);
        }
        
        return true;
    }
    
    @Override
    public void orderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
        loggerComm.finer("OrderStatus(): orderId: " + orderId + " Status: " + status + " filled: " + filled + " remaining: " + remaining + " avgFillPrice: " + avgFillPrice + " permId: " + permId + " parentId: " + parentId + " lastFillePrice: " + lastFillPrice + " clientId: " + clientId + " whyHeld: " + whyHeld);
        
        OrderStatus orderStatus = orderStatusMap.get(orderId);

        if (orderStatus == null) {
            loggerComm.severe("Open Order with ID: " + orderId + " not found");
            return;
        }

        orderStatus.filled = filled;
        orderStatus.remaining = remaining;
        orderStatus.fillPrice = avgFillPrice;
        
        orderStatus.status = OrderStatus.getOrderStatus(status);

        if (orderStatus.status == OrderStatus.Status.FILLED) {
            orderStatus.timestampFilled = TradingTimer.GetNYTimeNow();
            loggerComm.fine("FILLED id:" + orderId + ", " + orderStatus.toString());

            synchronized (activeOrdersMap) {
                activeOrdersMap.remove(orderId);
                if ((ordersClosedWaitCountdownLatch != null) && activeOrdersMap.isEmpty()) {
                    loggerComm.finest("Releasing latch for ordersClosedWaitCountdownLatch");
                    ordersClosedWaitCountdownLatch.countDown();
                }
            }
        }
    }

    public List<Position> getAllPositions() {
        positionsList.clear();
        if (!connected) {
            loggerComm.severe("IB not connected. Cannot get positions.");
            return positionsList;
        }
        
        getPositionsCountdownLatch = new CountDownLatch(1);
        ibClientSocket.reqPositions();
        try {
            if (!getPositionsCountdownLatch.await(30, TimeUnit.SECONDS)) {
                loggerComm.severe("Cannot get positions from IB");
            }
        } catch (InterruptedException ex) {
            loggerComm.log(Level.SEVERE, "getAllPositions", ex);
        }
        return positionsList;
    }

    @Override
    public void position(String account, Contract contract, int pos, double avgCost) {
        positionsList.add(new Position(contract.m_symbol, avgCost, pos));
        loggerComm.fine("Position - Account: " + account + " Contract: " + contract.m_symbol + " size: " + pos + " avgCost: " + avgCost );
    }

    @Override
    public void positionEnd() {
        getPositionsCountdownLatch.countDown();
        loggerComm.fine("Position END()");
    }

    public boolean waitUntilOrdersClosed(int maxWaitSeconds) {
        try {
            Thread.sleep(100);  // For safety if orders are not yet issued

            if (ordersClosedWaitCountdownLatch == null) {
                return true;
            }

            return ordersClosedWaitCountdownLatch.await(maxWaitSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            loggerComm.log(Level.SEVERE, "waitUntilOrdersClosed", ex);
        }
        return false;
    }

    public void clearOrderMaps() {
        synchronized (activeOrdersMap) {
            activeOrdersMap.clear();
            orderStatusMap.clear();
            ordersClosedWaitCountdownLatch = null;
        }
    }
    
    public void RequestRealtimeData(String ticker) {
        Contract contract = CreateContract(ticker);
        contract.m_secType = "STK"; //Cannot be CFD for req data
        
        int orderId = getNextOrderId();
    
        ibClientSocket.reqMktData(orderId, contract, null, false, new Vector<TagValue>());
        realtimeData.CreateNew(ticker, orderId);
        
        try {
            Thread.sleep(20);   //max number of commands to IB is 50/s
        } catch (InterruptedException ex) {
        }
    }
    
    public void CancelAllRealtimeData() {
        for (Integer orderId : realtimeData.GetAllOrderIds()) {
            ibClientSocket.cancelMktData(orderId);
            try {
                Thread.sleep(20);   //max number of commands to IB is 50/s
            } catch (InterruptedException ex) {
            }
        }
        realtimeData.ClearMaps();
    }

    @Override
    public void tickPrice(int orderId, int field, double price, int canAutoExecute) {
        // Print out the current price.
        // field will provide the price type:
        // 1 = bid,  2 = ask, 4 = last
        // 6 = high, 7 = low, 9 = close
        //loggerComm.finest("tickPrice: " + orderId + "," + RealtimeDataIB.GetTickPriceFieldString(field) + "," + price);
        
        realtimeData.UpdateValue(orderId, field, price);
    }
    
    public double GetLastPrice(String ticker) {
        return realtimeData.GetLastPrice(ticker);
    }

    private Contract CreateContract(String ticker) {
        Contract contract = new Contract();
        contract.m_symbol = ticker;
        contract.m_exchange = "SMART";
        contract.m_secType = "CFD";
        contract.m_currency = "USD";

        if ((ticker == "MSFT")
                || (ticker == "CSCO")
                || (ticker == "INTC")) {
            contract.m_exchange = "BATS";
        }

        return contract;
    }
}
