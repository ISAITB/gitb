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

package jaxws;

import config.Configurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;


public class HeaderHandler implements SOAPHandler<SOAPMessageContext> {

    private static final String SOAP_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope";
    private static final String TESTBED_CLIENT_NODE = "TestbedClient";
    private static final Logger logger = LoggerFactory.getLogger(HeaderHandler.class);

    public boolean handleMessage(SOAPMessageContext smc) {
        try {
            SOAPEnvelope envelope = smc.getMessage().getSOAPPart().getEnvelope();
            SOAPHeader header = envelope.getHeader();

            //add TestbedClient address as a custom header, so that TestbedService will now where it is
            SOAPElement client = header.addHeaderElement(new QName(SOAP_NAMESPACE, TESTBED_CLIENT_NODE));

            if(Configurations.TESTBED_CLIENT_URL().endsWith("?wsdl")){
                client.addTextNode(Configurations.TESTBED_CLIENT_URL());
            } else{
                client.addTextNode(Configurations.TESTBED_CLIENT_URL() + "?wsdl");
            }

        } catch (Exception e) {
            logger.error("Error in HeaderHandler", e);
        }

        return true;
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