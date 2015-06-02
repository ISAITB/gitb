package com.gitb.tbs.impl;

import com.gitb.engine.ITestbedServiceCallbackHandler;
import com.gitb.tbs.TestbedClient;
import com.gitb.tbs.TestbedClient_Service;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.developer.JAXWSProperties;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tuncay on 9/24/14.
 */
public class TestbedServiceCallbackHandler implements ITestbedServiceCallbackHandler {
    private static final String SOAP_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope";
    private static final String TESTBED_CLIENT_NODE = "TestbedClient";

    private static TestbedServiceCallbackHandler instance = null;
    private Map<String, WSAddresingProperties> sessionCallbackMap;
    private TestbedClient port;

    public synchronized static TestbedServiceCallbackHandler getInstance() {
        if(instance == null) {
            instance = new TestbedServiceCallbackHandler();
        }
        return instance;
    }

    protected TestbedServiceCallbackHandler(){
        sessionCallbackMap = new ConcurrentHashMap<>();
    }

    /**
     * Store the WS Addressing properties (replyTo and messageId) so that we can call callbacks later with the given session id
     * @param sessionId
     * @param wsc

    public void saveWSAddressingProperties(String sessionId, WebServiceContext wsc){
        //Process SOAP Header to find WS Addressing properties
        HeaderList headers = getHeaders(wsc);
        EndpointReference endpointReference = getReplyTo(headers);
        String messageId = getMessageId(headers);
        String testbedClientURL = getTestbedClientURL(headers);
        //Put into the map
        sessionCallbackMap.put(sessionId, new WSAddresingProperties(endpointReference, messageId, testbedClientURL));
    }*/

    /**
     * Store the WS Addressing properties (replyTo and messageId) so that we can call callbacks later with the given session id
     * @param sessionId
     * @param wsc
     */
    public void saveWSAddressingProperties(String sessionId, WebServiceContext wsc){
        //Process SOAP Header to find WS Addressing properties
        HeaderList headers = getHeaders(wsc);
        String testbedClientURL = getTestbedClientURL(headers);
        //Put into the map
        sessionCallbackMap.put(sessionId, new WSAddresingProperties(testbedClientURL));
    }

    @Override
    public TestbedClient getTestbedClient(String sessionId){
        /*
            WSAddresingProperties wsAddresingProperties = sessionCallbackMap.get(sessionId);
            //Construct Port
            TestbedClient_Service testbedClientService = new TestbedClient_Service();
            TestbedClient testbedClient = testbedClientService.getPort(wsAddresingProperties.getReplyTo(), TestbedClient.class);
            //Set message id
            ((WSBindingProvider)testbedClient)
                    .setOutboundHeaders(Headers.create(AddressingVersion.W3C.relatesToTag, wsAddresingProperties.getMessageId()));
            return testbedClient;
        */
        WSAddresingProperties wsAddresingProperties = sessionCallbackMap.get(sessionId);
        return wsAddresingProperties.getTestbedClient();
    }

    @Override
    public void releaseTestbedClient(String sessionId) {
        sessionCallbackMap.remove(sessionId);
    }

    private String getTestbedClientURL(HeaderList headers){
        return headers.get(new QName(SOAP_NAMESPACE, TESTBED_CLIENT_NODE), false).getStringContent();
    }

    /**
     * Retrieves the headers
     * @return
     */
    private HeaderList getHeaders(WebServiceContext wsc) {
        return (HeaderList)wsc.getMessageContext().get(JAXWSProperties.INBOUND_HEADER_LIST_PROPERTY);
    }

    /**
     * Grab WS-Addressing ReplyTo/Address header
     * @return

    private EndpointReference getReplyTo(HeaderList headers) {
        return headers.getReplyTo(AddressingVersion.W3C,
                SOAPVersion.SOAP_11).toSpec();
    }
     */

    /**
     * Grab WS-Addressing MessageID header
     * @return

    private String getMessageId(HeaderList headers) {
        return headers.getMessageID(AddressingVersion.W3C,
                SOAPVersion.SOAP_11);
    }
     */

    private class WSAddresingProperties {
        private String testbedClientURL;
        private TestbedClient testbedClient;

        private WSAddresingProperties(String testbedClientURL) {
            this.testbedClientURL = testbedClientURL;

            TestbedClient_Service testbedClientService = null;
            try {
                testbedClientService = new TestbedClient_Service(new URL(testbedClientURL));
            } catch (MalformedURLException e) {
                testbedClientService = new TestbedClient_Service();
            }
            this.testbedClient = testbedClientService.getTestbedClientPort();
        }

        public String getTestbedClientURL() {
            return testbedClientURL;
        }

        public TestbedClient getTestbedClient() {
            return testbedClient;
        }
    }

    /*
    private class WSAddresingProperties {
        private EndpointReference replyTo;
        private String messageId;
        private String testbedClientURL;

        private WSAddresingProperties(EndpointReference replyTo, String messageId, String testbedClientURL) {
            this.replyTo = replyTo;
            this.messageId = messageId;
            this.testbedClientURL = testbedClientURL;
        }

        public EndpointReference getReplyTo() {
            return replyTo;
        }

        public void setReplyTo(EndpointReference replyTo) {
            this.replyTo = replyTo;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getTestbedClientURL() {
            return testbedClientURL;
        }
    }*/

}
