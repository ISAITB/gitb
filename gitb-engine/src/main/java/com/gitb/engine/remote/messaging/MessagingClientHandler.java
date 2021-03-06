package com.gitb.engine.remote.messaging;

import com.gitb.engine.TestEngineConfiguration;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by simatosc on 28/11/2016.
 */
public class MessagingClientHandler implements SOAPHandler<SOAPMessageContext> {

    private static QName REPLY_TO_NAME = new QName("http://www.w3.org/2005/08/addressing", "ReplyTo");
    private static QName ADDRESS_NAME = new QName("http://www.w3.org/2005/08/addressing", "Address");

    @Override
    public Set<QName> getHeaders() {
        return Collections.EMPTY_SET;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outboundProperty = (Boolean)context.get (MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (outboundProperty) {
            try {
                SOAPHeader header = context.getMessage().getSOAPHeader();
                Iterator it = header.getChildElements(REPLY_TO_NAME);
                Element replyToElement = (Element) it.next();
                NodeList nodeList = replyToElement.getElementsByTagNameNS(ADDRESS_NAME.getNamespaceURI(), ADDRESS_NAME.getLocalPart());
                if (nodeList.getLength() > 0) {
                    Element addressElement = (Element)nodeList.item(0);
                    addressElement.setTextContent(TestEngineConfiguration.MESSAGING_CALLBACK_URL);
                }
                context.getMessage().saveChanges();
            } catch (SOAPException e) {
                throw new IllegalStateException(e);
            }
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

}
