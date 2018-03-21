package com.gitb.messaging.layer.application.as2;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.messaging.KeyStoreFactory;
import com.gitb.messaging.Message;
import com.gitb.messaging.ServerUtils;
import com.gitb.messaging.layer.application.as2.peppol.PeppolAS2MessagingHandler;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.layer.application.http.HttpSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.model.tcp.ITransactionReceiver;
import com.gitb.types.BinaryType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;
import com.helger.as2lib.disposition.DispositionOptions;
import com.helger.as2lib.disposition.DispositionType;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.DateUtil;
import com.helger.as2lib.util.javamail.ByteArrayDataSource;
import com.helger.commons.mime.CMimeType;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by senan on 11.11.2014.
 */
public class AS2Sender extends HttpSender {
    private Logger logger = LoggerFactory.getLogger(AS2Sender.class);

    public AS2Sender(SessionContext session, TransactionContext transaction) {
        super(session, transaction);
    }

    @Override
    public Message send(List<Configuration> configurations, Message message) throws Exception {
        //create mime body
        BinaryType payload = getAS2Message(message);
        MimeBodyPart originalMimeBody = createMimeBody(payload);

        MapType headers = getHeaders(originalMimeBody);

        //sign & encrypt mime body
        MimeBodyPart signedMimeBody    = AS2MessagingHandler.sign(originalMimeBody, KeyStoreFactory.getInstance().getCertificate(), KeyStoreFactory.getInstance().getPrivateKey());
        MimeBodyPart encryptedMimeBody = AS2MessagingHandler.encrypt(signedMimeBody, AS2MessagingHandler.getSUTCertificate(transaction));

        //calculate MIC after signing & encrypting since these operations updates headers of the original MIME Body
        String MIC = AS2MessagingHandler.calculateMIC(originalMimeBody, headers, true);

        logger.debug("MIC calculated: " + MIC);

        headers.addItem(CAS2Header.HEADER_CONTENT_TYPE, new StringType(encryptedMimeBody.getContentType()));

        //define message parameters
        BinaryType binaryType = new BinaryType();
        binaryType.setValue(IOUtils.toByteArray(encryptedMimeBody.getInputStream()));

        message.getFragments().put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, binaryType);
        message.getFragments().put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, headers);

        configurations
                .add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_METHOD_CONFIG_NAME, AS2MessagingHandler.HTTP_METHOD));

        super.send(configurations, message);

        //receive MDN (Message Disposition Notification) and perform necessary checks
