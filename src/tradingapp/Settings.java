/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author Muhe
 */

public class Settings {    
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    public static int port = 0;
    public static int clientId = 0;
    
    public static double investCash = 0;
    public static double leverage = 0;
    
    public static String mailAddressTradeLog = null;
    public static String mailAddressCheck = null;
    public static String mailAddressError = null;
    public static String mailFrom = null;
    public static String mailPassword = null;
    
    protected Settings() {
        // Exists only to defeat instantiation.
    }
    
    public static void ReadSettings() {
        try {
            File inputFile = new File(FilePaths.settingsPathFile);
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);

            Element rootElement = document.getRootElement();
            Attribute attribute;
            
            // connection
            Element connectionElement = rootElement.getChild("connection");
            
            attribute = connectionElement.getAttribute("port");
            port = attribute.getIntValue();
            
            attribute = connectionElement.getAttribute("clientId");
            clientId = attribute.getIntValue();
            
            // money
            Element moneyElement = rootElement.getChild("money");
            
            attribute = moneyElement.getAttribute("investCash");
            investCash = attribute.getDoubleValue();
            
            attribute = moneyElement.getAttribute("leverage");
            leverage = attribute.getDoubleValue();
            
            // mail
            Element mailElement = rootElement.getChild("mail");
            mailAddressTradeLog = mailElement.getAttribute("addressTradeLog").getValue();
            mailAddressCheck = mailElement.getAttribute("addressCheck").getValue();
            mailAddressError = mailElement.getAttribute("addressError").getValue();
            mailFrom = mailElement.getAttribute("from").getValue();
            mailPassword = mailElement.getAttribute("password").getValue();
            
            logger.fine("Updated Settings");
            logger.fine("Loaded Connection - port: " + port + ", clientId: " + clientId);
            logger.fine("Loaded money - investCash: " + investCash + ", leverage: " + leverage);
            logger.fine("Loaded mail - mailAddressTradeLog: " + mailAddressTradeLog + ", mailAddressCheck: " + mailAddressCheck + 
                    ", mailAddressError: " + mailAddressError + ", mailFrom: " + mailFrom + ", mailPassword: " + mailPassword);
            
        } catch (JDOMException e) {
            logger.severe("Error in loading from XML: JDOMException.\r\n" + e);
        } catch (IOException ioe) {
            logger.severe("Error in loading from XML: IOException.\r\n" + ioe);
        }
    }

}
