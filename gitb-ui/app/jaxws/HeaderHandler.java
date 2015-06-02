package jaxws;

import config.Configurations;

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
            e.printStackTrace();
        }

        return true;
    }

    public Set getHeaders() {
        final HashSet headers = new HashSet();
        return headers;
    }

    public boolean handleFault(SOAPMessageContext context) {
        //throw new UnsupportedOperationException("Not supported yet.");
        return true;
    }

    public void close(MessageContext context) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }
}