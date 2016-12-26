/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

    public void SaveHeldPositionsToFile() {
        /*try {
            List<String> lines = new ArrayList<>();

            lines.add("Number of held stock:" + heldStocks.size());

            for (HeldStock heldStock : heldStocks.values()) {
                lines.add(heldStock.toString());
            }

            Path file = Paths.get("HeldPositions.txt");
            Files.write(file, lines, Charset.forName("UTF-8"));

        } catch (IOException ex) {
            logger.severe("Failed to create file: " + ex.getMessage());
        }*/

        try {
            //root element
            Element rootElement = new Element("HeldPositions");
            Document doc = new Document(rootElement);
            rootElement.setAttribute("heldStocks", Integer.toString(heldStocks.size()));

            for (HeldStock held : heldStocks.values()) {
                held.AddToXml(rootElement);
            }

            XMLOutputter xmlOutput = new XMLOutputter();

            //Path file = Paths.get("HeldPositions.txt");
            File yourFile = new File("HeldPositions.xml");
            yourFile.createNewFile(); // if file already exists will do nothing 
            FileOutputStream oFile = new FileOutputStream(yourFile, false);

            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(doc, oFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ReadHeldPositions() {

        heldStocks.clear();

        /*try {
            Path file = Paths.get("HeldPositions.txt");
            List<String> allLines = Files.readAllLines(file);

            Iterator<String> iterator = allLines.iterator();
            String next = iterator.next();
            String[] split = next.split(":");
            int count = Integer.parseInt(split[1]);

            iterator.remove();

            for (int i = 0; i < count; i++) {
                HeldStock held = new HeldStock();
                held.LoadFromString(allLines);
                heldStocks.put(held.tickerSymbol, held);
            }

        } catch (IOException ex) {
            logger.severe("Failed to create file: " + ex.getMessage());
        }*/
        try {
            File inputFile = new File("HeldPositions.xml");

            SAXBuilder saxBuilder = new SAXBuilder();

            Document document = saxBuilder.build(inputFile);

            //System.out.println("Root element :"
            //        + document.getRootElement().getName());

            Element rootElement = document.getRootElement();

            List<Element> heldStocksElements = rootElement.getChildren();
            //System.out.println("----------------------------");

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
        //List<String> lines = new ArrayList<>();

        logger.info("Status report: number of held stock: " + heldStocks.size());
        logger.info("Held portions: " + GetBoughtPortions() + "/20");

        for (HeldStock heldStock : heldStocks.values()) {
            logger.info(heldStock.toString());
        }
    }
}
