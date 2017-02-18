/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import java.io.PrintWriter;
import java.io.StringWriter;
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
    private final boolean m_mailErrors;

    public TextAreaLogHandler(JTextArea textArea, Level fromLevel, Level toLevel, boolean mailErrors)
    {
        m_textArea = textArea;
        m_fromLevel = fromLevel.intValue();
        m_toLevel = toLevel.intValue();
        
        m_mailErrors = mailErrors;
    }
    
    @Override
    public void publish(LogRecord record) {
        StringBuilder msg = new StringBuilder();
        try {

            int level = record.getLevel().intValue();
            if ((level < m_fromLevel) || (level > m_toLevel)) {
                return;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
            ZonedDateTime now = TradingTimer.GetNYTimeNow();
            msg.append(formatter.format(now));
            msg.append(": ");

            if (level > Level.INFO.intValue()) {
                msg.append("[" + record.getLevel().getName() + "] ");
            }

            msg.append(record.getMessage());

            String throwable = "";
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println();
                record.getThrown().printStackTrace(pw);
                pw.close();
                throwable = sw.toString();

                msg.append(throwable);
            }

            msg.append("\r\n");

            m_textArea.append(msg.toString());

            if ((level > Level.INFO.intValue()) && m_mailErrors) {
                MailSender.AddErrorLineToMail(msg.toString());
            }

            if (record.getThrown() != null) {
                MailSender.getInstance().SendErrors();
            }

        } catch (Exception e) { // Exception thrown out of here could cause an infinite loop
            msg.append("Exception in logging " + e.getMessage() + "\r\n");

        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
    
}
