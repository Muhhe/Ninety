/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyMondayBuyer;

import communication.OrderStatus;
import communication.TradeOrder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import static java.lang.Double.max;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import tradingapp.FilePaths;
import tradingapp.GlobalConfig;
import tradingapp.MailSender;
import tradingapp.Settings;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class MBStatus {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public double equity = 0;
    public double fees = 0;

    static public final int PORTIONS_NUM = 8;

    public Map<String, MBHeldTicker> heldTickers = new HashMap<String, MBHeldTicker>();

    public Map<String, LocalDate> recentlySold = new HashMap<String, LocalDate>();

    public void UpdateHeldByOrderStatus(OrderStatus order) {
        MBHeldTicker held = heldTickers.get(order.order.tickerSymbol);

        if (order.filled == 0) {
            return;
        }

        // Add new stock
        if (held == null) {
            if (order.order.orderType == TradeOrder.OrderType.SELL) {
                logger.severe("Trying to sell not held stock: " + order.order.tickerSymbol);
                return;
            }

            held = new MBHeldTicker();
            held.ticker = order.order.tickerSymbol;

            held.date = TradeTimer.GetLocalDateNow();
            held.position = order.filled;
            held.price = order.fillPrice;

            heldTickers.put(held.ticker, held);
            CountInOrderFee(order.filled);
            logger.finer("New stock added: " + held.toString());

            MailSender.AddLineToMail("OPEN new - " + held.ticker
                    + ", price: " + TradeFormatter.toString(order.fillPrice)
                    + "$, position: " + order.filled);
            return;
        }

        if (order.order.orderType == TradeOrder.OrderType.SELL) {
            double profit = held.CalculateProfitIfSold(order.fillPrice);
            double profitPercent = held.CalculatePercentProfitIfSold(order.fillPrice);

            if (order.filled != held.position) {
                logger.severe("Not all position has been sold for: " + held.ticker);
                held.position -= order.filled;
                profit *= order.filled / held.position;
                profitPercent *= order.filled / held.position;
            } else {
                heldTickers.remove(held.ticker);
            }

            equity += profit;
            CountInOrderFee(order.filled);

            logger.info("Stock removed - profit: " + TradeFormatter.toString(profit) + " = " + TradeFormatter.toString(profitPercent) + "%, " + order.toString());

            MailSender.AddLineToMail("SELL - " + held.ticker
                    + ", profit: " + TradeFormatter.toString(profit) + "$ = " + TradeFormatter.toString(profitPercent)
                    + "%, sellPrice: " + TradeFormatter.toString(order.fillPrice));

            UpdateTradeLogs(order, held);
        }
    }

    public static double GetOrderFee(int position) {
        return max(1.0, position * 0.005);
    }

    private void CountInOrderFee(int position) {
        double fee = GetOrderFee(position);
        equity -= fee;
        fees += fee;
    }

    public void SaveTradingStatus() {
        try {
            Element rootElement = new Element("status");
            Document doc = new Document(rootElement);

            Element moneyElement = new Element("money");
            moneyElement.setAttribute("equity", TradeFormatter.toString(equity));
            moneyElement.setAttribute("currentFees", TradeFormatter.toString(fees));
            rootElement.addContent(moneyElement);

            Element heldPosElement = new Element("heldPositions");
            heldPosElement.setAttribute("heldStocks", Integer.toString(heldTickers.size()));
            rootElement.addContent(heldPosElement);

            for (MBHeldTicker held : heldTickers.values()) {
                held.AddToXml(heldPosElement);
            }

            Element recentlySoldElement = new Element("recentlySold");
            recentlySoldElement.setAttribute("num", Integer.toString(recentlySold.size()));
            rootElement.addContent(recentlySoldElement);

            for (Map.Entry<String, LocalDate> entry : recentlySold.entrySet()) {
                String ticker = entry.getKey();
                LocalDate date = entry.getValue();
                Element recSoldElement = new Element("recSold");
                recSoldElement.setAttribute(new Attribute("ticker", ticker));
                recSoldElement.setAttribute(new Attribute("date", date.toString()));
                recentlySoldElement.addContent(recSoldElement);
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

        heldTickers.clear();
        try {
            File inputFile = new File(FilePaths.tradingStatusPathFileInput);
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);

            Element rootElement = document.getRootElement();

            Element moneyElement = rootElement.getChild("money");

            Attribute attribute = moneyElement.getAttribute("equity");
            equity = attribute.getDoubleValue();

            attribute = moneyElement.getAttribute("currentFees");
            fees = attribute.getDoubleValue();

            List<Element> heldStocksElements = rootElement.getChild("heldPositions").getChildren();

            for (Element heldElement : heldStocksElements) {
                MBHeldTicker held = new MBHeldTicker();
                held.LoadFromXml(heldElement);

                heldTickers.put(held.ticker, held);
            }

            List<Element> recentlySoldElement = rootElement.getChild("recentlySold").getChildren();

            for (Element recSoldElement : recentlySoldElement) {
                Attribute att = recSoldElement.getAttribute("ticker");
                String ticker = att.getValue();
                att = recSoldElement.getAttribute("date");
                LocalDate date = LocalDate.parse(att.getValue());

                recentlySold.put(ticker, date);
            }
        } catch (JDOMException e) {
            logger.severe("Error in loading from XML: JDOMException.\r\n" + e);
        } catch (IOException ioe) {
            logger.severe("Error in loading from XML: IOException.\r\n" + ioe);
        }
    }

    public void PrintStatus() {
        logger.fine("Status report - equity: " + equity + " number of held stock: " + heldTickers.size());

        for (MBHeldTicker heldStock : heldTickers.values()) {
            logger.fine(heldStock.toString());
        }
    }
    
    public void removeOldRecSold() {
        for (Iterator<Map.Entry<String, LocalDate>> iterator = recentlySold.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, LocalDate> next = iterator.next();
            
            LocalDate soldDate = next.getValue();
            if (ChronoUnit.DAYS.between(soldDate, TradeTimer.GetLocalDateNow()) > 40) {
                iterator.remove();
            }
        }
    }

    public void UpdateEquityFile() {
        Writer writer = null;
        try {
            File equityFile = new File(FilePaths.equityPathFile);
            equityFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(equityFile, true), "UTF-8"));
            String line = TradeTimer.GetLocalDateNow().toString()
                    + "," + equity
                    + "," + Settings.investCash
                    + "," + (equity - Settings.investCash)
                    + "\r\n";
            writer.append(line);

            logger.fine("Updated equity file with value " + equity);
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

    static public void UpdateTradeLogs(OrderStatus order, MBHeldTicker held) {
        if (GlobalConfig.isBacktest) {
            return;
        }

        if (order.order.orderType != TradeOrder.OrderType.SELL) {
            logger.warning("Trying to add BUY order to trade log.");
            return;
        }

        if (held.position == 0) {
            logger.warning("Trying to add empty held stock to trade log.");
            return;
        }

        UpdateCloseTradeLogFile(order, held);
        //UpdateDetailedTradeLogFile(order, held);
    }

    static public void UpdateCloseTradeLogFile(OrderStatus order, MBHeldTicker held) {

        Writer writer = null;
        try {
            File equityFile = new File(FilePaths.tradeLogPathFile);
            equityFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(equityFile, true), "UTF-8"));

            double profit = held.CalculateProfitIfSold(order.fillPrice);
            double profitPercent = held.CalculatePercentProfitIfSold(order.fillPrice);

            writer.append(TradeTimer.GetLocalDateNow().toString() + ",");
            writer.append(held.date + ",");
            writer.append(held.ticker + ",");
            writer.append(TradeFormatter.toString(profit) + ",");
            writer.append(TradeFormatter.toString(profitPercent) + ",");
            writer.append(TradeFormatter.toString(held.price) + ",");
            writer.append(TradeFormatter.toString(held.position) + ",");
            double fees = GetOrderFee(held.position);
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
}
