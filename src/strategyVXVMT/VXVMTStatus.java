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
import tradingapp.Settings;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class VXVMTStatus {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public VXVMTSignal.Type heldType = VXVMTSignal.Type.None;
    public int heldPosition = 0;
    public double freeCapital = 0;
    public double closingEquity = 0;
    public double fees = 0;
    public double avgPrice = 0;

    public double GetEquity(double valueXIV, double valueVXX, double valueGLD) {
        double value = 0;
                
        switch (heldType) {
            case VXX:
                value = valueVXX;
                break;
            case XIV:
                value = valueXIV;
                break;
            case GLD:
                value = valueGLD;
                break;
            case None:
                value = 0;
                break;
            default:
                logger.warning("Invalid signal type used: " + heldType);
                break;
        }

        return freeCapital + heldPosition * value;
    }

    public void LoadTradingStatus() {

        try {
            File inputFile = new File(FilePaths.tradingStatusPathFileInput);
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);

            Element rootElement = document.getRootElement();

            Element moneyElement = rootElement.getChild("money");

            Attribute attribute = moneyElement.getAttribute("freeCapital");
            freeCapital = attribute.getDoubleValue();

            attribute = moneyElement.getAttribute("closingEquity");
            closingEquity = attribute.getDoubleValue();

            attribute = moneyElement.getAttribute("fees");
            fees = attribute.getDoubleValue();

            Element heldElement = rootElement.getChild("held");
            attribute = heldElement.getAttribute("type");
            String type = attribute.getValue();
            heldType = VXVMTSignal.typeFromString(type);

            attribute = heldElement.getAttribute("position");
            heldPosition = attribute.getIntValue();
            attribute = heldElement.getAttribute("avgPrice");
            avgPrice = attribute.getDoubleValue();

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
            moneyElement.setAttribute("freeCapital", TradeFormatter.toString(freeCapital));
            moneyElement.setAttribute("closingEquity", TradeFormatter.toString(closingEquity));
            moneyElement.setAttribute("fees", TradeFormatter.toString(fees));
            rootElement.addContent(moneyElement);

            Element heldElement = new Element("held");
            heldElement.setAttribute("type", heldType.toString());
            heldElement.setAttribute("position", Integer.toString(heldPosition));
            heldElement.setAttribute("avgPrice", TradeFormatter.toString(avgPrice));
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

    public void UpdateEquity(double valueXIV, double valueVXX, double valueGLD, String signal) {

        closingEquity = GetEquity(valueXIV, valueVXX, valueGLD);

        Writer writer = null;
        try {
            File equityFile = new File(FilePaths.equityPathFile);
            equityFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(equityFile, true), "UTF-8"));
            String line = TradeTimer.GetLocalDateNow().toString()
                    + "," + closingEquity
                    + "," + Settings.investCash
                    + "," + (closingEquity - Settings.investCash)
                    + "," + signal
                    + "\r\n";
            writer.append(line);

            logger.fine("Updated equity file with value " + GetEquity(valueXIV, valueVXX, valueGLD));
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

    public void PrintStatus() {
        logger.fine("Free cash: " + freeCapital);
        logger.fine("Held " + heldType + ", position: " + heldPosition);
    }

    public void PrintStatus(double XIV, double VXX, double GLD) {
        logger.fine("Status report - currentEquity: " + GetEquity(XIV, VXX, GLD));
        PrintStatus();
    }
}
