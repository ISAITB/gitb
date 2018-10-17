package com.gitb.messaging.layer.application.as2.peppol;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.messaging.KeyStoreFactory;
import com.gitb.messaging.Message;
import com.gitb.messaging.ServerUtils;
import com.gitb.messaging.layer.application.as2.AS2MIC;
import com.gitb.messaging.layer.application.as2.AS2MessagingHandler;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.layer.application.http.HttpSender;
import com.gitb.messaging.layer.application.https.HttpsSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.*;
import com.gitb.utils.ConfigurationUtils;
import com.helger.as2lib.disposition.DispositionOptions;
import com.helger.as2lib.disposition.DispositionType;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.DateUtil;
import com.helger.commons.mime.CMimeType;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.BHttpConnectionBase;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created by senan on 11.11.2014.
 */
public class PeppolAS2Sender extends HttpsSender {

    private Logger logger = LoggerFactory.getLogger(PeppolAS2Sender.class);

    private final String HTTP_CONNECTION_HEADER     = "Connection";
    private final String HTTP_CONNECTION_KEEP_ALIVE = "keep-alive";
    private final String HTTP_CONNECTION_CLOSE      = "close";

    public PeppolAS2Sender(SessionContext session, TransactionContext transaction) {
        super(session, transaction);
    }

