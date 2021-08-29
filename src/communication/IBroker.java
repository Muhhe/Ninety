/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communication;

import data.CloseData;
import data.OHLCData;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Muhe
 */
public interface IBroker {  
    public enum SecType {
        STK,
        CFD,
        IND
    }
    
    public boolean connect();
    
    public boolean isConnected();
    
    public void disconnect();
    
    public boolean PlaceOrder(TradeOrder tradeOrder);
    
    public boolean PlaceOrder(TradeOrder tradeOrder, SecType secType);
    
    public List<Position> getAllPositions(int wait);

    public boolean waitUntilOrdersClosed(int maxWaitSeconds);
    
    public Map<Integer, OrderStatus> GetOrderStatuses();
    
    public void clearOrderMaps();
    
    public void SubscribeRealtimeData(String ticker);
    
    public void SubscribeRealtimeData(String ticker, SecType secType);
    
    public void CancelAllRealtimeData();
    
    public double GetLastPrice(String ticker);
    
    public void RequestHistoricalData(String ticker, int count);
    
    public void RequestHistoricalData(String[] tickers, int startInx, int endInx, int count);
    
    public void CancelAllHistoricalData();
    
    public CloseData GetCloseData(String ticker);
    
    public OHLCData GetOHLCData(String ticker);

    public void RequestAccountSummary();
    
    public AccountSummary GetAccountSummary();
}
