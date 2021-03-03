package com.gitb.messaging.layer.application.as2;

import com.gitb.core.Configuration;
import com.gitb.messaging.KeyStoreFactory;
import com.gitb.messaging.Message;
import com.gitb.messaging.ServerUtils;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.layer.application.http.HttpReceiver;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.model.tcp.ITransactionSender;
import com.gitb.types.BinaryType;
import com.gitb.types.MapType;
import com.helger.as2lib.disposition.DispositionType;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.mime.CMimeType;
import com.helger.mail.datasource.ByteArrayDataSource;
import com.sun.xml.messaging.saaj.packaging.mime.internet.InternetHeaders;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by senan on 11.11.2014.
 */
public class AS2Receiver extends HttpReceiver {
    private Logger logger = LoggerFactory.getLogger(AS2Receiver.class);

    public AS2Receiver(SessionContext sessionContext, TransactionContext transactionContext) {
        super(sessionContext, transactionContext);
    }

    @Override
    public Message receive(List<Configuration> configurations, Message inputs) throws Exception {
        Message received =  super.receive(configurations, inputs);

        MapType headers = (MapType) received.getFragments().get(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME);
        BinaryType body = (BinaryType) received.getFragments().get(HttpMessagingHandler.HTTP_BODY_FIELD_NAME);

        //put data into a MimeBody
        MimeBodyPart mimeBody = createMimeBody(headers, body);

        //decrypt & verify MimeBody which is previously signed and encrypted by the sender
        mimeBody = AS2MessagingHandler.decrypt(mimeBody, KeyStoreFactory.getInstance().getCertificate(), KeyStoreFactory.getInstance().getPrivateKey());
        mimeBody = AS2MessagingHandler.verify(mimeBody, AS2MessagingHandler.getSUTCertificate(transaction));

        //send MDN (Message Disposition Notification)
        sendMDN(mimeBody, headers);

        //convert MimeBody into a DataType
        BinaryType as2Message = getAS2Message(mimeBody);

        Message message = new Message();
        message.getFragments().put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, headers);
        message.getFragments().put(AS2MessagingHandler.AS2_MESSAGE_FIELD_NAME,   as2Message);
        return message;
    }

    private MimeBodyPart createMimeBody(MapType headers, BinaryType body) throws Exception {
        ContentType contentType = new ContentType(ServerUtils.getHeader(headers, CHttpHeader.CONTENT_TYPE));
        String receivedContentType = contentType.toString();

        byte[] data = (byte[]) body.getValue();
        MimeBodyPart mimeBody = new MimeBodyPart ();
        mimeBody.setDataHandler(new DataHandler(new ByteArrayDataSource(data, receivedContentType, null)));
        mimeBody.setHeader(CHttpHeader.CONTENT_TYPE, receivedContentType);

        return mimeBody;
    }

    private BinaryType getAS2Message(MimeBodyPart mimeBody) throws IOException, MessagingException {
        InputStream stream = mimeBody.getInputStream();
        byte[] data = IOUtils.toByteArray(stream);

        BinaryType binaryType = new BinaryType();
        binaryType.setValue(data);
        return binaryType;
    }

    public Message receiveMDN(DefaultBHttpClientConnection connection, String originalMIC) throws Exception {
        Message received =  null; //*** super.receiveHttpResponse(connection);

        MapType headers = (MapType) received.getFragments().get(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME);
        BinaryType body = (BinaryType) received.getFragments().get(HttpMessagingHandler.HTTP_BODY_FIELD_NAME);

        //put data into a MimeBody
        MimeBodyPart mimeBody = createMimeBody(headers, body);

        //verify MimeBody which is previously signed by the sender
        mimeBody = AS2MessagingHandler.verify(mimeBody, AS2MessagingHandler.getSUTCertificate(transaction));

        //calculate MIC
        checkMDN(mimeBody, originalMIC);

        return received;
    }

    private void checkMDN(MimeBodyPart mimeBody, String originalMIC) throws Exception {
        MimeMultipart reportParts = new MimeMultipart(mimeBody.getDataHandler().getDataSource ());
        ContentType reportType = new ContentType (reportParts.getContentType ());
        String receivedMIC = null;
        String disposition = null;

        if (reportType.getBaseType().equalsIgnoreCase("multipart/report")) {
            int reportCount = reportParts.getCount();

            for (int j = 0; j < reportCount; j++)
            {
                final MimeBodyPart reportPart = (MimeBodyPart) reportParts.getBodyPart (j);
                if (reportPart.isMimeType (CMimeType.TEXT_PLAIN.getAsString ())){
                    //do nothing, since we only care about the MIC (Message Integrity Check)
                }
                else if(reportPart.isMimeType("message/disposition-notification"))  {
                    InternetHeaders headers = new InternetHeaders (reportPart.getInputStream ());
                    receivedMIC = headers.getHeader(AS2MessagingHandler.HEADER_RECEIVED_CONTENT_MIC, ", ");
                    disposition = headers.getHeader(AS2MessagingHandler.HEADER_DISPOSITION, ", ");
                }
            }
        }

        //compare MICs for equality
        if (receivedMIC == null || !receivedMIC.replaceAll (" ", "").equals (originalMIC.replaceAll (" ", ""))) {
            throw new Exception("MIC is not matched, original MIC: " +  originalMIC + " return mic: " + receivedMIC);
        } else {
            logger.debug(addMarker(), "MICs matched, MIC: " + originalMIC);
        }

        //validate disposition type
        DispositionType.createFromString(disposition).validate();
    }

    /** MDN section **/

    private void sendMDN(MimeBodyPart mimeBody, MapType receivedHeaders) throws Exception {
        //get sender and send MDN
        AS2Sender sender = (AS2Sender) transaction.getParameter(ITransactionSender.class);
        DefaultBHttpServerConnection connection = transaction.getParameter(DefaultBHttpServerConnection.class);
        sender.sendMDN(mimeBody, receivedHeaders, connection);
    }
}
