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
import tradingapp.TradeOrder;
import tradingapp.TradeTimer;

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
    public synchronized boolean PlaceOrder(TradeOrder tradeOrder) {
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
    public List<Position> getAllPositions() {
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
    public void RequestRealtimeData(String ticker) {
        
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
    
}
