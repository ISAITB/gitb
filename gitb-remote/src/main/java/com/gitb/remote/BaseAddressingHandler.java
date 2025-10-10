/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.remote;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.Set;

public abstract class BaseAddressingHandler implements SOAPHandler<SOAPMessageContext>  {

    private static final QName REPLY_TO_NAME = new QName("http://www.w3.org/2005/08/addressing", "ReplyTo");
    private static final QName ADDRESS_NAME = new QName("http://www.w3.org/2005/08/addressing", "Address");

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        String callbackUrl = callbackURL();
        if (callbackUrl != null && !callbackUrl.isEmpty()) {
            Boolean outboundProperty = (Boolean)context.get (MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            if (outboundProperty) {
                try {
                    SOAPHeader header = context.getMessage().getSOAPHeader();
                    var it = header.getChildElements(REPLY_TO_NAME);
                    Element replyToElement = (Element) it.next();
                    NodeList nodeList = replyToElement.getElementsByTagNameNS(ADDRESS_NAME.getNamespaceURI(), ADDRESS_NAME.getLocalPart());
                    if (nodeList.getLength() > 0) {
                        Element addressElement = (Element)nodeList.item(0);
                        addressElement.setTextContent(callbackURL());
                    }
                } catch (SOAPException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {}

    protected abstract String callbackURL();

}
