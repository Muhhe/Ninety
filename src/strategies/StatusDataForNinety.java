/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import communication.OrderStatus;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jdom2.Attribute;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import tradingapp.FilePaths;
import tradingapp.TradeFormatter;
import tradingapp.MailSender;
import tradingapp.Settings;
import tradingapp.TradeOrder;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class StatusDataForNinety {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public Map<String, HeldStock> heldStocks = new HashMap<>();
    public double moneyToInvest = 40000.0;
    public double currentCash = 20000.0;
    
    public void UpdateHeldByOrderStatus(OrderStatus order) {
        HeldStock held = heldStocks.get(order.order.tickerSymbol);
        
        if (order.filled == 0) {
            return;
        }
        
        // Add new stock
        if (held == null) {
            if (order.order.orderType == TradeOrder.OrderType.SELL) {
                logger.severe("Trying to sell not held stock: " + order.order.tickerSymbol);
                return;
            }

            held = new HeldStock();
            held.tickerSymbol = order.order.tickerSymbol;

            StockPurchase purchase = new StockPurchase();
            purchase.date = TradeTimer.GetLocalDateNow();
            purchase.portions = 1;
            purchase.position = order.filled;
            purchase.priceForOne = order.fillPrice;

            held.purchases.add(purchase);

            heldStocks.put(held.tickerSymbol, held);
            CountInOrderFee();
            logger.finer("New stock added: " + held.toString());
            MailSender.AddLineToMail("OPEN new - " + held.toString());
            return;
        }

        if (order.order.orderType == TradeOrder.OrderType.SELL) {
            double profit = held.CalculateProfitIfSold(order.fillPrice);
            double profitPercent = held.CalculatePercentProfitIfSold(order.fillPrice);
            
            if (order.filled != held.GetPosition()) {
                logger.severe("Not all position has been sold for: " + held.tickerSymbol);
                // TODO: nejak poresit
                return;
            }

            heldStocks.remove(held.tickerSymbol);
            CountInProfit(profit);
            CountInOrderFee();
            
            logger.info("Stock removed - profit: " + TradeFormatter.toString(profit) + " = " + TradeFormatter.toString(profitPercent) + "%, " + order.toString());
            
            MailSender.AddLineToMail("SELL - " + held.tickerSymbol + 
                    ", profit: " + TradeFormatter.toString(profit) + " = " + TradeFormatter.toString(profitPercent) + 
                    "%, sellPrice: " + TradeFormatter.toString(order.fillPrice) + 
                    ", portions: " + held.GetPortions());
            
            UpdateTradeLogs(order, held);
        } else {
            
            int newPortions = Ninety.GetNewPortionsToBuy(held.GetPortions());
            if (newPortions == 0) {
                logger.severe("Bought stock '" + held.tickerSymbol + "' has somehow " + held.GetPortions() + " bought portions!!!");
                //TODO: dafuq?
            }
            
            StockPurchase purchase = new StockPurchase();
            purchase.date = TradeTimer.GetLocalDateNow();
            purchase.portions = newPortions;
            purchase.position = order.filled;
            purchase.priceForOne = order.fillPrice;

            held.purchases.add(purchase);
            CountInOrderFee();
            logger.fine("More stock bought - " + held.toString());
            MailSender.AddLineToMail("SCALE up - " + held.toString());
        }
    }

    public void SaveHeldPositionsToXML() {
        try {
            Element rootElement = new Element("status");
            Document doc = new Document(rootElement);
            
            Element moneyElement = new Element("money");
            moneyElement.setAttribute("currentCash", TradeFormatter.toString(currentCash));
            rootElement.addContent(moneyElement);
            
            Element heldPosElement = new Element("heldPositions");
            heldPosElement.setAttribute("heldStocks", Integer.toString(heldStocks.size()));
            rootElement.addContent(heldPosElement);

            for (HeldStock held : heldStocks.values()) {
                held.AddToXml(heldPosElement);
            }

            XMLOutputter xmlOutput = new XMLOutputter();

            File statusFile = new File(FilePaths.tradingStatusPathFileOutput);
            statusFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(statusFile, false);

            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(doc, oFile);

        } catch (IOException e) {
            logger.severe("Failed to save held positions to XML file - " + e.toString());
        }
    }

    public void ReadHeldPositions() {

        heldStocks.clear();
        try {
            File inputFile = new File(FilePaths.tradingStatusPathFileInput);
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);

            Element rootElement = document.getRootElement();
            
            Element moneyElement = rootElement.getChild("money");
            
            Attribute attribute = moneyElement.getAttribute("currentCash");
            currentCash = attribute.getDoubleValue();
            
            List<Element> heldStocksElements = rootElement.getChild("heldPositions").getChildren();

            for (Element heldElement : heldStocksElements) {
                HeldStock held = new HeldStock();
                held.LoadFromXml(heldElement);

                heldStocks.put(held.tickerSymbol, held);
            }
        } catch (JDOMException e) {
            logger.severe("Error in loading from XML: JDOMException.\r\n" + e);
        } catch (IOException ioe) {
            logger.severe("Error in loading from XML: IOException.\r\n" + ioe);
        }
    }
    
    public void UpdateCashSettings() {

        heldStocks.clear();
        moneyToInvest = Settings.investCash * Settings.leverage;
        
        logger.fine("Updated cash settings - moneyToInvest: " + moneyToInvest);
    }

    public int GetBoughtPortions() {
        int boughtPortions = 0;
        for (HeldStock heldStock : heldStocks.values()) {
            boughtPortions += heldStock.GetPortions();
        }

        if (boughtPortions > 20) {
            logger.severe("Bought portions more than 20!!!");
        }

        return boughtPortions;
    }

    public double GetOnePortionValue() {
        return moneyToInvest / 20.0;
    }

    public void PrintStatus() {
        logger.fine("Status report - currentCash: " + currentCash + " number of held stock: " + heldStocks.size());
        logger.fine("Held portions: " + GetBoughtPortions() + "/20");

        for (HeldStock heldStock : heldStocks.values()) {
            logger.fine(heldStock.toString());
        }
    }

    void CountInProfit(double profit) {
        currentCash += profit;
    }

    void CountInOrderFee() {
        currentCash -= 1.0;
    }
    
    void UpdateEquityFile() {
        Writer writer = null;
        try {
            File equityFile = new File(FilePaths.equityPathFile);
            equityFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(equityFile, true), "UTF-8"));
            String line = TradeTimer.GetLocalDateNow().toString() + "," + currentCash + "\r\n";
            writer.append(line);
            
            logger.fine("Updated equity file with value " + currentCash);
        } catch (FileNotFoundException ex) {
            logger.severe("Cannot find equity file: " + ex);
        } catch (IOException ex) {
            logger.severe("Error updating equity file: " + ex);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                logger.severe("Error updating equity file: " + ex);
            }
        }
    }
    
    static public void UpdateTradeLogs(OrderStatus order, HeldStock held) {
        if (order.order.orderType != TradeOrder.OrderType.SELL) {
            logger.warning("Trying to add BUY order to trade log.");
            return;
        }
        
        if (held.purchases.isEmpty()) {
            logger.warning("Trying to add empty held stock to trade log.");
            return;
        }
        
        UpdateCloseTradeLogFile(order, held);
        UpdateDetailedTradeLogFile(order, held);
    }
    
    static public void UpdateCloseTradeLogFile(OrderStatus order, HeldStock held) {
        
        Writer writer = null;
        try {
            File equityFile = new File(FilePaths.tradeLogPathFile);
            equityFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(equityFile, true), "UTF-8"));
            
            double profit = held.CalculateProfitIfSold(order.fillPrice);
            double profitPercent = held.CalculatePercentProfitIfSold(order.fillPrice);
            
            writer.append(TradeTimer.GetLocalDateNow().toString() + ",");
            writer.append(held.purchases.get(0).date + ",");
            writer.append(held.tickerSymbol + ",");
            writer.append(profit + ",");
            writer.append(profitPercent + ",");
            writer.append(held.GetPortions() + ",");
            writer.append(held.GetTotalPricePaid() + ",");
            writer.append((held.purchases.size() + 1) + "\r\n");
            
            logger.finer("Updated close trade log file.");
        } catch (FileNotFoundException ex) {
            logger.severe("Cannot find close trade log file: " + ex);
        } catch (IOException ex) {
            logger.severe("Error updating close trade log file: " + ex);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                logger.severe("Error updating close trade log file: " + ex);
            }
        }
    }
    
    static public void UpdateDetailedTradeLogFile(OrderStatus order, HeldStock held) {
                
        Writer writer = null;
        try {
            File equityFile = new File(FilePaths.tradeLogDetailedPathFile);
            equityFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(equityFile, true), "UTF-8"));
            
            for (StockPurchase purchase : held.purchases) {
                writer.append(purchase.date.toString() + ", ");
                if (purchase.portions == 1) {
                    writer.append("OPEN, ");
                } else {
                    writer.append("SCALE, ");
                }
                writer.append("Ticker: " + held.tickerSymbol + ", ");
                writer.append("Price: " + TradeFormatter.toString(purchase.priceForOne) + ", ");
                writer.append("Position: " + purchase.position + "\r\n");
            }
            
            double profit = held.CalculateProfitIfSold(order.fillPrice);
            double profitPercent = held.CalculatePercentProfitIfSold(order.fillPrice);
            
            writer.append(TradeTimer.GetLocalDateNow().toString() + ", ");
            writer.append("CLOSE, ");
            writer.append("Ticker: " + held.tickerSymbol + ", ");
            writer.append("Price: " + TradeFormatter.toString(order.fillPrice) + ", ");
            writer.append("Position: " + held.GetPosition()+ ", ");
            writer.append("Profit: " + TradeFormatter.toString(profit) + "$, = " + TradeFormatter.toString(profitPercent) + "%, ");
            writer.append("Portions: " + held.GetPortions() + ",");
            writer.append("Fees: " + (held.purchases.size() + 1) + "$\r\n\r\n");
            
            logger.finer("Updated detailed trade log file.");
        } catch (FileNotFoundException ex) {
            logger.severe("Cannot find detailed trade log file: " + ex);
        } catch (IOException ex) {
            logger.severe("Error updating detailed trade log file: " + ex);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                logger.severe("Error updating detailed trade log file: " + ex);
            }
        }
    }
    
    
}
