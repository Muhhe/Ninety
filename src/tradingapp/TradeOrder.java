/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

/**
 *
 * @author Muhe
 */
public class TradeOrder {
    public enum OrderType {BUY, SELL};
    
    public OrderType orderType = OrderType.SELL;
    public int position = 0;
    public String tickerSymbol;
    public double expectedPrice = 0;

    @Override
    public String toString() {
        String str = new String();
        
        str += "Order '" + orderType + "' '" + tickerSymbol + "' position: " + position;
        return str;
    }
    
    
}
