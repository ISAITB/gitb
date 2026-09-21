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

package com.gitb.tbs.filters;

import com.gitb.engine.CallbackAuthorizer;
import com.gitb.tdl.HandlerApiType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Filter bound to the REST and SOAP API test service callback endpoints, used as a first layer of authorization
 * for callbacks.
 */
public class CallbackAuthorizationFilter extends BaseAuthorizationFilter {

    private final HandlerApiType apiType;

    public CallbackAuthorizationFilter(HandlerApiType apiType) {
        this.apiType = apiType;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (apiType == HandlerApiType.SOAP && isSoapMetadataRequest(request)) {
            // Don't restrict WSDL lookup calls.
            chain.doFilter(request, response);
            return;
        }
        try {
            CallbackAuthorizer.getInstance().checkRequestAccepted(apiType, request);
        } catch (CallbackAuthorizer.CallbackAuthorizationException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Callback request denied");
            return;
        }
        chain.doFilter(request, response);
    }

}
