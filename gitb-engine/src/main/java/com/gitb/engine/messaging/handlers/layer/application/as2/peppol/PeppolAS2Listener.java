package com.gitb.engine.messaging.handlers.layer.application.as2.peppol;

import com.gitb.core.Configuration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpMessagingHandler;
import com.gitb.engine.messaging.handlers.layer.application.https.HttpsListener;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.types.ObjectType;
import com.gitb.utils.XMLUtils;
import org.apache.http.impl.BHttpConnectionBase;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by root on 09.04.2015.
 */
public class PeppolAS2Listener extends HttpsListener{
    private static final String DEFAULT_SBD_NAMESPACE = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader";
    private static final String SBD_NODE_NAME = "StandardBusinessDocument";

    public PeppolAS2Listener(SessionContext session, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        super(session, receiverTransactionContext, senderTransactionContext);
    }

    //overrided to send empty configurations, since sender does not need any.
    @Override
    public List<Configuration> transformConfigurations(Message incomingMessage, List<Configuration> configurations) {
        return new ArrayList<>();
    }

    @Override
    public Message transformMessage(Message incomingMessage) throws Exception {
        //use the connection retrieved from the transaction
        BHttpConnectionBase connection = receiverTransactionContext.getParameter(BHttpConnectionBase.class);

        //connection is a server connection and will receive AS2 message
        if (connection instanceof DefaultBHttpServerConnection) {
            return transformAS2Message(incomingMessage);
        }

        //connection has sent an AS2 message and will receive AS2 MDN
        if(connection instanceof DefaultBHttpClientConnection) {
            return transformMDN(incomingMessage);
        }

        //not likely to happen
        throw new GITBEngineInternalError("Unexpected HTTP connection type");
    }

    private Message transformAS2Message(Message incomingMessage) throws Exception {
        Message message = new Message();

        //get separated standard business document header and business message
        ObjectType businessHeader  = (ObjectType) incomingMessage.getFragments().get(PeppolAS2MessagingHandler.BUSINESS_HEADER_FIELD_NAME);
        ObjectType businessMessage = (ObjectType) incomingMessage.getFragments().get(PeppolAS2MessagingHandler.BUSINESS_MESSAGE_FIELD_NAME);

        //merge SBDH and business message
        DocumentBuilderFactory factory = XMLUtils.getSecureDocumentBuilderFactory();
        Document doc = factory.newDocumentBuilder().newDocument();

        Element root = doc.createElementNS(DEFAULT_SBD_NAMESPACE, SBD_NODE_NAME);
        root.appendChild(doc.importNode((Node) businessHeader.getValue(),  true));
        root.appendChild(doc.importNode((Node) businessMessage.getValue(), true));
        doc.appendChild(root);

        //add standard business document
        ObjectType sbd = new ObjectType(root);
        message.getFragments().put(PeppolAS2MessagingHandler.BUSINESS_DOCUMENT_FIELD_NAME, sbd);

        //add http headers directly
        message.getFragments().put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, incomingMessage.getFragments().get(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME));

        return message;
    }

    private Message transformMDN(Message incomingMessage) {
        return incomingMessage;
    }
}
