/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import java.time.ZonedDateTime;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Element;
import tradingapp.TradeFormatter;
import tradingapp.TradingTimer;

/**
 *
 * @author Muhe
 */
public class StockPurchase {
    
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    public double priceForOne = 0;
    public int position = 0;
    public int portions = 0;
    public ZonedDateTime date = TradingTimer.GetNYTimeNow();
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Stock purchase - priceForOne ").append(TradeFormatter.toString(priceForOne));
        str.append(", position ").append(position);
        str.append(", portions ").append(portions);
        str.append(", date ").append(date).append("\r\n");
        
        return str.toString();
    }

    void AddToXml(Element heldElement) {
        Element purchaseElement = new Element("purchase");
        purchaseElement.setAttribute(new Attribute("priceForOne", TradeFormatter.toString(priceForOne)));
        purchaseElement.setAttribute(new Attribute("position", Integer.toString(position)));
        purchaseElement.setAttribute(new Attribute("portions", Integer.toString(portions)));
        purchaseElement.setAttribute(new Attribute("date", date.toString()));
        heldElement.addContent(purchaseElement);
    }

    void LoadFromXml(Element purchaseElement) {
        try {
            Attribute attribute = purchaseElement.getAttribute("priceForOne");
            priceForOne = attribute.getDoubleValue();
            
            attribute = purchaseElement.getAttribute("position");
            position = attribute.getIntValue();

            attribute = purchaseElement.getAttribute("portions");
            portions = attribute.getIntValue();
                    
            attribute = purchaseElement.getAttribute("date");
            date = ZonedDateTime.parse(attribute.getValue());
            
        } catch (DataConversionException ex) {
            logger.severe("Error in loading from XML: Failed to convert values in purchases.");
        }
    }
}
