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

import com.gitb.utils.HmacUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Filter that is used to authorise calls to the TestBedService endpoint.
 * <br>
 * Authorization takes place using the HMAC token and timestamp provided by the caller (gitb-ui).
 */
public class TestBedServiceAuthorizationFilter extends BaseAuthorizationFilter {

    private static final Logger LOG = LoggerFactory.getLogger(TestBedServiceAuthorizationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (isSoapMetadataRequest(request)) {
            // Don't HMAC-restrict WSDL lookup calls.
            chain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String expectedText = "%s|%s".formatted(request.getMethod(), path);
        boolean valid;
        try {
            valid = HmacUtils.isTokenValid(request.getHeader(HmacUtils.HMAC_HEADER_TOKEN), expectedText, request.getHeader(HmacUtils.HMAC_HEADER_TIMESTAMP));
        } catch (RuntimeException e) {
            LOG.warn("HMAC validation error: {}", e.getMessage());
            valid = false;
        }
        if (valid) {
            chain.doFilter(request, response);
        } else {
            LOG.warn("HMAC authorization rejected for {} {}", request.getMethod(), request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        }
    }

}
