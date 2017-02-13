package tradingapp;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Properties;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class MailSender {

    private static MailSender instance = null;

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private StringBuilder m_mailBody = new StringBuilder();
    private StringBuilder m_mailBodyError = new StringBuilder();

    protected MailSender() {
        // Exists only to defeat instantiation.
    }

    public static MailSender getInstance() {
        if (instance == null) {
            instance = new MailSender();
        }
        return instance;
    }

    static public void AddLineToMail(String str) {
        getInstance().m_mailBody.append(str + "\r\n");
    }

    static public void AddErrorLineToMail(String str) {
        getInstance().m_mailBodyError.append(str + "\r\n");
    }

    private Properties SetupProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        return props;
    }

    private Session SetupSession() {
        Properties props = SetupProperties();

        Session session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Settings.getInstance().mailFrom, Settings.getInstance().mailPassword);
            }
        });

        return session;
    }

    public void SendTradingLog() {

        if (m_mailBody.length() == 0) {
            m_mailBody.append("No trades today!");
        }

        logger.fine("Sending trade log mail!");
        Send("Trading log 90", Settings.getInstance().mailAddressTradeLog, m_mailBody.toString());
        m_mailBody.setLength(0);
        logger.info("Trade log mail sent!");
    }

    public void SendCheckResult() {
        logger.fine("Sending check mail!");
        Send("Check 90", Settings.getInstance().mailAddressCheck, m_mailBody.toString());
        m_mailBody.setLength(0);
        logger.info("Check mail sent!");
    }

    public boolean SendErrors() {
        if (m_mailBodyError.length() > 0) {
            logger.fine("Sending error mail!");
            Send("Errors!!!", Settings.getInstance().mailAddressError, m_mailBodyError.toString());
            m_mailBodyError.setLength(0);
            logger.info("Error mail sent!");
            return true;
        }
        return false;
    }

    private void Send(String subject, String address, String mailBody) {
        Session session = SetupSession();

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(Settings.getInstance().mailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(address));
            message.setSubject(subject);

            // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();

            // Now set the actual message
            messageBodyPart.setText(mailBody);

            // Create a multipar message
            Multipart multipart = new MimeMultipart();

            // Set text message part
            multipart.addBodyPart(messageBodyPart);

            String todayString = LocalDate.now().toString();
            String pathDir = "dataLog/" + todayString + "/Log.txt";
            AddAttachment(pathDir, multipart);

            pathDir = "dataLog/" + todayString + "/LogComm.txt";
            AddAttachment(pathDir, multipart);

            pathDir = "dataLog/" + todayString + "/LogDetailed.txt";
            AddAttachment(pathDir, multipart);

            pathDir = "Equity.csv";
            AddAttachment(pathDir, multipart);

            pathDir = "TradingStatus.xml";
            AddAttachment(pathDir, multipart);

            // Send the complete message parts
            message.setContent(multipart);

            Transport.send(message);

        } catch (MessagingException e) {
            logger.severe("Failed to send mail: " + e);
        }
    }

    public void AddAttachment(String filename, Multipart multipart) {
        try {
            MimeBodyPart messageBodyPart = new MimeBodyPart();

            DataSource source = new FileDataSource(filename);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(filename);
            multipart.addBodyPart(messageBodyPart);
        } catch (MessagingException ex) {
            logger.severe("Failed to add attachment to mail: " + ex);
        }
    }
}
