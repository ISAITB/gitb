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

import com.gitb.engine.CallbackAuthorizer;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.cxf.interceptor.Fault;

/**
 * Base class for all test engine callback endpoint implementations.
 */
public abstract class BaseClientImpl {

    @Resource
    private WebServiceContext wsContext;

    /**
     * Check the API key linked to the current request contains an expected API key header for the given session ID.
     *
     * @param testSessionId The session ID to check for.
     */
    public void checkApiKey(String testSessionId) {
        MessageContext mc = wsContext.getMessageContext();
        HttpServletRequest request = (HttpServletRequest) mc.get(MessageContext.SERVLET_REQUEST);
        try {
            CallbackAuthorizer.getInstance().checkApiKeyAccepted(testSessionId, request);
        } catch (CallbackAuthorizer.CallbackAuthorizationException e) {
            Fault fault = new Fault(new Exception("Callback request denied"));
            fault.setStatusCode(HttpServletResponse.SC_FORBIDDEN);
            throw fault;
        }
    }

}
