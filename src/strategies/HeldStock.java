/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jdom2.Attribute;
import org.jdom2.Element;
import tradingapp.Formatter;

/**
 *
 * @author Muhe
 */
public class HeldStock {
    public List<StockPurchase> purchases = new ArrayList<StockPurchase>();
    public String tickerSymbol;
    
    int GetPosition() {
        int positions = 0;
        for (StockPurchase purchase : purchases) {
            positions += purchase.position;
        }
        return positions;
    }
    
    int GetPortions() {
        int portions = 0;
        for (StockPurchase purchase : purchases) {
            portions += purchase.portions;
        }
        return portions;
    }
    
    double GetTotalPricePaid() {
        double price = 0;
        for (StockPurchase purchase : purchases) {
            price += purchase.priceForOne * purchase.position;
        }
        return price;
    }

    double GetAvgPricePaid() {
        return GetTotalPricePaid() / GetPosition();
    }

    void AddToXml(Element rootElement) {
        Element heldElement = new Element("heldStock");
        heldElement.setAttribute(new Attribute("ticker", tickerSymbol));
        
        for (StockPurchase purchase : purchases) {
            purchase.AddToXml(heldElement);
        }
        rootElement.addContent(heldElement);
    }

    void LoadFromXml(Element heldElement) {
        Attribute attribute = heldElement.getAttribute("ticker");
        tickerSymbol = attribute.getValue();
        
        List<Element> purchaseElements = heldElement.getChildren();
        for (Element purchaseElement : purchaseElements) {
            StockPurchase purchase = new StockPurchase();
            purchase.LoadFromXml(purchaseElement);
            purchases.add(purchase);
        }
    }
   
    
    public String toStringLong() {
        StringBuilder str = new StringBuilder();
        str.append("Held stock: ").append(tickerSymbol);
        str.append(", purchases ").append(purchases.size()).append("\r\n");
        
        for (StockPurchase purchase : purchases) {
            str.append(purchase.toString());
        }

        return str.toString();
    }
    
    public double CalculateProfitIfSold(double actValue) {
        return (actValue * GetPosition()) - GetTotalPricePaid();
    }
    
    public double CalculatePercentProfitIfSold(double actValue) {
        double profit = CalculateProfitIfSold(actValue);
        double totalPrice = GetTotalPricePaid();

        return (profit / totalPrice) * 100;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(tickerSymbol);
        sb.append(", position: ").append(GetPosition());
        sb.append(", portions: ").append(GetPortions());
        sb.append(", avgPrice: ").append(Formatter.toString(GetAvgPricePaid())).append("$");

        return sb.toString();
    }
}
