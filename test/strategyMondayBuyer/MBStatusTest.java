/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyMondayBuyer;

import java.io.File;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import tradingapp.FilePaths;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class MBStatusTest {
    @Test
    public void testSaveLoad() {
        
        MBStatus status = new MBStatus();
        status.equity = 999;
        status.fees = 12;
        
        MBHeldTicker ticker1 = new MBHeldTicker();
        ticker1.ticker = "AAPL";
        ticker1.date = LocalDate.parse("2007-12-03");
        ticker1.price = 123;
        ticker1.position = 10;
        
        MBHeldTicker ticker2 = new MBHeldTicker();
        ticker2.ticker = "GOOG";
        ticker2.date = LocalDate.parse("2009-11-03");
        ticker2.price = 321;
        ticker2.position = 5;
        
        MBHeldTicker ticker3 = new MBHeldTicker();
        ticker3.ticker = "FB";
        ticker3.date = LocalDate.parse("2011-11-03");
        ticker3.price = 222;
        ticker3.position = 8;
        
        status.heldTickers.put(ticker1.ticker, ticker1);
        status.heldTickers.put(ticker2.ticker, ticker2);
        status.heldTickers.put(ticker3.ticker, ticker3);
        
        status.recentlySold.put("APPL", LocalDate.parse("2011-11-03"));
        status.recentlySold.put("JPM", LocalDate.parse("2012-12-02"));
        status.recentlySold.put("C", LocalDate.parse("2013-05-23"));
        
        FilePaths.tradingStatusPathFileInput = "TestTradingStatus.xml";
        FilePaths.tradingStatusPathFileOutput = "TestTradingStatus.xml";
        
        status.SaveTradingStatus();
        
        MBStatus newStatus = new MBStatus();
        newStatus.LoadTradingStatus();
        
        assertEquals(status.equity, newStatus.equity, 0.01);
        assertEquals(status.fees, newStatus.fees, 0.01);
        assertEquals(status.heldTickers.size(), newStatus.heldTickers.size());
        
        for (MBHeldTicker tick : status.heldTickers.values()) {
            assertEquals(tick.ticker, newStatus.heldTickers.get(tick.ticker).ticker);
            assertEquals(tick.position, newStatus.heldTickers.get(tick.ticker).position);
            assertEquals(tick.price, newStatus.heldTickers.get(tick.ticker).price, 0.01);
            assertEquals(tick.date, newStatus.heldTickers.get(tick.ticker).date);
        }
        
        assertEquals(status.recentlySold.size(), newStatus.recentlySold.size());
        for (Map.Entry<String, LocalDate> entry : status.recentlySold.entrySet()) {
            String ticker = entry.getKey();
            LocalDate date = entry.getValue();
            
            assertEquals(date, newStatus.recentlySold.get(ticker));
        }
        
        TradeTimer.SetToday(LocalDate.parse("2013-05-25"));
        status.removeOldRecSold();
        
        assertEquals(status.recentlySold.size(), 1);
        for (Map.Entry<String, LocalDate> entry : status.recentlySold.entrySet()) {
            String ticker = entry.getKey();
            LocalDate date = entry.getValue();
            
            assertEquals(date, LocalDate.parse("2013-05-23"));
        }
        
        try {
            File file = new File(FilePaths.tradingStatusPathFileInput);
            file.delete();
        } catch (Exception e) {
        }
    }
}
