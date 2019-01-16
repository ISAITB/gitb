package jaxws;

import config.Configurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;


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