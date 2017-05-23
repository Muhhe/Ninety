/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyVXVMT;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class VXVMTStatus {
    
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    //VXVMTSignal yesterdaySignal = new VXVMTSignal();
    
    public VXVMTSignal.Type heldType = VXVMTSignal.Type.None;
    public int heldPosition = 0;
    public double capital = 0;
    public double fees = 0;
    
    
    public double GetEquity(double valueXIV, double valueVXX) {
        double value = 0;
        if (heldType == VXVMTSignal.Type.VXX) {
            value = valueVXX;
        } else {
            value = valueXIV;
        }
        
        return capital + heldPosition * value;
    }

    public void LoadTradingStatus() {

        try {
            File inputFile = new File(FilePaths.tradingStatusPathFileInput);
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);

            Element rootElement = document.getRootElement();
            
            Element moneyElement = rootElement.getChild("money");
            
            Attribute attribute = moneyElement.getAttribute("freeCapital");
            capital = attribute.getDoubleValue();
            
            attribute = moneyElement.getAttribute("fees");
            fees = attribute.getDoubleValue();
            
            /*Element yesterdayElement = rootElement.getChild("yesterdaySignal");
            
            attribute = yesterdayElement.getAttribute("type");
            String type = attribute.getValue();
            yesterdaySignal.type = VXVMTSignal.typeFromString(type);
            
            attribute = yesterdayElement.getAttribute("exposure");
            yesterdaySignal.exposure = attribute.getDoubleValue();*/
            
            
            Element heldElement = rootElement.getChild("held");
            attribute = heldElement.getAttribute("type");
            String type = attribute.getValue();
            heldType = VXVMTSignal.typeFromString(type);
            
            attribute = heldElement.getAttribute("position");
            heldPosition = attribute.getIntValue();
            
        } catch (JDOMException e) {
            logger.severe("Error in loading from XML: JDOMException.\r\n" + e);
        } catch (IOException ioe) {
            logger.severe("Error in loading from XML: IOException.\r\n" + ioe);
        }
    }
    
    public void SaveTradingStatus() {
        try {
            Element rootElement = new Element("status");
            Document doc = new Document(rootElement);
            
            Element moneyElement = new Element("money");
            moneyElement.setAttribute("freeCapital", TradeFormatter.toString(capital));
            moneyElement.setAttribute("fees", TradeFormatter.toString(fees));
            rootElement.addContent(moneyElement);
            
            /*Element yesterdayElement = new Element("yesterdaySignal");
            yesterdayElement.setAttribute("type", yesterdaySignal.typeToString());
            yesterdayElement.setAttribute("exposure", TradeFormatter.toString(yesterdaySignal.exposure));
            rootElement.addContent(yesterdayElement);*/
            
            Element heldElement = new Element("held");
            heldElement.setAttribute("type", heldType.toString());
            heldElement.setAttribute("position", Integer.toString(heldPosition));
            rootElement.addContent(heldElement);

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
    
    public void UpdateEquityFile(double valueXIV, double valueVXX) {
        Writer writer = null;
        try {
            File equityFile = new File(FilePaths.equityPathFile);
            equityFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(equityFile, true), "UTF-8"));
            String line = TradeTimer.GetLocalDateNow().toString() + "," + GetEquity(valueXIV, valueVXX) + "\r\n";
            writer.append(line);
            
            logger.fine("Updated equity file with value " + GetEquity(valueXIV, valueVXX));
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
