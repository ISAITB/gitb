package com.gitb.messaging.layer.application.as4;

import com.gitb.core.Configuration;
import com.gitb.messaging.KeyStoreFactory;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.layer.application.http.HttpSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.BinaryType;
import com.gitb.types.MapType;
import com.gitb.types.ObjectType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;
import com.gitb.utils.XMLUtils;
import com.helger.as2lib.util.javamail.ByteArrayDataSource;
import com.helger.commons.base64.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.activation.DataHandler;
import javax.mail.internet.MimeBodyPart;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Class used to respond to AS4 messages.
 *
 * This class extracts the received SOAP message and replaces the contained sender information with that corresponding
 * to the testbed:
 * <ul>
 *    <li>The sender's certificate.</li>
 *    <li>The sender party ID (to match the testbed certificate's CN).</li>
 *    <li>The signature based on the final content and the testbed certificate.</li>
 * <ul/>
 * This replacement is done in order to allow the same reference implementation to participate in communication
 * with any AS4 gateway as SUT without needing reconfiguration of trusted certificates.
 *
 * As a subsequent verification step, and once the message is sent to the reference implementation, this class also
 * queries the reference implementation backend to retrieve the message just sent. This is done in order to expose
 * the downloaded message's content and business payload so that they can be included in test cases.
 *
 * Created by simatosc on 02/04/2015.
 */
public class AS4Sender extends HttpSender {

    private Logger logger = LoggerFactory.getLogger(AS4Sender.class);

    public AS4Sender(SessionContext sessionContext, TransactionContext transactionContext) {
        super(sessionContext, transactionContext);
    }

    /**
     * Replace the signature and sender identification information in the received message.
     *
     * @param header The SOAP header of the received message.
     * @throws Exception
     */
    private void replaceSignature(SOAPHeader header) throws Exception {
        X509Certificate certificate = KeyStoreFactory.getInstance().getCertificate();
        SOAPElement securityNode = AS4MessagingHandler.getMandatoryChild(header, AS4MessagingHandler.WSSE_URI, "Security");
        SOAPElement binarySecurityToken = AS4MessagingHandler.getMandatoryChild(securityNode, AS4MessagingHandler.WSSE_URI, "BinarySecurityToken");
        // Remove the existing certificate information.
        securityNode.removeChild(binarySecurityToken);
        // Add the new binary security token.
        SOAPElement binarySecurity = securityNode.addChildElement(new QName(AS4MessagingHandler.WSSE_URI, "BinarySecurityToken", "wsse"));
        binarySecurity.setAttribute("EncodingType", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");
        binarySecurity.setAttribute("ValueType", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3");
        binarySecurity.setValue(Base64.encodeBytes(certificate.getEncoded()));
        // Remove existing signature node.
        SOAPElement signatureNode = AS4MessagingHandler.getMandatoryChild(securityNode, AS4MessagingHandler.DS_URI, "Signature");
        securityNode.removeChild(signatureNode);
        // Replace sender ID.
        replaceSenderId(header, certificate);
        // Add new signature.
        XMLUtils.sign(securityNode, KeyStoreFactory.getInstance().getCertificate(), KeyStoreFactory.getInstance().getPrivateKey());
    }

    /**
     * Replace the sender party ID in the message header to match that of the testbed.
     *
     * @param header The current SOAP header.
     * @param certificate The certificate whose CN is to be set as the new party ID.
     * @throws Exception
     */
    private void replaceSenderId(SOAPHeader header, X509Certificate certificate) throws Exception {
        // Retrieve existing party ID node.
        SOAPElement ebMessaging = AS4MessagingHandler.getMandatoryChild(header, AS4MessagingHandler.EB_URI, "Messaging");
        SOAPElement ebUserMessage = AS4MessagingHandler.getMandatoryChild(ebMessaging, AS4MessagingHandler.EB_URI, "UserMessage");
        SOAPElement ebPartyInfo = AS4MessagingHandler.getMandatoryChild(ebUserMessage, AS4MessagingHandler.EB_URI, "PartyInfo");
        SOAPElement ebFrom = AS4MessagingHandler.getMandatoryChild(ebPartyInfo, AS4MessagingHandler.EB_URI, "From");
        SOAPElement ebPartyId = AS4MessagingHandler.getMandatoryChild(ebFrom, AS4MessagingHandler.EB_URI, "PartyId");
        // Get CN from testbed certificate.
        String dn = certificate.getIssuerX500Principal().getName();
        LdapName ln = new LdapName(dn);
        String cn = null;
        for (Rdn rdn : ln.getRdns()) {
            if (rdn.getType().equalsIgnoreCase("CN")) {
                cn = String.valueOf(rdn.getValue());
            }
        }
        if (cn == null) {
            throw new IllegalStateException("Could not extract CN from testbed certificate");
        }
        // Use certificate CN as new sender ID.
        ebPartyId.setValue(cn);
    }

    /**
     * Send the AS4 message to the reference implementation.
     *
     * @param configurations The input configuration.
     * @param message The input message.
     * @throws Exception
     */
    private void sendAS4MessageToReferenceImplementation(List<Configuration> configurations, Message message) throws Exception {
        MapType as4CommunicationData = (MapType) message.getFragments().get(AS4MessagingHandler.AS4_COMMUNICATION_DATA_FIELD_NAME);
        BinaryType rawMessage = (BinaryType) as4CommunicationData.getItem(AS4MessagingHandler.RAW_MESSAGE_FIELD_NAME);
        MapType receivedHeaders = (MapType) message.getFragments().get(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME);
        // Retrieve original SOAP message.
        MimeBodyPart mimeBody = AS4MessagingHandler.createMimeBody(receivedHeaders, rawMessage);
        SOAPMessage soapMessage = AS4MessagingHandler.getSoapEnvelope(mimeBody);
        // Replace signature and sender information.
        replaceSignature(soapMessage.getSOAPHeader());
        // Prepare message for reference implementation. We do this manually to construct the exact header and mime content needed.
        String contentType = AS4MessagingHandler.getContentTypeForSoapMessage(soapMessage);
        StringBuilder messageContent = new StringBuilder();
        messageContent
                .append("--MIMEBoundary_70d7c4906bec8b568135451bd3ab7fee123962d07f1566df\n")
                .append("Content-Type: ").append(contentType).append("; charset=UTF-8\n")
                .append("Content-Transfer-Encoding: binary\n")
                .append("Content-ID: <0.60d7c4906bec8b568135451bd3ab7fee123962d07f1566df@apache.org>\n\n");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        soapMessage.writeTo(outputStream);
        messageContent.append(new String(outputStream.toByteArray()))
                .append("\n--MIMEBoundary_70d7c4906bec8b568135451bd3ab7fee123962d07f1566df--");
        HttpEntity postEntity = new ByteArrayEntity(messageContent.toString().getBytes());
        // Send message to reference implementation.
        Configuration receiverConfiguration = ConfigurationUtils.getConfiguration(configurations, AS4MessagingHandler.AS4_RECEIVER_ADDRESS_CONFIG_NAME);
        HttpPost httpPost = new HttpPost(receiverConfiguration.getValue());
        httpPost.setEntity(postEntity);
        httpPost.setHeader(AS4MessagingHandler.HTTP_CONTENT_TYPE_HEADER, "multipart/related; boundary=\"MIMEBoundary_70d7c4906bec8b568135451bd3ab7fee123962d07f1566df\"; type=\""+contentType+"\"; start=\"<0.60d7c4906bec8b568135451bd3ab7fee123962d07f1566df@apache.org>\"");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        byte[] responseContent = null;
        try {
            response = httpclient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            responseContent = EntityUtils.toByteArray(responseEntity);
        } catch (Exception e) {
            logger.error("Error POSTing to AS4 backend", e);
            throw e;
        } finally {
            if (response != null) {
                response.close();
            }
        }
        // Update message to return to sender.
        configurations.add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_METHOD_CONFIG_NAME, AS4MessagingHandler.HTTP_METHOD));
        MapType headers = new MapType();
        for (Header header: response.getAllHeaders()) {
            headers.addItem(header.getName(), new StringType(header.getValue()));
        }
        BinaryType responseMessageContent = new BinaryType();
        responseMessageContent.setValue(responseContent);
        // Get SOAP envelope.
        mimeBody = AS4MessagingHandler.createMimeBody(headers, responseMessageContent);
        message.getFragments().put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, responseMessageContent);
        message.getFragments().put(AS4MessagingHandler.RECEIPT_SOAP_RESPONSE_FIELD_NAME, new ObjectType(AS4MessagingHandler.getSoapEnvelope(mimeBody).getSOAPPart().getEnvelope()));
        message.getFragments().put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, headers);
    }

    /**
     * Download the sent message from the backend reference implementation.
     *
     * @param configurations The input configuration.
     * @param message The input message.
     * @throws Exception
     */
    private void downloadMessageFromReferenceImplementation(List<Configuration> configurations, Message message) throws Exception {
        // Prepare POST.
        Configuration domibusReceiverConfiguration = ConfigurationUtils.getConfiguration(configurations, AS4MessagingHandler.DOMIBUS_ADDRESS_CONFIG_NAME);
        HttpPost domibusPost = new HttpPost(domibusReceiverConfiguration.getValue());
        ObjectType domibusRequest = (ObjectType) message.getFragments().get(AS4MessagingHandler.DOMIBUS_REQUEST_FIELD_NAME);
        Document document = (Document) domibusRequest.getValue();
        String domibusRequestAsString = ((org.w3c.dom.ls.DOMImplementationLS)document.getImplementation()).createLSSerializer().writeToString(document);
        ByteArrayEntity domibusEntity = new ByteArrayEntity(domibusRequestAsString.getBytes());
        domibusPost.setEntity(domibusEntity);
        // Execute POST.
        CloseableHttpClient domibusHttpclient = HttpClients.createDefault();
        CloseableHttpResponse domibusResponse = null;
        byte[] domibusResponseContent = null;
        String responseContentType = null;
        try {
            domibusResponse = domibusHttpclient.execute(domibusPost);
            HttpEntity responseEntity = domibusResponse.getEntity();
            Header[] headers = domibusResponse.getHeaders("Content-Type");
            if (headers.length > 0) {
                responseContentType = headers[0].getValue();
            }
            domibusResponseContent = EntityUtils.toByteArray(responseEntity);
        } catch (Exception e) {
            logger.error("Error POSTing to AS4 backend", e);
            throw e;
        } finally {
            if (domibusResponse != null) {
                domibusResponse.close();
            }
        }
        // Extract payload bytes.
        MimeBodyPart mimeBody = new MimeBodyPart();
        mimeBody.setDataHandler(new DataHandler(new ByteArrayDataSource(domibusResponseContent, responseContentType, null)));

        SOAPMessage domibusSoapMessage = AS4MessagingHandler.getSoapEnvelope(mimeBody);

        BinaryType domibusResponseMessageContent = new BinaryType();
        domibusResponseMessageContent.setValue(domibusResponseContent);

        // Envelope/Body/bodyLoad.
        InputSource source = new InputSource(new ByteArrayInputStream(AS4MessagingHandler.getBase64AsBytes(getDomibusResponseBusinessPayload(domibusSoapMessage))));
        Document domibusResponsePayloadDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);

        // Add to the communication data the response from the Domibus backend.
        MapType domibusPlaceholder = (MapType) message.getFragments().get(AS4MessagingHandler.AS4_COMMUNICATION_DATA_FIELD_NAME);
        domibusPlaceholder.addItem(AS4MessagingHandler.DOMIBUS_RAW_RESPONSE_FIELD_NAME, domibusResponseMessageContent);
        domibusPlaceholder.addItem(AS4MessagingHandler.DOMIBUS_SOAP_RESPONSE_FIELD_NAME, new ObjectType(domibusSoapMessage.getSOAPPart().getEnvelope()));
        domibusPlaceholder.addItem(AS4MessagingHandler.DOMIBUS_RESPONSE_PAYLOAD_FIELD_NAME, new ObjectType(domibusResponsePayloadDocument));
    }

    /**
     * Get the base64 content for the payload of the download message response.
     *
     * @param domibusSoapMessage The SOAP message.
     * @return The base64 payload.
     * @throws javax.xml.soap.SOAPException
     */
    private String getDomibusResponseBusinessPayload(SOAPMessage domibusSoapMessage) throws SOAPException {
        SOAPElement messageResponseNode = AS4MessagingHandler.getMandatoryChild(domibusSoapMessage.getSOAPBody(), null, "downloadMessageResponse");
        SOAPElement bodyloadNode = AS4MessagingHandler.getMandatoryChild(messageResponseNode, null, "bodyload");
        return bodyloadNode.getTextContent();
    }

    @Override
    public Message send(List<Configuration> configurations, Message message) throws Exception {
        // Send AS4 message to reference implementation.
        sendAS4MessageToReferenceImplementation(configurations, message);
        // Get message from backend Domibus application.
        downloadMessageFromReferenceImplementation(configurations, message);
        // Return response.
        super.send(configurations, message);
        return message;
    }

}
