/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import communication.AccountSummary;
import communication.IBroker;
import communication.OrderStatus;
import communication.Position;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import communication.TradeOrder;
import data.CloseData;
import data.OHLCData;

/**
 *
 * @author Muhe
 */
public class BrokerNoIB implements IBroker {
    
    private boolean connected = false;
    private final Map<Integer, OrderStatus> orderStatusMap = new HashMap<>();
    private int orderId = 1;
    
    private final List<Position> positionsList = new ArrayList<>();

    @Override
    public boolean connect() {
        connected = true;
        return true;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void disconnect() {
        connected = false;
    }

    @Override
    public boolean PlaceOrder(TradeOrder tradeOrder) {
        return PlaceOrder(tradeOrder, SecType.STK);
    }

    @Override
    public synchronized boolean PlaceOrder(TradeOrder tradeOrder, SecType secType) {
        OrderStatus orderStatus = new OrderStatus(tradeOrder, orderId);
        orderStatus.status = OrderStatus.Status.FILLED;
        orderStatus.fillPrice = tradeOrder.expectedPrice;
        orderStatus.filled = tradeOrder.position;
        orderStatus.remaining = 0;
        orderStatusMap.put(orderId, orderStatus);
        
        orderId++;
        return true;
    }

    @Override
    public List<Position> getAllPositions(int wait) {
        return positionsList;
    }

    @Override
    public boolean waitUntilOrdersClosed(int maxWaitSeconds) {
        return true;
    }

    @Override
    public Map<Integer, OrderStatus> GetOrderStatuses() {
        return orderStatusMap;
    }

    @Override
    public void clearOrderMaps() {
        orderStatusMap.clear();
    }

    @Override
    public void SubscribeRealtimeData(String ticker) {
        
    }

    @Override
    public void SubscribeRealtimeData(String ticker, SecType secType) {
        
    }

    @Override
    public void CancelAllRealtimeData() {
        
    }

    @Override
    public double GetLastPrice(String ticker) {
        return 0;
    }

    @Override
    public void RequestAccountSummary() {
        
    }

    @Override
    public AccountSummary GetAccountSummary() {
        return new AccountSummary();
    }

    @Override
    public void RequestHistoricalData(String ticker, int count) {
    }

    @Override
    public void RequestHistoricalData(String[] tickers, int startInx, int endInx, int count) {
    }

    @Override
    public CloseData GetCloseData(String ticker) {
        return null;
    }

    @Override
    public OHLCData GetOHLCData(String ticker) {
        return null;
    }

    @Override
    public void CancelAllHistoricalData() {
    }
    
}
