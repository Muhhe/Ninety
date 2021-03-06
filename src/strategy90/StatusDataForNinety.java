/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategy90;

import communication.OrderStatus;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import static java.lang.Double.max;
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
import communication.TradeOrder;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class StatusDataForNinety {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    static public final int PORTIONS_NUM = 20;

    public Map<String, HeldStock> heldStocks = new HashMap<>();
    public double moneyToInvest = 40000.0;
    public double currentCash = 20000.0;
    public double currentFees = 0;
    
    public static double GetOrderFee(int position) {
        return max(1.0, position * 0.005);
    }
    
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
            CountInOrderFee(order.filled);
            logger.finer("New stock added: " + held.toString());
            
            MailSender.AddLineToMail("OPEN new - " + held.tickerSymbol +
                    ", price: " + TradeFormatter.toString(order.fillPrice) + 
                    "$, position: " + order.filled);
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
            CountInOrderFee(order.filled);
            
            logger.info("Stock removed - profit: " + TradeFormatter.toString(profit) + " = " + TradeFormatter.toString(profitPercent) + "%, " + order.toString());
            
            MailSender.AddLineToMail("SELL - " + held.tickerSymbol + 
                    ", profit: " + TradeFormatter.toString(profit) + "$ = " + TradeFormatter.toString(profitPercent) + 
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
            CountInOrderFee(order.filled);
            logger.fine("More stock bought - " + held.toString());
            
            MailSender.AddLineToMail("SCALE up - " + held.tickerSymbol +
                    ", price: " + TradeFormatter.toString(order.fillPrice) + 
                    "$, position: " + order.filled +
                    ", portion: " + newPortions);
        }
    }

    public void SaveTradingStatus() {
        try {
            Element rootElement = new Element("status");
            Document doc = new Document(rootElement);
            
            Element moneyElement = new Element("money");
            moneyElement.setAttribute("currentCash", TradeFormatter.toString(currentCash));
            moneyElement.setAttribute("currentFees", TradeFormatter.toString(currentFees));
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

    public void LoadTradingStatus() {

        heldStocks.clear();
        try {
            File inputFile = new File(FilePaths.tradingStatusPathFileInput);
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);

            Element rootElement = document.getRootElement();
            
            Element moneyElement = rootElement.getChild("money");
            
            Attribute attribute = moneyElement.getAttribute("currentCash");
            currentCash = attribute.getDoubleValue();
            
            attribute = moneyElement.getAttribute("currentFees");
            currentFees = attribute.getDoubleValue();
            
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

        if (boughtPortions > StatusDataForNinety.PORTIONS_NUM) {
            logger.severe("Bought portions more than 20!!!");
        }

        return boughtPortions;
    }

    public double GetOnePortionValue() {
        return moneyToInvest / StatusDataForNinety.PORTIONS_NUM;
    }

    public void PrintStatus() {
        logger.fine("Status report - currentCash: " + currentCash + " number of held stock: " + heldStocks.size());
        logger.fine("Held portions: " + GetBoughtPortions() + "/20");

        for (HeldStock heldStock : heldStocks.values()) {
            logger.fine(heldStock.toString());
        }
    }

    private void CountInProfit(double profit) {
        currentCash += profit;
    }

    private void CountInOrderFee(int position) {
        double fee = GetOrderFee(position);
        currentCash -= fee;
        currentFees += fee;
    }
    
    public void UpdateEquityFile() {
        Writer writer = null;
        try {
            File equityFile = new File(FilePaths.equityPathFile);
            equityFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(equityFile, true), "UTF-8"));
            String line = TradeTimer.GetLocalDateNow().toString() 
                    + "," + currentCash 
                    + "," + Settings.investCash
                    + "," + (currentCash - Settings.investCash)
                    + "\r\n";
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
            writer.append(TradeFormatter.toString(profit) + ",");
            writer.append(TradeFormatter.toString(profitPercent) + ",");
            writer.append(held.GetPortions() + ",");
            writer.append(TradeFormatter.toString(held.GetTotalPricePaid()) + ",");
            double fees = held.GetTotalFeesPaid();
            writer.append(TradeFormatter.toString(fees) + "\r\n");
            
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
            writer.append("Profit: " + TradeFormatter.toString(profit) + "$ = " + TradeFormatter.toString(profitPercent) + "%, ");
            writer.append("Portions: " + held.GetPortions() + ", ");
            writer.append("Fees: " + TradeFormatter.toString(held.GetTotalFeesPaid()) + "$\r\n\r\n");
            
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
