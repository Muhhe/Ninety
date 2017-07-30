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
import tradingapp.TradeTimer;
import static tradingapp.MainWindow90.LOGGER_COMM_NAME;

/**
 *
 * @author Muhe
 */
public class BrokerIB extends BaseIBConnectionImpl implements IBroker {

    protected final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    protected final static Logger loggerComm = Logger.getLogger(LOGGER_COMM_NAME);

    protected int port;
    protected int clientId;
    protected SecType secType;

    protected final Map<Integer, OrderStatus> orderStatusMap = new ConcurrentHashMap<>();
    protected final Map<Integer, OrderStatus> activeOrdersMap = new HashMap<>();

    protected final RealtimeDataIB realtimeData = new RealtimeDataIB();

    protected EClientSocket ibClientSocket = new EClientSocket(this);
    protected boolean connected = false;
    protected BlockingQueue<Integer> nextIdQueue = new LinkedBlockingQueue<>();
    protected CountDownLatch getPositionsCountdownLatch = null;
    protected CountDownLatch ordersClosedWaitCountdownLatch = null;
    protected CountDownLatch connectionLatch = null;
    protected List<Position> positionsList = new ArrayList<>();
    protected int nextOrderId = -1;

    protected AccountSummary accountSummary = new AccountSummary();
    protected boolean accountSummarySubscribed = false;

    public BrokerIB(int port, int clientId, SecType secType) {
        this.port = port;
        this.clientId = clientId;
        this.secType = secType;
    }

    @Override
    public synchronized boolean connect() {
        if (!connected) {
            logger.fine("Connecting to IB.");
            ibClientSocket.eConnect(null, port, clientId);
            connectionLatch = new CountDownLatch(1);
            try {
                if (!connectionLatch.await(5, TimeUnit.SECONDS) || !connected) {
                    logger.severe("Cannot connect to IB");
                    return false;
                }
            } catch (InterruptedException ex) {
                loggerComm.log(Level.SEVERE, null, ex);
            }

            RequestAccountSummary();
        }

        return true;
    }

    @Override
    public synchronized boolean isConnected() {
        return connected;
    }

    @Override
    public synchronized void disconnect() {
        if (connected) {
            loggerComm.info("Disconnecting from IB.");
            ibClientSocket.eDisconnect();
            connected = false;
            accountSummarySubscribed = false;
        }
    }

    @Override
    public void connectionClosed() {
        loggerComm.info("Connection to IB closed.");
        connected = false;
        accountSummarySubscribed = false;
        clearOrderMaps();
        realtimeData.ClearMaps();
        connectionLatch.countDown();    // if connection fails
    }

    @Override
    public void nextValidId(int orderId) {
        try {
            loggerComm.info("Puting nextValidId: " + orderId);
            nextIdQueue.put(orderId);
        } catch (InterruptedException ex) {
            loggerComm.severe("Comm Error: nextValidId " + ex);
        }
        if (!connected) {
            connected = true;
            logger.fine("Connection to IB successful.");
            connectionLatch.countDown();
        }
    }