//        receiveMDN(MIC);

        return message;
    }

    private MimeBodyPart createMimeBody(BinaryType payload) throws Exception {
        byte[] data = (byte[]) payload.getValue();
        String contentType = CMimeType.TEXT_PLAIN.getAsString();

        MimeBodyPart mimeBody = new MimeBodyPart ();
        mimeBody.setDataHandler(new DataHandler(new ByteArrayDataSource(data, contentType, null)));
        return mimeBody;
    }

    private BinaryType getAS2Message( Message message) {
        BinaryType payload = (BinaryType) message.getFragments().get(AS2MessagingHandler.AS2_MESSAGE_FIELD_NAME);
        return payload;
    }

    private MapType getHeaders(MimeBodyPart mimeBodyPart) throws Exception {
        MapType headers = new MapType();

        headers.addItem(CAS2Header.HEADER_CONNECTION, new StringType(CAS2Header.DEFAULT_CONNECTION));
        headers.addItem(CAS2Header.HEADER_USER_AGENT, new StringType(CAS2Header.DEFAULT_USER_AGENT));
        headers.addItem(CAS2Header.HEADER_DATE, new StringType(DateUtil.getFormattedDateNow(CAS2Header.DEFAULT_DATE_FORMAT)));
        headers.addItem(CAS2Header.HEADER_MESSAGE_ID, new StringType(AS2MessagingHandler.generateMessageId(getAS2To())));
        headers.addItem(CAS2Header.HEADER_MIME_VERSION, new StringType(CAS2Header.DEFAULT_MIME_VERSION));
        headers.addItem(CAS2Header.HEADER_AS2_VERSION, new StringType(CAS2Header.DEFAULT_AS2_VERSION));
        headers.addItem(CAS2Header.HEADER_RECIPIENT_ADDRESS, new StringType(getRecipient()));
        headers.addItem(CAS2Header.HEADER_AS2_TO, new StringType(getAS2To()));
        headers.addItem(CAS2Header.HEADER_AS2_FROM, new StringType(AS2MessagingHandler.AS2_FROM));
        headers.addItem(CAS2Header.HEADER_SUBJECT, new StringType(AS2MessagingHandler.AS2_SUBJECT));
        headers.addItem(CAS2Header.HEADER_FROM, new StringType(AS2MessagingHandler.AS2_FROM));
        headers.addItem(CAS2Header.HEADER_DISPOSITION_NOTIFICATION_OPTIONS, new StringType(AS2MessagingHandler.DEFAULT_MDN_OPTIONS));
        //Content-Length will be added by HttpSender

        return headers;
    }

    public void sendMDN(MimeBodyPart mimeBody, MapType receivedHeaders, DefaultBHttpServerConnection connection) throws Exception {
        //calculate original MIC
        String MIC = AS2MessagingHandler.calculateMIC(mimeBody, receivedHeaders, true);

        //get disposition options
        String sDispositionOptions = ServerUtils.getHeader(receivedHeaders, CAS2Header.HEADER_DISPOSITION_NOTIFICATION_OPTIONS);
        DispositionOptions dispositionOptions = DispositionOptions.createFromString (sDispositionOptions);

        //crete MDN Mime Body
        MimeBodyPart mdnMimeBody = createMDNMimeBody(MIC, receivedHeaders);

        if(dispositionOptions.getProtocol() != null){
            mdnMimeBody = AS2MessagingHandler.sign(mdnMimeBody, KeyStoreFactory.getInstance().getCertificate(), KeyStoreFactory.getInstance().getPrivateKey());
        }

        //get MDN headers
        MapType mdnHeaders = getMDNHeaders(mdnMimeBody, receivedHeaders);

        BinaryType binaryType = new BinaryType();
        binaryType.setValue(IOUtils.toByteArray(mdnMimeBody.getInputStream()));

        Message message = new Message();
        message.getFragments().put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, binaryType);
        message.getFragments().put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, mdnHeaders);

        ActorConfiguration actorConfiguration = transaction.getWith();
        actorConfiguration.getConfig()
                .add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_METHOD_CONFIG_NAME, AS2MessagingHandler.HTTP_METHOD));

        //get sender and send MDN
