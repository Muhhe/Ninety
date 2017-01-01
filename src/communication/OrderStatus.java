/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communication;

import java.time.ZonedDateTime;
import tradingapp.TradeOrder;
import tradingapp.TradingTimer;

/**
 *
 * @author Muhe
 */
public class OrderStatus {
    public enum Status { NEW, PARTIAL_FILL, PENDING_CANCEL, REJECTED, FILLED, CANCELED, REPLACED, UNKNOWN };
    
    public TradeOrder order;
    
    public Status status;
    public int orderId;
    public int filled;
    public int remaining;
    public double fillPrice;
    //public String tickerSymbol;
    public ZonedDateTime timestampIssued;
    public ZonedDateTime timestampFilled;

    public OrderStatus(TradeOrder order, int orderId) {
        this.order = order;
        this.status = Status.NEW;
        this.orderId = orderId;
        this.filled = 0;
        this.remaining = order.position;
        this.fillPrice = 0;
        //this.tickerSymbol = order.tickerSymbol;
        this.timestampIssued = TradingTimer.GetNYTimeNow();
        this.timestampFilled = TradingTimer.GetNYTimeNow();
    }
    
    public static OrderStatus.Status getOrderStatus(String status) {
        if ("PendingSubmit".equalsIgnoreCase(status)) {
            return OrderStatus.Status.NEW;
        } else if ("PendingCancel".equalsIgnoreCase(status)) {
            return OrderStatus.Status.PENDING_CANCEL;
        } else if ("PreSubmitted".equalsIgnoreCase(status)) {
            return OrderStatus.Status.NEW;
        } else if ("Submitted".equalsIgnoreCase(status)) {
            return OrderStatus.Status.NEW;
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            return OrderStatus.Status.CANCELED;
        } else if ("Filled".equalsIgnoreCase(status)) {
            return OrderStatus.Status.FILLED;
        } else if ("Inactive".equalsIgnoreCase(status)) {
            return OrderStatus.Status.CANCELED;
        } else {
            return OrderStatus.Status.UNKNOWN;
        }
    }
}