/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import communication.BrokerIB;
import communication.OrderStatus;
import tradingapp.TradeOrder;

/**
 *
 * @author Muhe
 */
public class BrokerIBReadOnly extends BrokerIB {
    
    public BrokerIBReadOnly(int port, int clientId) {
        super(port, clientId);
    }

    @Override
    public synchronized boolean PlaceOrder(TradeOrder tradeOrder) {
        int orderId = getNextOrderId();
        OrderStatus orderStatus = new OrderStatus(tradeOrder, orderId);
        orderStatus.status = OrderStatus.Status.FILLED;
        orderStatus.fillPrice = tradeOrder.expectedPrice;
        orderStatus.filled = tradeOrder.position;
        orderStatus.remaining = 0;
        orderStatusMap.put(orderId, orderStatus);
        
        return true;
    }

    @Override
    public boolean waitUntilOrdersClosed(int maxWaitSeconds) {
        return true;
    }
    
}
