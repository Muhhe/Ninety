/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

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

/**
 *
 * @author Muhe
 */
public class StatusDataForNinety {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public Map<String, HeldStock> heldStocks = new HashMap<>();
    public double moneyToInvest = 40000;
    public double investCash = 40000;
    public double currentCash = 40000;

    public void SaveHeldPositionsToFile() {
        try {
            Element rootElement = new Element("status");
            Document doc = new Document(rootElement);
            
            Element moneyElement = new Element("money");
            moneyElement.setAttribute("currentCash", Double.toString(currentCash));
            rootElement.addContent(moneyElement);
            
            Element heldPosElement = new Element("heldPositions");
            heldPosElement.setAttribute("heldStocks", Integer.toString(heldStocks.size()));
            rootElement.addContent(heldPosElement);

            for (HeldStock held : heldStocks.values()) {
                held.AddToXml(heldPosElement);
            }

            XMLOutputter xmlOutput = new XMLOutputter();

            File statusFile = new File("TradingStatus.xml");
            statusFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(statusFile, false);

            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(doc, oFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ReadHeldPositions() {

        heldStocks.clear();
        try {
            File inputFile = new File("TradingStatus.xml");
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
            e.printStackTrace();
            logger.severe("Error in loading from XML: JDOMException.\r\n" + e);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.severe("Error in loading from XML: IOException.\r\n" + ioe);
        }
    }
    
    public void ReadSettings() {

        heldStocks.clear();
        try {
            File inputFile = new File("Settings.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);

            Element rootElement = document.getRootElement();
            
            Element moneyElement = rootElement.getChild("money");
            
            Attribute attribute = moneyElement.getAttribute("investCash");
            investCash = attribute.getDoubleValue();
            
            attribute = moneyElement.getAttribute("leverage");
            double leverage = attribute.getDoubleValue();
            
            moneyToInvest = investCash * leverage;
            
            logger.fine("Loaded settings. InvestCash: " + investCash + ", leverage: " + leverage);
        } catch (JDOMException e) {
            e.printStackTrace();
            logger.severe("Error in loading from XML: JDOMException.\r\n" + e);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.severe("Error in loading from XML: IOException.\r\n" + ioe);
        }
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
        return moneyToInvest / 20;
    }

    public void PrintStatus() {
        logger.info("Status report - currentCash: " + currentCash + " number of held stock: " + heldStocks.size());
        logger.fine("Held portions: " + GetBoughtPortions() + "/20");

        for (HeldStock heldStock : heldStocks.values()) {
            logger.fine(heldStock.toStringLong());
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
            File equityFile = new File("Equity.csv");
            equityFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(equityFile, true), "UTF-8"));
            String line = LocalDate.now().toString() + ":" + currentCash + "\r\n";
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
}