    protected synchronized int getNextOrderId() {
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

    @Override
    public synchronized boolean PlaceOrder(TradeOrder tradeOrder) {
        if (!connected) {
            logger.severe("IB not connected. Cannot place order.");
            return false;
        }

        Contract contract = CreateOrderContract(tradeOrder.tickerSymbol);

        Order ibOrder = new Order();
        if (tradeOrder.orderType == TradeOrder.OrderType.BUY) {
            ibOrder.m_action = "BUY";
        } else {
            ibOrder.m_action = "SELL";
        }

        ibOrder.m_orderId = getNextOrderId();

        if (orderStatusMap.containsKey(ibOrder.m_orderId)) {
            logger.severe("Trying to use duplicate ID for order: " + tradeOrder.tickerSymbol + ", " + ibOrder.m_action);
            return false;
            //TODO: co s tim? omezenej while?
        }

        ibOrder.m_totalQuantity = tradeOrder.position;
        ibOrder.m_orderType = "MKT";
        ibOrder.m_tif = "DAY";

        logger.fine("Placing order - ID: " + ibOrder.m_orderId + ", Ticker: " + tradeOrder.tickerSymbol + ", " + ibOrder.m_action);

        synchronized (activeOrdersMap) {
            if (activeOrdersMap.isEmpty()) {
                loggerComm.finest("Creating new latch for ordersClosedWaitCountdownLatch");
                ordersClosedWaitCountdownLatch = new CountDownLatch(1);
            }

            OrderStatus orderStatus = new OrderStatus(tradeOrder, ibOrder.m_orderId);
            orderStatusMap.put(ibOrder.m_orderId, orderStatus);
            activeOrdersMap.put(ibOrder.m_orderId, orderStatus);

            loggerComm.info("Placing order - ID: " + ibOrder.m_orderId + ", " + tradeOrder.toString());
            ibClientSocket.placeOrder(ibOrder.m_orderId, contract, ibOrder);
        }

        return true;
    }

    @Override
    public void orderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
        loggerComm.finer("OrderStatus(): orderId: " + orderId + " Status: " + status + " filled: " + filled + " remaining: " + remaining + " avgFillPrice: " + avgFillPrice + " permId: " + permId + " parentId: " + parentId + " lastFillePrice: " + lastFillPrice + " clientId: " + clientId + " whyHeld: " + whyHeld);

        OrderStatus orderStatus = orderStatusMap.get(orderId);

        if (orderStatus == null) {
            if (OrderStatus.getOrderStatus(status) == OrderStatus.Status.FILLED) {
                loggerComm.info("Open Order with ID: " + orderId + " not found");
            } else {
                loggerComm.severe("Open Order with ID: " + orderId + " not found");
            }
            return;
        }
        
        if (orderStatus.status == OrderStatus.Status.FILLED) {
            return;
        }

        orderStatus.status = OrderStatus.getOrderStatus(status);

        orderStatus.filled = filled;
        orderStatus.remaining = remaining;
        orderStatus.fillPrice = avgFillPrice;

        if (orderStatus.status == OrderStatus.Status.FILLED) {
            orderStatus.timestampFilled = TradeTimer.GetNYTimeNow();
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

    @Override
    public List<Position> getAllPositions() {
        positionsList.clear();
        if (!connected) {
            logger.severe("IB not connected. Cannot get positions.");
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
        loggerComm.fine("Position - Account: " + account + " Contract: " + contract.m_symbol + " size: " + pos + " avgCost: " + avgCost);
    }

    @Override
    public void positionEnd() {
        getPositionsCountdownLatch.countDown();
        loggerComm.fine("Position END()");
    }

    @Override
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

    @Override
    public Map<Integer, OrderStatus> GetOrderStatuses() {
        return orderStatusMap;
    }

    @Override
    public void clearOrderMaps() {
        synchronized (activeOrdersMap) {
            activeOrdersMap.clear();
            orderStatusMap.clear();
            ordersClosedWaitCountdownLatch = null;
        }
    }

    @Override
    public void SubscribeRealtimeData(String ticker) {
        SubscribeRealtimeData(ticker, SecType.STK);
    }

    @Override
    public void SubscribeRealtimeData(String ticker, SecType secType) {
        if (!connected) {
            logger.severe("IB not connected. Cannot RequestRealtimeData.");
            return;
        }

        Contract contract = CreateDataContract(ticker, secType);

        int orderId = getNextOrderId();

        realtimeData.CreateNew(ticker, orderId);
        ibClientSocket.reqMktData(orderId, contract, null, false, new Vector<TagValue>());

        try {
            Thread.sleep(20);   //max number of commands to IB is 50/s
        } catch (InterruptedException ex) {
        }
    }

    @Override
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

    @Override
    public double GetLastPrice(String ticker) {
        return realtimeData.GetLastPrice(ticker);
    }

    protected Contract CreateOrderContract(String ticker) {
        Contract contract = new Contract();
        contract.m_symbol = ticker;
        contract.m_exchange = "SMART";
        contract.m_secType = secType.toString();
        contract.m_currency = "USD";

        return contract;
    }

    protected Contract CreateDataContract(String ticker, SecType secType) {
        Contract contract = new Contract();
        contract.m_symbol = ticker;
        contract.m_localSymbol = ticker;
        contract.m_exchange = "SMART";
        contract.m_secType = secType.toString();
        contract.m_currency = "USD";

        if (ticker.equals("MSFT")
                || ticker.equals("CSCO")
                || ticker.equals("INTC")) {
            contract.m_exchange = "BATS";
        }
        
        if (ticker.equals("VXV")
                || ticker.equals("VXMT")) {
            contract.m_exchange = "CBOE";
        }

        return contract;
    }

    @Override
    public void RequestAccountSummary() {
        if (!accountSummarySubscribed) {
            ibClientSocket.reqAccountSummary(getNextOrderId(), "All", "TotalCashValue,NetLiquidation,SettledCash,AccruedCash,BuyingPower,"
                    + "EquityWithLoanValue,RegTEquity,RegTMargin,AvailableFunds,Leverage");
            accountSummarySubscribed = true;
        }
    }

    @Override
    public AccountSummary GetAccountSummary() {
        return accountSummary;
    }

    @Override
    public void accountSummary(int reqId, String account, String tag, String value, String currency) {
        loggerComm.fine("accountSummary - reqId: " + reqId + ", account: " + account + ", tag: " + tag + ", value: " + value + ", currency: " + currency);

        if (tag.compareTo("AvailableFunds") == 0) {
            accountSummary.availableFunds = Double.valueOf(value);
        }
        if (tag.compareTo("NetLiquidation") == 0) {
            accountSummary.netLiquidation = Double.valueOf(value);
        }
        if (tag.compareTo("TotalCashValue") == 0) {
            accountSummary.totalCashValue = Double.valueOf(value);
        }
        if (tag.compareTo("BuyingPower") == 0) {
            accountSummary.buyingPower = Double.valueOf(value);
        }
    }
}
