package com.gitb.tbs.impl;

import com.gitb.engine.ITestbedServiceCallbackHandler;
import com.gitb.tbs.TestbedClient;
import com.gitb.tbs.TestbedClient_Service;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.xml.ws.WebServiceContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tuncay on 9/24/14.
 */
public class TestbedServiceCallbackHandler implements ITestbedServiceCallbackHandler {
    private static final String SOAP_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope";
    private static final String TESTBED_CLIENT_NODE = "TestbedClient";

    private static final Logger LOG = LoggerFactory.getLogger(TestbedServiceCallbackHandler.class);

    private static TestbedServiceCallbackHandler instance = null;
    private Map<String, WSAddresingProperties> sessionCallbackMap;

    public synchronized static TestbedServiceCallbackHandler getInstance() {
        if(instance == null) {
            instance = new TestbedServiceCallbackHandler();
        }
        return instance;
    }

    private TestbedServiceCallbackHandler(){
        sessionCallbackMap = new ConcurrentHashMap<>();
    }

    /**
     * Store the WS Addressing properties (replyTo and messageId) so that we can call callbacks later with the given session id
     * @param sessionId The session ID.
     * @param wsc The web service context.
     */
    void saveWSAddressingProperties(String sessionId, WebServiceContext wsc){
        //Process SOAP Header to find WS Addressing properties
        String testbedClientURL = getTestbedClientURL(getHeaders(wsc));
        //Put into the map
        sessionCallbackMap.put(sessionId, new WSAddresingProperties(testbedClientURL));
    }

    @Override
    public TestbedClient getTestbedClient(String sessionId){
        WSAddresingProperties wsAddresingProperties = sessionCallbackMap.get(sessionId);
        return wsAddresingProperties.getTestbedClient();
    }

    @Override
    public void releaseTestbedClient(String sessionId) {
        sessionCallbackMap.remove(sessionId);
    }

    private String getTestbedClientURL(List<Header> headers) {
        if (headers != null) {
            for (Header header: headers) {
                Element headerElement = ((Element)header.getObject());
                if (SOAP_NAMESPACE.equals(headerElement.getNamespaceURI()) && TESTBED_CLIENT_NODE.equals(headerElement.getLocalName())) {
                    return headerElement.getTextContent();
                }
            }
        }
        LOG.error("SOAP headers did not include test bed client URI.");
        return null;
    }

    /**
     * Retrieves the headers
     * @return The list of headers
     */
    private List<Header> getHeaders(WebServiceContext wsc) {
        if (wsc != null && wsc.getMessageContext() instanceof WrappedMessageContext) {
            return CastUtils.cast((List<?>)((WrappedMessageContext)wsc.getMessageContext()).getWrappedMessage().get(Header.HEADER_LIST));
        }
        throw new IllegalStateException("Headers could not be retrieved from web service call");
    }

    private class WSAddresingProperties {
        private TestbedClient testbedClient;

        private WSAddresingProperties(String testbedClientURL) {
            TestbedClient_Service testbedClientService;
            try {
                testbedClientService = new TestbedClient_Service(new URL(testbedClientURL));
            } catch (MalformedURLException e) {
                testbedClientService = new TestbedClient_Service();
            }
            this.testbedClient = testbedClientService.getTestbedClientPort();
        }

        TestbedClient getTestbedClient() {
            return testbedClient;
        }
    }

}
