/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import tradingapp.Stock;

/**
 *
 * @author Muhe
 */
public class HeldStock extends Stock {
    public int boughtPortions = 0;
    public List<StockPurchase> purchases = new ArrayList<StockPurchase>();
    public double actValue = 0;

    int GetPosition() {
        int positions = 0;
        for (StockPurchase purchase : purchases) {
            positions += purchase.position;
        }
        return positions;
    }
    
    public void LoadFromString(List<String> allLines) {
        int lineCounter = 0;
        int purchCount = 0;
        for (Iterator<String> iterator = allLines.iterator(); iterator.hasNext();) {
            String next = iterator.next();
            
            if (next.isEmpty()) {
                continue;
            }
            
            String[] strs = next.split(";");
            
            if ( lineCounter == 0) {
                tickerSymbol = strs[1];
                actValue = Double.parseDouble(strs[3]);
                purchCount = Integer.parseInt(strs[5]);
            }
            else
            {
                StockPurchase pur = new StockPurchase();
                pur.LoadFromStrings(strs);
                purchases.add(pur);
                purchCount--;
            }

            lineCounter++;
            
            iterator.remove();
            
            if (purchCount == 0) {
                break;
            }
        }
        
        boughtPortions = 0;
        for (StockPurchase purchase : purchases) {
            boughtPortions += purchase.portions;
        }
    }

    double GetAvgPrice() {
        double avgPrice = 0;
        for (StockPurchase purchase : purchases) {
            avgPrice += purchase.priceForOne * purchase.position;
        }
        return avgPrice / GetPosition();
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Held stock;").append(tickerSymbol);
        str.append("; actValue;").append(actValue);
        str.append("; purchases;").append(purchases.size()).append("\r\n");
        
        for (StockPurchase purchase : purchases) {
            str.append(purchase.toString());
        }
        
        return str.toString();
    }
}