//        BasicHttpResponse response = createHttpResponse(actorConfiguration.getConfig(), message);

        super.send(actorConfiguration.getConfig(), message);
    }

    private MimeBodyPart createMDNMimeBody(String MIC, MapType headers) throws MessagingException {
        // Create the report and sub-body parts
        MimeMultipart reportParts = new MimeMultipart();

        // Create the text part
        final MimeBodyPart textPart = new MimeBodyPart ();
        textPart.setContent("This is an MDN message" + "\r\n", CMimeType.TEXT_PLAIN.getAsString ());
        textPart.setHeader(CAS2Header.HEADER_CONTENT_TYPE, CMimeType.TEXT_PLAIN.getAsString ());
        reportParts.addBodyPart(textPart);

        // Create the report part
        MimeBodyPart reportPart = new MimeBodyPart ();
        InternetHeaders reportHeaders = new InternetHeaders ();
        reportHeaders.setHeader(AS2MessagingHandler.HEADER_REPORTING_UA,AS2MessagingHandler.AS2_FROM + "@" + getHost() + ":" + getPort());
        reportHeaders.setHeader(AS2MessagingHandler.HEADER_ORIGINAL_RECIPIENT, "rfc822; " + ServerUtils.getHeader(headers, CAS2Header.HEADER_AS2_TO));
        reportHeaders.setHeader(AS2MessagingHandler.HEADER_FINAL_RECIPIENT, "rfc822; " + ServerUtils.getHeader(headers, CAS2Header.HEADER_AS2_FROM));
        reportHeaders.setHeader(AS2MessagingHandler.HEADER_ORIGINAL_MESSAGE_ID, ServerUtils.getHeader(headers, CAS2Header.HEADER_MESSAGE_ID));
        reportHeaders.setHeader(AS2MessagingHandler.HEADER_DISPOSITION, DispositionType.createSuccess().getAsString());
        reportHeaders.setHeader(AS2MessagingHandler.HEADER_RECEIVED_CONTENT_MIC, MIC);

        final Enumeration<?> reportEnumeration = reportHeaders.getAllHeaderLines();
        final StringBuilder reportData = new StringBuilder();
        while (reportEnumeration.hasMoreElements()){
            reportData.append ((String) reportEnumeration.nextElement()).append ("\r\n");
        }
        reportData.append ("\r\n");
        reportPart.setContent (reportData.toString(), "message/disposition-notification");
        reportPart.setHeader (CAS2Header.HEADER_CONTENT_TYPE, "message/disposition-notification");
        reportParts.addBodyPart (reportPart);

        // Convert report parts to MimeBodyPart
        MimeBodyPart report = new MimeBodyPart ();
        reportParts.setSubType ("report; report-type=disposition-notification");
        report.setContent(reportParts);
        report.setHeader(CAS2Header.HEADER_CONTENT_TYPE, reportParts.getContentType ());

        return report;
    }

    private MapType getMDNHeaders(MimeBodyPart mimeBody, MapType receivedHeaders) throws Exception {
        MapType headers = new MapType();

        headers.addItem(CAS2Header.HEADER_AS2_VERSION, new StringType(CAS2Header.DEFAULT_AS2_VERSION));
        headers.addItem(CAS2Header.HEADER_DATE, new StringType(DateUtil.getFormattedDateNow(CAS2Header.DEFAULT_DATE_FORMAT)));
        headers.addItem(CAS2Header.HEADER_SERVER, new StringType(AS2MessagingHandler.AS2_FROM));
        headers.addItem(CAS2Header.HEADER_MIME_VERSION, new StringType(CAS2Header.DEFAULT_MIME_VERSION));
        headers.addItem(CAS2Header.HEADER_AS2_FROM, new StringType(ServerUtils.getHeader(receivedHeaders, CAS2Header.HEADER_AS2_TO)));
        headers.addItem(CAS2Header.HEADER_AS2_TO, new StringType(ServerUtils.getHeader(receivedHeaders, CAS2Header.HEADER_AS2_FROM)));
        headers.addItem(CAS2Header.HEADER_FROM, new StringType(ServerUtils.getHeader(receivedHeaders, CAS2Header.HEADER_AS2_TO)));
        headers.addItem(CAS2Header.HEADER_CONTENT_TYPE, new StringType(mimeBody.getContentType()));

        String subject = ServerUtils.getHeader(receivedHeaders, CAS2Header.HEADER_SUBJECT);
        if(subject != null) {
            headers.addItem(CAS2Header.HEADER_SUBJECT, new StringType(subject));
        } else {
            headers.addItem(CAS2Header.HEADER_SUBJECT, new StringType("Requested MDN Response"));
        }
        return headers;
    }

    private String getAS2To() throws Exception {
        String to;
        ActorConfiguration actorConfiguration = transaction.getWith();
        Configuration as2To = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), AS2MessagingHandler.AS2_NAME_CONFIG_NAME);
        if (as2To == null) {
            X509Certificate certificate = AS2MessagingHandler.getSUTCertificate(transaction);
            to = PeppolAS2MessagingHandler.getCN(certificate);
        } else {
            to = as2To.getValue();
        }
        return to;
    }

    private String getRecipient() {
        ActorConfiguration actorConfiguration = transaction.getWith();
        Configuration ipAddress = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.IP_ADDRESS_CONFIG_NAME);
        Configuration port      = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.PORT_CONFIG_NAME);
        return "http://" + ipAddress.getValue() + ":" + port.getValue() + "/";
    }

    private Message receiveMDN(String originalMIC) throws Exception {
        AS2Receiver receiver = (AS2Receiver) transaction.getParameter(ITransactionReceiver.class);
        DefaultBHttpClientConnection connection = transaction.getParameter(DefaultBHttpClientConnection.class);
        return receiver.receiveMDN(connection, originalMIC);
    }
}
