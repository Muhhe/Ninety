/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communication;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.Order;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static tradingapp.MainWindow.COMM_LOGGER_NAME;
import tradingapp.TradeOrder;
import tradingapp.TradingTimer;

/**
 *
 * @author Muhe
 */
public class IBBroker extends BaseIBConnectionImpl {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private final static Logger loggerComm = Logger.getLogger( COMM_LOGGER_NAME );
    
    public Map<Integer, OrderStatus> orderStatusMap = new ConcurrentHashMap<>();
    public Map<Integer, OrderStatus> activeOrdersMap = new ConcurrentHashMap<>();
    
    public EClientSocket ibClientSocket = new EClientSocket(this);
    boolean connected = false;
    protected BlockingQueue<Integer> nextIdQueue = new LinkedBlockingQueue<>();
    protected CountDownLatch getPositionsCountdownLatch = null;
    protected CountDownLatch ordersClosedWaitCountdownLatch = null;
    protected List<Position> positionsList = new ArrayList<>();
    
    public void connect() {
        if( !connected ) {
            loggerComm.info("Connecting to IB.");
            ibClientSocket.eConnect(null, 4001, 1 );
            // TODO: wait?
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
            nextIdQueue.put(orderId);
        } catch (Exception ex) {
            ex.printStackTrace();
            loggerComm.severe("Comm Error: nextValidId " + ex);
        }
        if (!connected) {
            connected = true;
            loggerComm.info("Connection to IB successful.");
            // TODO: nejakej latch?
        }
    }
    
    public void PlaceOrder(TradeOrder tradeOrder) {
        if (!connected) {
            loggerComm.severe("IB not connected. Cannot place order.");
            return;
        }
        
        Contract contract = new Contract();
        contract.m_symbol = tradeOrder.tickerSymbol;
        contract.m_exchange = "SMART";
        contract.m_secType = "CFD";
        contract.m_currency = "USD";
        
        Order ibOrder = new Order();
        if (tradeOrder.orderType == TradeOrder.OrderType.BUY) {
            ibOrder.m_action = "BUY";
        } else {
            ibOrder.m_action = "SELL";
        }
        try {
            Integer id = nextIdQueue.poll(10, TimeUnit.SECONDS);
            if (id == null) {
                loggerComm.severe("Cannot get ID for order: " + tradeOrder.tickerSymbol + ", " + ibOrder.m_action);
                return;
            } 
            ibOrder.m_orderId = id;
        } catch (InterruptedException ex) {
            loggerComm.log(Level.SEVERE, null, ex);
        }
        
        ibOrder.m_totalQuantity = tradeOrder.position;
        ibOrder.m_orderType = "MKT";
        ibOrder.m_tif = "DAY";
        
        ibClientSocket.placeOrder(ibOrder.m_orderId, contract, ibOrder);
        logger.info("Placing order - ID: " + ibOrder.m_orderId + ", Ticker :" + tradeOrder.tickerSymbol + ", " + ibOrder.m_action);
        
        if (activeOrdersMap.isEmpty()) {
            loggerComm.finest("Creating new latch for ordersClosedWaitCountdownLatch");
            ordersClosedWaitCountdownLatch = new CountDownLatch(1);
        }
        
        OrderStatus orderStatus = new OrderStatus(tradeOrder, ibOrder.m_orderId);
        orderStatusMap.put(ibOrder.m_orderId, orderStatus);
        activeOrdersMap.put(ibOrder.m_orderId, orderStatus);
    }
    
    @Override
    public void orderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
        loggerComm.fine("OrderStatus(): orderId: " + orderId + " Status: " + status + " filled: " + filled + " remaining: " + remaining + " avgFillPrice: " + avgFillPrice + " permId: " + permId + " parentId: " + parentId + " lastFillePrice: " + lastFillPrice + " clientId: " + clientId + " whyHeld: " + whyHeld);
        
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
            
            activeOrdersMap.remove(orderId);
            if (activeOrdersMap.isEmpty()) {
                loggerComm.finest("Releasing latch for ordersClosedWaitCountdownLatch");
                ordersClosedWaitCountdownLatch.countDown();
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
        loggerComm.fine( "Position END()");
    }
    
    public boolean waitUntilOrdersClosed(int maxWaitSeconds) {
        try {
            return ordersClosedWaitCountdownLatch.await(maxWaitSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            loggerComm.log(Level.SEVERE, "waitUntilOrdersClosed", ex);
        }
        return false;
    }
}
