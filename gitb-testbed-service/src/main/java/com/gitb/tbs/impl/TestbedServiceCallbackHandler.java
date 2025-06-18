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

package com.gitb.tbs.impl;

import com.gitb.engine.ITestbedServiceCallbackHandler;
import com.gitb.tbs.TestbedClient;
import com.gitb.tbs.TestbedClient_Service;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.net.MalformedURLException;
import java.net.URI;
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
    private final Map<String, WSAddressingProperties> sessionCallbackMap;

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
        String testbedClientURL = getTestbedClientURL(wsc);
        //Put into the map
        sessionCallbackMap.put(sessionId, new WSAddressingProperties(testbedClientURL));
    }

    @Override
    public TestbedClient getTestbedClient(String sessionId){
        WSAddressingProperties wsAddressingProperties = sessionCallbackMap.get(sessionId);
        if (wsAddressingProperties != null) {
            return wsAddressingProperties.getTestbedClient();
        }
        return null;
    }

    @Override
    public void releaseTestbedClient(String sessionId) {
        sessionCallbackMap.remove(sessionId);
    }

    public static String getTestbedClientURL(WebServiceContext wsc) {
        return getTestbedClientURL(getHeaders(wsc));
    }

    public static TestbedClient createTestBedClient(WebServiceContext wsc) {
        return createTestBedClient(getTestbedClientURL(wsc));
    }

    public static TestbedClient createTestBedClient(String testbedClientURL) {
        TestbedClient_Service testbedClientService;
        try {
            testbedClientService = new TestbedClient_Service(URI.create(testbedClientURL).toURL());
        } catch (MalformedURLException e) {
            testbedClientService = new TestbedClient_Service();
        }
        return testbedClientService.getTestbedClientPort();
    }

    private static String getTestbedClientURL(List<Header> headers) {
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
    private static List<Header> getHeaders(WebServiceContext wsc) {
        if (wsc != null && wsc.getMessageContext() instanceof WrappedMessageContext) {
            return CastUtils.cast((List<?>)((WrappedMessageContext)wsc.getMessageContext()).getWrappedMessage().get(Header.HEADER_LIST));
        }
        throw new IllegalStateException("Headers could not be retrieved from web service call");
    }

    private static class WSAddressingProperties {
        private final TestbedClient testbedClient;

        private WSAddressingProperties(String testbedClientURL) {
            this.testbedClient = createTestBedClient(testbedClientURL);
        }

        TestbedClient getTestbedClient() {
            return testbedClient;
        }
    }

}
