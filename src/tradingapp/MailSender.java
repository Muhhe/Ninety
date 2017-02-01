/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

    private static MailSender instance = null;

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    public StringBuilder m_mailBody = new StringBuilder();

    protected MailSender() {
        // Exists only to defeat instantiation.
    }

    public static MailSender getInstance() {
        if (instance == null) {
            instance = new MailSender();
        }
        return instance;
    }

    public void AddLineToMail(String str) {
        m_mailBody.append(str + "\r\n");
    }

    public void Send() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("svoboded@gmail.com", "svob4ded");
            }
        });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("svoboded@gmail.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("svoboda77@volny.cz,dedinam@gmail.com"));
            message.setSubject("Trading log 90");
            
            // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();

            // Now set the actual message
            messageBodyPart.setText(m_mailBody.toString());

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

            m_mailBody.setLength(0);
            logger.info("Mail sent!");

        } catch (MessagingException e) {
            logger.severe("Failed to send mail: " + e);
            throw new RuntimeException(e);
        }
    }
    
    public void AddAttachment(String filename, Multipart multipart) {
        try {
            // Part two is attachment
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
}
