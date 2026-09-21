/*
 * Copyright (C) 2026 European Union
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

package jaxws;

import com.gitb.utils.HmacUtils;
import config.Configurations;
import jakarta.xml.soap.SOAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import javax.xml.namespace.QName;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

public class HeaderHandler implements SOAPHandler<SOAPMessageContext> {

    private static final String SOAP_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String TESTBED_CLIENT_NODE = "TestbedClient";
    private static final Logger logger = LoggerFactory.getLogger(HeaderHandler.class);

    public boolean handleMessage(SOAPMessageContext smc) {
        Boolean outbound = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (!Boolean.TRUE.equals(outbound)) {
            return true;
        }
        try {
            addTestbedClientHeader(smc);
            addHttpHeaders(smc);
        } catch (Exception e) {
            logger.error("Error in HeaderHandler", e);
        }
        return true;
    }

    private void addTestbedClientHeader(SOAPMessageContext smc) throws SOAPException {
        SOAPEnvelope envelope = smc.getMessage().getSOAPPart().getEnvelope();
        SOAPHeader header = envelope.getHeader();
        SOAPElement client = header.addHeaderElement(new QName(SOAP_NAMESPACE, TESTBED_CLIENT_NODE));
        String url = Configurations.TESTBED_CLIENT_URL();
        client.addTextNode(url.endsWith("?wsdl") ? url : url + "?wsdl");
    }

    private void addHttpHeaders(SOAPMessageContext smc) {
        Map<String, List<String>> headers = new HashMap<>();
        HmacUtils.TokenData tokenData = HmacUtils.getTokenData("POST|/TestbedService");
        headers.put(HmacUtils.HMAC_HEADER_TOKEN, Collections.singletonList(tokenData.getTokenValue()));
        headers.put(HmacUtils.HMAC_HEADER_TIMESTAMP, Collections.singletonList(tokenData.getTokenTimestamp()));
        smc.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
        smc.setScope(MessageContext.HTTP_REQUEST_HEADERS, MessageContext.Scope.APPLICATION);
    }

    public Set<QName> getHeaders() {
        return new HashSet<>();
    }

    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    public void close(MessageContext context) {
    }
}