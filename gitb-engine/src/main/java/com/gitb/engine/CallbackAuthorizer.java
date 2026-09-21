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

package com.gitb.engine;

import com.gitb.PropertyConstants;
import com.gitb.core.ActorConfiguration;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.tdl.HandlerApiType;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used as a central point for authorization checks on test service callbacks received during test sessions.
 */
public class CallbackAuthorizer {

    private static final CallbackAuthorizer INSTANCE = new CallbackAuthorizer();
    private static final String HEADER_NAME = "ITB-API-KEY";
    private static final Logger LOG = LoggerFactory.getLogger(CallbackAuthorizer.class);

    private volatile boolean soapCallbacksEnabled = true;
    private volatile boolean restCallbacksEnabled = true;
    private volatile boolean callbackApiKeysEnabled = false;

    public static CallbackAuthorizer getInstance() {
        return INSTANCE;
    }

    public boolean areCallbackApiKeysEnabled() {
        return callbackApiKeysEnabled;
    }

    public void updateSettings(ActorConfiguration settings) {
        if (settings != null) {
            boolean allowRestCallbacks = settings.getConfig().stream().filter(x -> PropertyConstants.TEST_SERVICE_CALLBACKS_REST_ENABLED.equals(x.getName())).findAny().map(x -> Boolean.parseBoolean(x.getValue())).orElse(true);
            boolean allowSoapCallbacks = settings.getConfig().stream().filter(x -> PropertyConstants.TEST_SERVICE_CALLBACKS_SOAP_ENABLED.equals(x.getName())).findAny().map(x -> Boolean.parseBoolean(x.getValue())).orElse(true);
            boolean requireCallbackApiKeys = settings.getConfig().stream().filter(x -> PropertyConstants.TEST_SERVICE_CALLBACKS_API_KEYS_ENABLED.equals(x.getName())).findAny().map(x -> Boolean.parseBoolean(x.getValue())).orElse(false);
            updateSettings(allowSoapCallbacks, allowRestCallbacks, requireCallbackApiKeys);
        }
    }

    /**
     * Update the basic callback-related flags regarding the enabled APIs and expectance of API keys.
     *
     * @param soapCallbacksEnabled Whether the SOAP API is enabled.
     * @param restCallbacksEnabled Whether the REST API is enabled.
     * @param callbackApiKeysEnabled  Whether callback API keys are enabled.
     */
    public void updateSettings(boolean soapCallbacksEnabled, boolean restCallbacksEnabled, boolean callbackApiKeysEnabled) {
        this.soapCallbacksEnabled = soapCallbacksEnabled;
        this.restCallbacksEnabled = restCallbacksEnabled;
        this.callbackApiKeysEnabled = callbackApiKeysEnabled;
    }

    private String getApiKey(HttpServletRequest request) {
        return request.getHeader(HEADER_NAME);
    }

    private void raiseError(String message) {
        LOG.warn(message);
        throw new CallbackAuthorizationException(message);
    }

    /**
     * Check method called at the level of HTTP request processing, before we do protocol-specific parsing.
     * <br>
     * This is called by HTTP filters as early as possible in the processing chain.
     *
     * @param apiType The API type.
     * @param request The request.
     */
    public void checkRequestAccepted(HandlerApiType apiType, HttpServletRequest request) {
        if (apiType == HandlerApiType.REST && !restCallbacksEnabled) {
            // REST callback received when REST endpoint is not enabled.
            raiseError("The REST callback API is not enabled.");
        } else if (apiType == HandlerApiType.SOAP && !soapCallbacksEnabled) {
            // SOAP callback received when SOAP endpoint is not enabled.
            raiseError("The SOAP callback API is not enabled.");
        } else if (!SessionManager.getInstance().hasActiveSessions()) {
            // There are no active sessions so callbacks are never accepted.
            raiseError("Callback rejected as no sessions are active.");
        } else if (callbackApiKeysEnabled) {
            // API keys are required.
            String apiKey = getApiKey(request);
            if (apiKey == null) {
                // No API key header was provided.
                raiseError("Callback rejected due to missing %s header.".formatted(HEADER_NAME));
            } else if (!SessionManager.getInstance().isApiKeyExpectedForAnyTestSession(apiKey)) {
                // The provided API key is related to *one* of the actively running test sessions.
                // We do this check here to catch an invalid API key early, without needing to process
                // the received payload to determine the relevant test session ID.
                raiseError("Callback rejected due to invalid %s header.".formatted(HEADER_NAME));
            }
        }
    }

    /**
     * Check method called once we know the test session a callback refers to.
     * <br>
     * Checks here take place after payload processing, and focus on ensuring a received API key
     * (if API keys are enabled) matches an API key expected for the given test session.
     *
     * @param testSessionId The test session ID.
     * @param request The HTTP request.
     */
    public void checkApiKeyAccepted(String testSessionId, HttpServletRequest request) {
        if (callbackApiKeysEnabled) {
            // API keys are required.
            String apiKey = getApiKey(request);
            if (apiKey == null) {
                // No API key header was provided. This is a sanity check as this should have already been checked.
                raiseError("Callback rejected due to missing %s header.".formatted(HEADER_NAME));
            } else {
                TestCaseContext context = SessionManager.getInstance().getContext(testSessionId);
                if (context == null) {
                    // Test session could not be determined for the provided test session ID.
                    raiseError("Test session not found for identifier [%s].".formatted(testSessionId));
                } else if (!context.isApiKeyExpectedForTestSession(apiKey)) {
                    // Check that the specific API key is expected for the specific test session ID.
                    raiseError("Callback rejected due to invalid %s header.".formatted(HEADER_NAME));
                }
            }
        }
    }

    /**
     * Marker exception thrown when a callback authorization error is detected.
     */
    public static class CallbackAuthorizationException extends RuntimeException {

        public CallbackAuthorizationException(String message) {
            super(message);
        }

    }

}
