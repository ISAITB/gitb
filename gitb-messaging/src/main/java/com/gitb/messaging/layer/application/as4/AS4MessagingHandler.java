package com.gitb.messaging.layer.application.as4;

import com.gitb.core.MessagingModule;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.ServerUtils;
import com.gitb.messaging.layer.AbstractMessagingHandler;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.model.tcp.ITransactionReceiver;
import com.gitb.messaging.model.tcp.ITransactionSender;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import com.gitb.types.BinaryType;
import com.gitb.types.MapType;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.javamail.ByteArrayDataSource;
import com.helger.commons.base64.Base64;
import org.apache.commons.io.IOUtils;
import org.kohsuke.MetaInfServices;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Class handling AS4 communication.
 *
 * This class contains reusable utilities shared by both its related receiver and sender.
 *
 * Created by simatosc on 02/04/2015.
 */
@MetaInfServices(IMessagingHandler.class)
public class AS4MessagingHandler extends AbstractMessagingHandler {

    private static final String MODULE_DEFINITION_XML = "/as4-messaging-definition.xml";

    public static final String HTTP_HEADERS_FIELD_NAME = HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME;
    public static final String AS4_COMMUNICATION_DATA_FIELD_NAME = "as4_communication_data";
    public static final String SOAP_ENVELOPE_FIELD_NAME = "soap_envelope";
    public static final String RAW_MESSAGE_FIELD_NAME = "raw_message";
    public static final String BUSINESS_MESSAGE_FIELD_NAME = "business_message";
    public static final String AS4_MESSAGE_ID_FIELD_NAME  = "as4_message_id";
    public static final String RECEIPT_SOAP_RESPONSE_FIELD_NAME  = "receipt_soap_response";

    public static final String DOMIBUS_REQUEST_FIELD_NAME  = "domibus_request";
    public static final String DOMIBUS_SOAP_RESPONSE_FIELD_NAME = "domibus_soap_response";
    public static final String DOMIBUS_RAW_RESPONSE_FIELD_NAME = "domibus_raw_response";
    public static final String DOMIBUS_RESPONSE_PAYLOAD_FIELD_NAME  = "domibus_response_payload";

    public static final String DOMIBUS_ADDRESS_CONFIG_NAME  = "domibus.address";
    public static final String AS4_RECEIVER_ADDRESS_CONFIG_NAME  = "as4.receiver.address";

    public static final String HTTP_METHOD = "POST";

    public static final String WSSE_URI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    public static final String DS_URI = "http://www.w3.org/2000/09/xmldsig#";
    public static final String EB_URI = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";
    public static final String HTTP_CONTENT_TYPE_HEADER = "Content-Type";

    private static MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

    @Override
    public MessagingModule getModuleDefinition() {
        return module;
    }

    @Override
    public ITransactionReceiver getReceiver(SessionContext sessionContext, TransactionContext transactionContext) throws IOException {
        return new AS4Receiver(sessionContext, transactionContext);
    }

    @Override
    public ITransactionSender getSender(SessionContext sessionContext, TransactionContext transactionContext) {
        return new AS4Sender(sessionContext, transactionContext);
    }

    /**
     * Create a mime body part from the provided header and body information.
     *
     * @param headers The headers to use.
     * @param body The body of the mime part.
     * @return The generated mime body part.
     * @throws Exception
     */
    protected static MimeBodyPart createMimeBody(MapType headers, BinaryType body) throws Exception {
        ContentType contentType = new ContentType(ServerUtils.getHeader(headers, CAS2Header.HEADER_CONTENT_TYPE));
        String receivedContentType = contentType.toString();

        byte[] data = (byte[]) body.getValue();
        MimeBodyPart mimeBody = new MimeBodyPart ();
        mimeBody.setDataHandler(new DataHandler(new ByteArrayDataSource(data, receivedContentType, null)));
        mimeBody.setHeader(CAS2Header.HEADER_CONTENT_TYPE, receivedContentType);

        return mimeBody;
    }

