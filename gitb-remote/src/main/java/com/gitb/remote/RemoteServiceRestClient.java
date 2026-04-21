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

package com.gitb.remote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitb.PropertyConstants;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

import static com.gitb.CoreConfiguration.GITB_REST_CALLBACK_API_ROOT;
import static com.gitb.CoreConfiguration.TEST_ENGINE_VERSION;

public abstract class RemoteServiceRestClient {

    protected static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private final Properties callProperties;
    protected final String testSessionId;
    protected final Supplier<ClientConfiguration> clientConfigurationProvider;
    protected URI serviceURL;

    protected RemoteServiceRestClient(URI serviceURL, Properties callProperties, String sessionId, String testCaseIdentifier, Supplier<ClientConfiguration> clientConfigurationProvider) {
        this.serviceURL = serviceURL;
        this.testSessionId = sessionId;
        this.clientConfigurationProvider = clientConfigurationProvider;
        this.callProperties = Objects.requireNonNullElseGet(callProperties, Properties::new);
        if (sessionId != null) {
            this.callProperties.put(PropertyConstants.TEST_SESSION_ID, sessionId);
        }
        if (testCaseIdentifier != null) {
            this.callProperties.put(PropertyConstants.TEST_CASE_ID, testCaseIdentifier);
        }
    }

    protected abstract String getServiceLocation();

    protected <X, Y> Optional<Y> call(String method, String operationPath, Optional<X> requestBody, Optional<Class<Y>> responseType) {
        return call(method, operationPath, requestBody, responseType, null);
    }

    protected <X, Y> Optional<Y> call(String method, String operationPath, Optional<X> requestBody, Optional<Class<Y>> responseType, Map<String, String> extraCallProperties) {
        return call(method, operationPath, requestBody, responseType, extraCallProperties, true);
    }

    protected <X, Y> Optional<Y> call(String method, String operationPath, Optional<X> requestBody, Optional<Class<Y>> responseType, Map<String, String> extraCallProperties, boolean configureTimeouts) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(resolveOperationPath(operationPath))
                .method(method, requestBody.map(x -> {
                    try {
                        return HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(x));
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Unable to serialise request as JSON", e);
                    }
                }).orElse(HttpRequest.BodyPublishers.noBody()));
        if (requestBody.isPresent()) {
            requestBuilder.header("Content-Type", "application/json");
        }
        // Handle call properties
        var propertiesToUse = new Properties();
        propertiesToUse.putAll(callProperties);
        if (extraCallProperties != null) {
            propertiesToUse.putAll(extraCallProperties);
        }
        handleCallProperties(requestBuilder, propertiesToUse);
        // Build client
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(30000L))
                .followRedirects(HttpClient.Redirect.ALWAYS);
        if (configureTimeouts && clientConfigurationProvider != null) {
            var clientConfiguration = clientConfigurationProvider.get();
            if (clientConfiguration.timeout() != null) {
                long specificTimeout = clientConfiguration.timeout();
                if (specificTimeout > 0L) {
                    requestBuilder.timeout(Duration.ofMillis(specificTimeout));
                }
            }
        }
        // Make request
        try (HttpClient client = clientBuilder.build()) {
            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Remote service returned non-success status code: " + response.statusCode());
            }
            String body = response.body();
            if (body == null || body.isEmpty() || responseType.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(JSON.readValue(body, responseType.get()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected error while calling remote service", e);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private URI resolveOperationPath(String operationPath) {
        String baseUrl = serviceURL.toString();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return URI.create(baseUrl + operationPath);
    }

    private void handleCallProperties(HttpRequest.Builder requestBuilder, Properties callProperties) {
        // HTTP basic authentication
        String username = callProperties.getProperty(PropertyConstants.AUTH_BASIC_USERNAME);
        String password = callProperties.getProperty(PropertyConstants.AUTH_BASIC_PASSWORD);
        if (username != null && password != null) {
            String credentials = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            requestBuilder.header("Authorization", "Basic " + encoded);
        }
        // HTTP header authentication
        String headerName = callProperties.getProperty(PropertyConstants.AUTH_HEADER_NAME);
        String headerValue = callProperties.getProperty(PropertyConstants.AUTH_HEADER_VALUE);
        if (headerName != null && headerValue != null) {
            requestBuilder.header(headerName, headerValue);
        }
        // Other headers
        addCustomHeaders(requestBuilder, callProperties);
    }

    private void addCustomHeaders(HttpRequest.Builder requestBuilder, Properties callProperties) {
        requestBuilder.header("Gitb-Reply-To", GITB_REST_CALLBACK_API_ROOT);
        requestBuilder.header("Gitb-Test-Engine-Version", TEST_ENGINE_VERSION);
        if (callProperties.containsKey(PropertyConstants.TEST_SESSION_ID)) requestBuilder.header("Gitb-Test-Session-Identifier", callProperties.getProperty(PropertyConstants.TEST_SESSION_ID));
        if (callProperties.containsKey(PropertyConstants.TEST_CASE_ID)) requestBuilder.header("Gitb-Test-Case-Identifier", callProperties.getProperty(PropertyConstants.TEST_CASE_ID));
        if (callProperties.containsKey(PropertyConstants.TEST_STEP_ID)) requestBuilder.header("Gitb-Test-Step-Identifier", callProperties.getProperty(PropertyConstants.TEST_STEP_ID));
    }

    protected Map<String, String> stepIdMap(String stepId) {
        if (stepId != null) {
            return Map.of(PropertyConstants.TEST_STEP_ID, stepId);
        }
        return null;
    }

}