    @Override
    public Message send(List<Configuration> configurations, Message message) throws Exception {
        connection = transaction.getParameter(BHttpConnectionBase.class);
        //A client connection is required to send a PEPPOL AS2 Message
        if( connection == null || connection instanceof DefaultBHttpClientConnection) {
            //if connection is null do not create one since HttpSender will take care of it; just deliver the msg.
            message = createAS2Message(configurations, message);
        }
        //A server connection is required to send an MDN response
        if(connection instanceof DefaultBHttpServerConnection) {
            message = createMDN(configurations, message);
        }

        configurations.add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_METHOD_CONFIG_NAME, AS2MessagingHandler.HTTP_METHOD));

        super.send(configurations, message);

        return message;
    }

    private Message createAS2Message(List<Configuration> configurations, Message message) throws Exception {
        //create mime body
        ObjectType businessDocument   = getBusinessDocument(message);
        MimeBodyPart originalMimeBody = createMimeBody(businessDocument);

        MapType headers = getHeaders(originalMimeBody);

        //sign mime body
        MimeBodyPart signedMimeBody = AS2MessagingHandler.sign(originalMimeBody, KeyStoreFactory.getInstance().getCertificate(), KeyStoreFactory.getInstance().getPrivateKey());
        headers.addItem(CAS2Header.HEADER_CONTENT_TYPE, new StringType(signedMimeBody.getContentType()));

        //calculate MIC and save it to the transaction
        String MIC = AS2MessagingHandler.calculateMIC(originalMimeBody, headers, true);
        transaction.setParameter(AS2MIC.class, new AS2MIC(MIC));
        logger.debug(addMarker(), "MIC calculated: " + MIC);

        //define message body
        BinaryType binaryType = new BinaryType();
        binaryType.setValue(IOUtils.toByteArray(signedMimeBody.getInputStream()));
        headers.addItem(CAS2Header.HEADER_CONTENT_LENGTH, new StringType(""+((byte[])binaryType.getValue()).length));

        //create received message structure
        message = new Message();
        message.getFragments().put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, binaryType);
        message.getFragments().put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, headers);
        return message;
    }

    private Message createMDN(List<Configuration> configurations, Message message) throws Exception {
        //get disposition options
        MapType receivedHeaders = (MapType) message.getFragments().get(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME);
        String sDispositionOptions = ServerUtils.getHeader(receivedHeaders, CAS2Header.HEADER_DISPOSITION_NOTIFICATION_OPTIONS);
        DispositionOptions dispositionOptions = DispositionOptions.createFromString (sDispositionOptions);

        //crete MDN Mime Body
        MimeBodyPart mdnMimeBody = createMDNMimeBody(receivedHeaders);

        //sign MDN Mime body with GITB-Engine's private key
        mdnMimeBody = AS2MessagingHandler.sign(mdnMimeBody, KeyStoreFactory.getInstance().getCertificate(), KeyStoreFactory.getInstance().getPrivateKey());

        //get MDN headers
        MapType mdnHeaders = getMDNHeaders(mdnMimeBody, receivedHeaders);

        //define MDN body
        BinaryType binaryType = new BinaryType();
        binaryType.setValue(IOUtils.toByteArray(mdnMimeBody.getInputStream()));
        mdnHeaders.addItem(CAS2Header.HEADER_CONTENT_LENGTH, new StringType(""+((byte[])binaryType.getValue()).length));

        //create received message structure
        Message mdn = new Message();
        mdn.getFragments().put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, binaryType);
        mdn.getFragments().put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, mdnHeaders);
        return mdn;
    }

    private MimeBodyPart createMimeBody(ObjectType businessDocument) throws Exception {
        byte[] data = businessDocument.serializeByDefaultEncoding();
        String contentType = CMimeType.APPLICATION_XML.getAsString();

        MimeBodyPart mimeBody = new MimeBodyPart ();
        mimeBody.setContent(businessDocument.toString(), contentType);
        //mimeBody.setDataHandler(new DataHandler(data, contentType));
        mimeBody.setHeader("Content-Transfer-Encoding", "base64");
        //mimeBody.setDataHandler(new DataHandler(new ByteArrayDataSource(data, contentType, null)));
        return mimeBody;
    }

    private MimeBodyPart createMDNMimeBody(MapType headers) throws Exception {
        String MIC = null;
        DispositionType dispositionType = transaction.getParameter(DispositionType.class);

        if(dispositionType.getStatusModifier() == null) { //no error
            //get calculated MIC
            MIC = transaction.getParameter(AS2MIC.class).getMessageIntegrityCheck();
        }

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
        reportHeaders.setHeader(AS2MessagingHandler.HEADER_REPORTING_UA, getAS2From() + "@" + getHost() + ":" + getPort());
        reportHeaders.setHeader(AS2MessagingHandler.HEADER_ORIGINAL_RECIPIENT, "rfc822; " + ServerUtils.getHeader(headers, CAS2Header.HEADER_AS2_TO));
        reportHeaders.setHeader(AS2MessagingHandler.HEADER_FINAL_RECIPIENT, "rfc822; " + ServerUtils.getHeader(headers, CAS2Header.HEADER_AS2_FROM));
        reportHeaders.setHeader(AS2MessagingHandler.HEADER_ORIGINAL_MESSAGE_ID, ServerUtils.getHeader(headers, CAS2Header.HEADER_MESSAGE_ID));
        reportHeaders.setHeader(AS2MessagingHandler.HEADER_DISPOSITION, dispositionType.getAsString());
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

    private ObjectType getBusinessDocument( Message message) {
        ObjectType object = (ObjectType) message.getFragments().get(PeppolAS2MessagingHandler.BUSINESS_DOCUMENT_FIELD_NAME);
        return object;
    }

    private MapType getHeaders(MimeBodyPart mimeBodyPart) throws Exception {
        MapType headers = new MapType();
        headers.addItem(CAS2Header.HEADER_CONNECTION, new StringType(CAS2Header.DEFAULT_CONNECTION));
        headers.addItem(CAS2Header.HEADER_USER_AGENT, new StringType(AS2MessagingHandler.AS2_FROM));
        headers.addItem(CAS2Header.HEADER_DATE, new StringType(DateUtil.getFormattedDateNow(CAS2Header.DEFAULT_DATE_FORMAT)));
        headers.addItem(CAS2Header.HEADER_MESSAGE_ID, new StringType(AS2MessagingHandler.generateMessageId(getAS2To())));
        headers.addItem(CAS2Header.HEADER_MIME_VERSION, new StringType(CAS2Header.DEFAULT_MIME_VERSION));
        headers.addItem(CAS2Header.HEADER_AS2_VERSION, new StringType(CAS2Header.DEFAULT_AS2_VERSION));
        headers.addItem(CAS2Header.HEADER_RECIPIENT_ADDRESS, new StringType("https://" + getHost() + ":" + getPort()));
        headers.addItem(CAS2Header.HEADER_AS2_TO, new StringType(getAS2To()));
        headers.addItem(CAS2Header.HEADER_AS2_FROM, new StringType(getAS2From()));
        headers.addItem(CAS2Header.HEADER_SUBJECT, new StringType(AS2MessagingHandler.AS2_SUBJECT));
        headers.addItem(CAS2Header.HEADER_FROM, new StringType(getAS2From()));
        headers.addItem(CAS2Header.HEADER_DISPOSITION_NOTIFICATION_OPTIONS, new StringType(AS2MessagingHandler.DEFAULT_MDN_OPTIONS));
        headers.addItem(HTTP_CONNECTION_HEADER, new StringType(HTTP_CONNECTION_KEEP_ALIVE));
        return headers;
    }

    private MapType getMDNHeaders(MimeBodyPart mimeBody, MapType receivedHeaders) throws Exception {
        MapType headers = new MapType();
        headers.addItem(CAS2Header.HEADER_AS2_VERSION, new StringType(CAS2Header.DEFAULT_AS2_VERSION));
        headers.addItem(CAS2Header.HEADER_DATE, new StringType(DateUtil.getFormattedDateNow(CAS2Header.DEFAULT_DATE_FORMAT)));
        headers.addItem(CAS2Header.HEADER_SERVER, new StringType(getAS2From()));
        headers.addItem(CAS2Header.HEADER_MESSAGE_ID, new StringType(AS2MessagingHandler.generateMessageId(getAS2To())));
        headers.addItem(CAS2Header.HEADER_MIME_VERSION, new StringType(CAS2Header.DEFAULT_MIME_VERSION));
        headers.addItem(CAS2Header.HEADER_AS2_FROM, new StringType(ServerUtils.getHeader(receivedHeaders, CAS2Header.HEADER_AS2_TO)));
        headers.addItem(CAS2Header.HEADER_AS2_TO, new StringType(ServerUtils.getHeader(receivedHeaders, CAS2Header.HEADER_AS2_FROM)));
        headers.addItem(CAS2Header.HEADER_FROM, new StringType(ServerUtils.getHeader(receivedHeaders, CAS2Header.HEADER_AS2_TO)));
        headers.addItem(CAS2Header.HEADER_CONTENT_TYPE, new StringType(mimeBody.getContentType()));
        headers.addItem(HTTP_CONNECTION_HEADER, new StringType(HTTP_CONNECTION_CLOSE));
        headers.addItem(CAS2Header.HEADER_SUBJECT, new StringType("Requested MDN Response"));
        return headers;
    }

    private String getAS2From() throws Exception {
        X509Certificate certificate = KeyStoreFactory.getInstance().getCertificate();
        return PeppolAS2MessagingHandler.getCN(certificate);
    }

    private String getAS2To() throws Exception {
        X509Certificate certificate = AS2MessagingHandler.getSUTCertificate(transaction);
        return PeppolAS2MessagingHandler.getCN(certificate);
    }
}
