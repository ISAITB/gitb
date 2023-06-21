package utils;

import config.Configurations;
import managers.AttachmentType;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

public class EmailUtil {

    private static Authenticator authenticator;
    private static final Object mutex = new Object();

    private static Authenticator getAuthenticator(String username, String password) {
        if (authenticator == null) {
            synchronized (mutex) {
                if (authenticator == null) {
                    authenticator = new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    };
                }
            }
        }
        return authenticator;
    }

    public static void sendEmail(String from, String[] tos, String[] ccs, String subject, String content, AttachmentType[] attachments) {
        Session session = Session.getInstance(Configurations.SMTP_PROPERTIES(), getAuthenticator(Configurations.EMAIL_SMTP_AUTH_USERNAME(), Configurations.EMAIL_SMTP_AUTH_PASSWORD()));
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
                    DataSource source = new FileDataSource(attachment.getContent());
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
