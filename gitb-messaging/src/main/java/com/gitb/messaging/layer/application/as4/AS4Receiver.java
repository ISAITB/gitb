package com.gitb.messaging.layer.application.as4;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.layer.application.http.HttpReceiver;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.BinaryType;
import com.gitb.types.MapType;
import com.gitb.types.ObjectType;
import com.gitb.types.StringType;

import javax.mail.internet.MimeBodyPart;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import java.util.List;

/**
 * Class handling the reception of AS4 messages.
 *
 * Created by simatosc on 02/04/2015.
 */
public class AS4Receiver extends HttpReceiver {

    public AS4Receiver(SessionContext sessionContext, TransactionContext transactionContext) {
        super(sessionContext, transactionContext);
    }

    @Override
    public Message receive(List<Configuration> configurations) throws Exception {
        Message received = super.receive(configurations);
        Message message = new Message();

        MapType headers = (MapType) received.getFragments().get(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME);
        BinaryType body = (BinaryType) received.getFragments().get(HttpMessagingHandler.HTTP_BODY_FIELD_NAME);

        // Put data into a MimeBody
        MimeBodyPart mimeBody = AS4MessagingHandler.createMimeBody(headers, body);

        StringType as4MessageID = null;
        ObjectType businessMessage = null;
        SOAPMessage soapMessage = AS4MessagingHandler.getSoapEnvelope(mimeBody);
        if(soapMessage != null) {
            // Capture entire business message.
            businessMessage = getBusinessMessage(soapMessage.getSOAPBody());
            // Capture message ID.
            as4MessageID = getMessageID(soapMessage.getSOAPHeader());
        }
        //convert MimeBody into a DataType
        BinaryType rawMessage = AS4MessagingHandler.getRawMessage(mimeBody);

        message.getFragments().put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, headers);
        message.getFragments().put(AS4MessagingHandler.RAW_MESSAGE_FIELD_NAME, rawMessage);
        if (soapMessage != null) {
            message.getFragments().put(AS4MessagingHandler.SOAP_ENVELOPE_FIELD_NAME, new ObjectType(soapMessage.getSOAPPart().getEnvelope()));
        }
        message.getFragments().put(AS4MessagingHandler.AS4_MESSAGE_ID_FIELD_NAME, as4MessageID);
        message.getFragments().put(AS4MessagingHandler.BUSINESS_MESSAGE_FIELD_NAME, businessMessage);
        return message;
    }

    /**
     * Extract the message ID from the message header.
     *
     * @param header The SOAP header.
     * @return The message ID.
     */
    private StringType getMessageID(SOAPHeader header) {
        SOAPElement messagingNode = AS4MessagingHandler.getMandatoryChild(header, AS4MessagingHandler.EB_URI, "Messaging");
        SOAPElement userMessageNode = AS4MessagingHandler.getMandatoryChild(messagingNode, AS4MessagingHandler.EB_URI, "UserMessage");
        SOAPElement messageInfoNode = AS4MessagingHandler.getMandatoryChild(userMessageNode, AS4MessagingHandler.EB_URI, "MessageInfo");
        SOAPElement messageIdNode = AS4MessagingHandler.getMandatoryChild(messageInfoNode, AS4MessagingHandler.EB_URI, "MessageId");
        return new StringType(messageIdNode.getTextContent());
    }

    /**
     * Extract the business payload of the SOAP body.
     *
     * @param body The SOAP body.
     * @return The payload.
     */
    private ObjectType getBusinessMessage(SOAPBody body) {
        return new ObjectType(body.getFirstChild());
    }

}
