/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communication;

import java.util.List;
import java.util.Map;
import tradingapp.TradeOrder;

/**
 *
 * @author Muhe
 */
public interface IBroker {  

    public boolean connect();
    
    public boolean isConnected();
    
    public void disconnect();
    
    public boolean PlaceOrder(TradeOrder tradeOrder);
    
    public List<Position> getAllPositions();

    public boolean waitUntilOrdersClosed(int maxWaitSeconds);
    
    public Map<Integer, OrderStatus> GetOrderStatuses();
    
    public void clearOrderMaps();
    
    public void RequestRealtimeData(String ticker);
    
    public void CancelAllRealtimeData();
    
    public double GetLastPrice(String ticker);

    public void RequestAccountSummary();
    
    public AccountSummary GetAccountSummary();
}
