/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.swing.JTextArea;

/**
 *
 * @author Muhe
 */
public class TextAreaLogHandler extends Handler {

    private final JTextArea m_textArea;
    private final int m_fromLevel;
    private final int m_toLevel;

    TextAreaLogHandler(JTextArea textArea, Level fromLevel, Level toLevel)
    {
        m_textArea = textArea;
        m_fromLevel = fromLevel.intValue();
        m_toLevel = toLevel.intValue();
    }
    
    @Override
    public void publish(LogRecord record) {
        int level = record.getLevel().intValue();
        if ((level < m_fromLevel) || (level > m_toLevel)) {
            return;
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        ZonedDateTime now = TradingTimer.GetNYTimeNow();
        m_textArea.append( formatter.format(now) );
        m_textArea.append(": ");
        m_textArea.append(record.getMessage());
        m_textArea.append("\r\n");
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
    
}
