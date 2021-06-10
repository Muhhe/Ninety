/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyMondayBuyer;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Element;
import tradingapp.TradeFormatter;

/**
 *
 * @author Muhe
 */
public class MBHeldTicker {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public String ticker = "";
    public double price = 0;
    public int position = 0;
    public LocalDate date = LocalDate.MIN;

    public void AddToXml(Element rootElement) {
        Element heldElement = new Element("heldStock");
        heldElement.setAttribute(new Attribute("ticker", ticker));
        heldElement.setAttribute(new Attribute("price", TradeFormatter.toString(price)));
        heldElement.setAttribute(new Attribute("position", Integer.toString(position)));
        heldElement.setAttribute(new Attribute("date", date.toString()));
        rootElement.addContent(heldElement);
    }

    public void LoadFromXml(Element heldElement) {
        Attribute attribute = heldElement.getAttribute("ticker");
        ticker = attribute.getValue();
        try {
            attribute = heldElement.getAttribute("price");
            price = attribute.getDoubleValue();

            attribute = heldElement.getAttribute("position");
            position = attribute.getIntValue();

            attribute = heldElement.getAttribute("date");
            date = LocalDate.parse(attribute.getValue());

        } catch (DataConversionException ex) {
            logger.severe("Error in loading from XML: Failed to convert values in purchases. " + ex.getMessage());
        }
    }

    public double CalculateProfitIfSold(double actValue) {
        return (actValue - price) * position;
    }

    public double CalculatePercentProfitIfSold(double actValue) {
        double profit = CalculateProfitIfSold(actValue);
        double totalPrice = price * position;

        return (profit / totalPrice) * 100;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ticker);
        sb.append(", position: ").append(position);
        sb.append(", price: ").append(TradeFormatter.toString(price)).append("$");

        return sb.toString();
    }
}
