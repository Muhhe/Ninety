package tradingapp;

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

public class MailSender {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static StringBuilder m_mailBody = new StringBuilder();
    private static StringBuilder m_mailBodyError = new StringBuilder();
    
    private static String[] m_attachmentsLog = {};
    private static String[] m_attachmentsError = {};
    
    private static String m_tradeLogSubject = "AOS Trade log";
    private static String m_checkSubject = "AOS Check";
    private static String m_errorSubject = "AOS Errors";

    protected MailSender() {
        // Exists only to defeat instantiation.
    }

    static public void AddLineToMail(String str) {
        m_mailBody.append(str + "\r\n");
    }

    static public void AddErrorLineToMail(String str) {
        m_mailBodyError.append(str + "\r\n");
    }
    
    static public void SetTradeLogAttachments(String[] attachments) {
        m_attachmentsLog = attachments;
    }
    
    static public void SetErrorAttachments(String[] attachments) {
        m_attachmentsError = attachments;
    }
    
    static public void SetTradeLogSubject(String subject) {
        m_tradeLogSubject = subject;
    }
    
    static public void SetCheckSubject(String subject) {
        m_checkSubject = subject;
    }
    
    static public void SetErrorSubject(String subject) {
        m_errorSubject = subject;
    }

    private static Properties SetupProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        return props;
    }

    private static Session SetupSession() {
        Properties props = SetupProperties();

        Session session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Settings.mailFrom, Settings.mailPassword);
            }
        });

        return session;
    }

    public static void SendTradingLog() {
        if (!GlobalConfig.sendMails) {
            return;
        }

        if (m_mailBody.length() == 0) {
            m_mailBody.append("No trades today!");
        }

        logger.fine("Sending trade log mail!");

        if (Send(m_tradeLogSubject, Settings.mailAddressTradeLog, m_mailBody.toString(), m_attachmentsLog)) {
            m_mailBody.setLength(0);
            logger.info("Trade mail sent!");
        } else {
            logger.info("Trade mail NOT sent!");
        }
    }

    public static void SendCheckResult() {
        if (!GlobalConfig.sendMails) {
            return;
        }

        logger.fine("Sending check mail!");

        if (Send(m_checkSubject, Settings.mailAddressCheck, m_mailBody.toString(), m_attachmentsError)) {
            m_mailBody.setLength(0);
            logger.info("Check mail sent!");
        } else {
            logger.info("Check mail NOT sent!");
        }
    }

    public static boolean SendErrors() {
        if (!GlobalConfig.sendMails) {
            return true;
        }

        if (m_mailBodyError.length() > 0) {
            logger.fine("Sending error mail!");

            if (Send(m_errorSubject, Settings.mailAddressError, m_mailBodyError.toString(), m_attachmentsError)) {
                m_mailBodyError.setLength(0);
                logger.info("Error mail sent!");
            } else {
                logger.info("Error mail NOT sent!");
            }
            return true;
        }

        return false;
    }

    private static boolean Send(String subject, String address, String mailBody, String[] attachments) {
        Session session = SetupSession();

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(Settings.mailFrom));
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
            
            for (String attachment : attachments) {
                AddAttachment(attachment, multipart);
            }

            // Send the complete message parts
            message.setContent(multipart);

            Transport.send(message);

        } catch (MessagingException e) {
            logger.severe("Failed to send mail: " + e);
            return false;
        }

        return true;
    }

    private static void AddAttachment(String filename, Multipart multipart) {
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
