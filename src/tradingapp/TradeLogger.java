/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.logging.Formatter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;
import static tradingapp.MainWindow.LOGGER_COMM_NAME;

/**
 *
 * @author Muhe
 */
public class TradeLogger {

    private static TradeLogger instance = null;

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private final static Logger loggerComm = Logger.getLogger(LOGGER_COMM_NAME);

    private JTextArea m_infoArea = null;
    private JTextArea m_fineArea = null;
    private JTextArea m_commArea = null;
    
    private FileHandler m_infoLogFile = null;
    private FileHandler m_commLogFile = null;
    private FileHandler m_detailedLogFile = null;
    
    LocalDate m_initDate = null;

    protected TradeLogger() {
        // Exists only to defeat instantiation.
    }

    public static TradeLogger getInstance() {
        if (instance == null) {
            instance = new TradeLogger();
        }
        return instance;
    }

    public void initializeTextAreas(JTextArea info, Level infoLvl, JTextArea fine, Level fineLvl, JTextArea comm, Level comLvl) {
        m_infoArea = info;
        m_fineArea = fine;
        m_commArea = comm;
        
        logger.setLevel(Level.ALL);

        TextAreaLogHandler textHandlerInfo = new TextAreaLogHandler(info, infoLvl, Level.SEVERE, true);
        logger.addHandler(textHandlerInfo);
        TextAreaLogHandler textHandlerFine = new TextAreaLogHandler(fine, fineLvl, Level.SEVERE, false);
        logger.addHandler(textHandlerFine);

        loggerComm.setLevel(Level.FINE);
        TextAreaLogHandler textHandlerComm = new TextAreaLogHandler(comm, comLvl, Level.SEVERE,false);
        loggerComm.addHandler(textHandlerComm);
        
        logger.fine("Logger areas initialized");
    }

    public void initializeFiles(LocalDate date) {
        
        logger.removeHandler(m_infoLogFile);
        logger.removeHandler(m_commLogFile);
        logger.removeHandler(m_detailedLogFile);
        
        String todayString = date.toString();
        String pathDir = FilePaths.dataLogDirectory + todayString + "/";
        File file = new File(pathDir);
                
        if (!file.exists() && !file.mkdirs()) {
            logger.warning("Failed to create directory for logs.");
        }
        try {
            Formatter formatterSimple = new LogFileFormatter("[%1$tH:%1$tM:%1$tS.%1$tL] %4$s: %5$s %6$s %n");
            Formatter formatterDetailed = new LogFileFormatter("[%1$tH:%1$tM:%1$tS.%1$tL] %4$s: %5$s [%2$s] %6$s %n");
            
            m_infoLogFile = new FileHandler(pathDir + "Log.txt", true);
            m_infoLogFile.setFormatter(formatterSimple);
            logger.addHandler(m_infoLogFile);
            m_infoLogFile.setLevel(Level.INFO);

            m_commLogFile = new FileHandler(pathDir + "LogComm.txt", true);
            m_commLogFile.setFormatter(formatterSimple);
            loggerComm.addHandler(m_commLogFile);
            m_infoLogFile.setLevel(Level.FINE);
            
            m_detailedLogFile = new FileHandler(pathDir + "LogDetailed.txt", true);
            m_detailedLogFile.setFormatter(formatterDetailed);
            m_detailedLogFile.setLevel(Level.ALL);
            logger.addHandler(m_detailedLogFile);
            loggerComm.addHandler(m_detailedLogFile);

        } catch (IOException e) {
            logger.warning("Failed to create file loggers.");
            return;
        }
        m_initDate = date;
        logger.fine("Logger files initialized for day " + date.toString());
    }
    
    public void clearLogs() {
        closeFiles();
        
        m_infoArea.setText("");
        m_fineArea.setText("");
        m_commArea.setText("");
    }
    
    public void closeFiles() {
        if (m_initDate == null) {
            return;
        }
        m_initDate = null;
        
        m_infoLogFile.close();
        m_commLogFile.close();
        m_detailedLogFile.close();
    }
    
    public boolean isInitedFiles(LocalDate date) {
        return (m_initDate != null) && (m_initDate.equals(date));
    }
    
    public boolean isInitedFiles() {
        return (m_initDate != null);
    }
}
