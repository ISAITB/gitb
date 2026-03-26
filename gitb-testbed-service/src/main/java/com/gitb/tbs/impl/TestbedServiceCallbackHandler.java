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

package com.gitb.tbs.impl;

import com.gitb.engine.ITestbedServiceCallbackHandler;
import com.gitb.tbs.TestbedClient;
import com.gitb.tbs.TestbedClient_Service;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.frontend.ClientProxy;
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

    private final Map<String, TestbedClient> frontEndToClientMap;
    private final Map<String, TestbedClient> sessionToClientMap;

    public synchronized static TestbedServiceCallbackHandler getInstance() {
        if (instance == null) {
            instance = new TestbedServiceCallbackHandler();
        }
        return instance;
    }

    private TestbedServiceCallbackHandler() {
        frontEndToClientMap = new ConcurrentHashMap<>();
        sessionToClientMap = new ConcurrentHashMap<>();
    }

    public TestbedClient createClient(WebServiceContext clientContext) {
        return createClient(null, clientContext);
    }

    public TestbedClient createClient(String sessionId, WebServiceContext clientContext) {
        // The callback address is the GITB-UI component.
        // There is no need to cleanup this cache because there will normally be only one client.
        String callbackAddress = getTestbedClientURL(clientContext);
        var frontEndClient = frontEndToClientMap.computeIfAbsent(callbackAddress, key -> createTestBedClient(callbackAddress));
        if (sessionId != null) {
            sessionToClientMap.put(sessionId, frontEndClient);
        }
        return frontEndClient;
    }

    public void destroy() {
        sessionToClientMap.clear();
        frontEndToClientMap.values().forEach(client -> {
            try {
                ClientProxy.getClient(client).destroy();
            } catch (Exception e) {
                // Ignore client cleanup issues.
            }
        });
        frontEndToClientMap.clear();
    }

    @Override
    public TestbedClient getTestbedClient(String sessionId) {
        return sessionToClientMap.get(sessionId);
    }

    @Override
    public void releaseTestbedClient(String sessionId) {
        sessionToClientMap.remove(sessionId);
    }

    private String getTestbedClientURL(WebServiceContext wsc) {
        return getTestbedClientURL(getHeaders(wsc));
    }

    private TestbedClient createTestBedClient(String testbedClientURL) {
        TestbedClient_Service testbedClientService;
        try {
            testbedClientService = new TestbedClient_Service(URI.create(testbedClientURL).toURL());
        } catch (MalformedURLException e) {
            testbedClientService = new TestbedClient_Service();
        }
        return testbedClientService.getTestbedClientPort();
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

}
