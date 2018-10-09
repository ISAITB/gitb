package utils;

import managers.AttachmentType;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.util.Properties;

public class EmailUtil {

    private static Authenticator authenticator;
    private static Object mutex = new Object();

    private static Authenticator getAuthenticator(String username, String password) {
        if (authenticator == null) {
            synchronized (mutex) {
                if (authenticator == null) {
                    Authenticator tempAuthenticator = new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    };
                    authenticator = tempAuthenticator;
                }
            }
        }
        return authenticator;
    }

    public static void sendEmail(String from, String[] tos, String[] ccs, String subject, String content, AttachmentType[] attachments, Properties mailProperties, String username, String password) {
        Session session = Session.getDefaultInstance(mailProperties, getAuthenticator(username, password));
        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(from));
            for (String to: tos) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            }
            if (ccs != null) {
                for (String cc: ccs) {
                    message.addRecipient(Message.RecipientType.CC, new InternetAddress(cc));
                }
            }
            message.setSubject(subject);
            if (attachments != null && attachments.length > 0) {
                Multipart multipart = new MimeMultipart();
                message.setContent(multipart);
                MimeBodyPart textBodyPart = new MimeBodyPart();
                textBodyPart.setContent(content, "text/html; charset=utf-8");
                multipart.addBodyPart(textBodyPart);  // add the text part
                message.saveChanges();
                for (AttachmentType attachment: attachments) {
                    MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                    DataSource source = new ByteArrayDataSource(attachment.getContent(), attachment.getType());
                    attachmentBodyPart.setDataHandler(new DataHandler(source));
                    attachmentBodyPart.setFileName(attachment.getName());
                    multipart.addBodyPart(attachmentBodyPart);
                }
            } else {
                message.setContent(content, "text/html; charset=utf-8");
            }
            Transport.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Error sending email", e);
        }
    }

}
