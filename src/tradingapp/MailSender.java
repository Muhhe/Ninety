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
    
    private String m_mailAddressTradeLog = new String();
    private String m_mailAddressCheck = new String();
    private String m_mailAddressError = new String();
    
    private String m_mailFrom = new String();
    private String m_mailPassword = new String();

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
                return new PasswordAuthentication(m_mailFrom, m_mailPassword);
            }
        });

        return session;
    }

    public void SendTradingLog() {
        
        if (m_mailBody.length() == 0) {
            m_mailBody.append("No trades today!");
        }
        
        Send("Trading log 90", m_mailAddressTradeLog, m_mailBody.toString());
        m_mailBody.setLength(0);
    }

    public void SendCheckResult() {
        Send("Check 90", m_mailAddressCheck, m_mailBody.toString());
        m_mailBody.setLength(0);
    }

    public boolean SendErrors() {
        if (m_mailBodyError.length() > 0) {
            Send("Errors!!!", m_mailAddressError, m_mailBodyError.toString());
            m_mailBodyError.setLength(0);
            return true;
        }
        return false;
    }

    private void Send(String subject, String address, String mailBody) {
        Session session = SetupSession();

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(m_mailFrom));
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
            logger.info("Mail sent!");

        } catch (MessagingException e) {
            logger.severe("Failed to send mail: " + e);
            throw new RuntimeException(e);
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
            throw new RuntimeException(ex);
        }
    }
    
    
    public void ReadSettings() {
        try {
            File inputFile = new File("Settings.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);

            Element rootElement = document.getRootElement();
            
            Element moneyElement = rootElement.getChild("mail");
            
            m_mailAddressTradeLog = moneyElement.getAttribute("addressTradeLog").getValue();
            m_mailAddressCheck = moneyElement.getAttribute("addressCheck").getValue();
            m_mailAddressError = moneyElement.getAttribute("addressError").getValue();
            m_mailFrom = moneyElement.getAttribute("from").getValue();
            m_mailPassword = moneyElement.getAttribute("password").getValue();
            
            logger.fine("Loaded mail settings. Address trade log: " + m_mailAddressTradeLog + " check: " + m_mailAddressCheck + " error: " + m_mailAddressError + " from: " + m_mailFrom);
        } catch (JDOMException e) {
            e.printStackTrace();
            logger.severe("Error in loading from XML: JDOMException.\r\n" + e);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.severe("Error in loading from XML: IOException.\r\n" + ioe);
        }
    }
}