    /**
     * Extract the SOAP envelope contained in the provided mime body part.
     * This method assumes that the SOAP envelope is the first body part. Both SOAP 1.1 and 1.2 are supported.
     *
     * @param mimeBody The mime body from which the SOAP message is to be extracted.
     * @return The SOAP message.
     * @throws Exception
     */
    public static SOAPMessage getSoapEnvelope(MimeBodyPart mimeBody) throws Exception {
        SOAPMessage soapMessage;
        try {
            byte[] messageBytes = IOUtils.toByteArray(((MimeBodyPart) ((MimeMultipart) (mimeBody.getContent())).getBodyPart(0)).getRawInputStream());
            // Inspect the XML content to determine the SOAP version to use.
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            InputSource source = new InputSource(new ByteArrayInputStream(messageBytes));
            Document document = factory.newDocumentBuilder().parse(source);
            String soapProtocol;
            Node envelopeNode = document.getFirstChild();
            if ("Envelope".equals(envelopeNode.getLocalName())) {
                if (SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE.equals(envelopeNode.getNamespaceURI())) {
                    soapProtocol = SOAPConstants.SOAP_1_2_PROTOCOL;
                } else if (SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE.equals(envelopeNode.getNamespaceURI())) {
                    soapProtocol = SOAPConstants.SOAP_1_1_PROTOCOL;
                } else {
                    throw new IllegalStateException("Received message was not SOAP version 1.1 or 1.2. The URI received was ["+envelopeNode.getNamespaceURI()+"].");
                }
            } else {
                throw new IllegalStateException("Envelope node was not the root of the message.");
            }
            MessageFactory messageFactory = MessageFactory.newInstance(soapProtocol);
            soapMessage = messageFactory.createMessage(null, new ByteArrayInputStream(messageBytes));
        } catch (Exception e) {
            throw new IllegalStateException("An error occurred while extracting SOAP message ["+e.getMessage()+"].", e);
        }
        if (soapMessage == null) {
            throw new IllegalStateException("Unable to extract SOAP message from mime body.");
        }
        return soapMessage;
    }

    /**
     * Determine which content (mime) type should be used for the provided SOAP message.
     *
     * @param soapMessage The SOAP message.
     * @return The content type to use.
     * @throws javax.xml.soap.SOAPException
     */
    public static String getContentTypeForSoapMessage(SOAPMessage soapMessage) throws SOAPException {
        String contentType;
        if (SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE.equals(soapMessage.getSOAPPart().getEnvelope().getNamespaceURI())) {
            contentType = SOAPConstants.SOAP_1_2_CONTENT_TYPE;
        } else if (SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE.equals(soapMessage.getSOAPPart().getEnvelope().getNamespaceURI())) {
            contentType = SOAPConstants.SOAP_1_1_CONTENT_TYPE;
        } else {
            throw new IllegalStateException("Only SOAP 1.1 and SOAP 1.2 messages are supported (namespace URI received was ["+soapMessage.getSOAPPart().getEnvelope().getNamespaceURI()+"])");
        }
        return contentType;
    }

    /**
     * Get a child SOAP element matching the namespace URI and local name provided under the provided parent node.
     * The child element is expected as mandatory and if missing will cause an error.
     *
     * @param parent The parent node under which to search.
     * @param uri The namespace URI for the child element.
     * @param localName The local name for the child element.
     * @return The matched SOAP element.
     * @throws IllegalStateException If the child element is not found.
     */
    public static SOAPElement getMandatoryChild(SOAPElement parent, String uri, String localName) {
        for (Iterator it = parent.getChildElements(); it.hasNext();) {
            Object element = it.next();
            if (element instanceof SOAPElement) {
                if ((uri == null || uri.equals(((SOAPElement)element).getNamespaceURI())) && localName.equals(((SOAPElement)element).getLocalName())) {
                    return (SOAPElement)element;
                }
            }
        }
        throw new IllegalStateException("No element with local name ["+localName+"] and namespace URI ["+uri+"] could be found under ["+parent.getNodeName()+"].");
    }

    /**
     * Convert the provided Base64 string into its corresponding bytes.
     *
     * @param base64 The base64 string.
     * @return The bytes.
     * @throws Exception
     */
    public static byte[] getBase64AsBytes(String base64) throws Exception {
        return Base64.decode(base64, Base64.DECODE);
    }

    /**
     * Get the raw message bytes from the provided mime body part.
     *
     * @param mimeBody The mime body part.
     * @return The part's bytes.
     * @throws java.io.IOException
     * @throws javax.mail.MessagingException
     */
    public static BinaryType getRawMessage(MimeBodyPart mimeBody) throws IOException, MessagingException {
        InputStream stream = mimeBody.getInputStream();
        byte[] data = IOUtils.toByteArray(stream);
        BinaryType binaryType = new BinaryType();
        binaryType.setValue(data);
        return binaryType;
    }

}
