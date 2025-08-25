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

package com.gitb.remote;

import com.gitb.PropertyConstants;
import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import java.net.*;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

public abstract class RemoteServiceClient {

    private final Properties callProperties;
    protected URL serviceURL;
    protected final String testSessionId;
    protected final Supplier<ClientConfiguration> clientConfigurationProvider;
    protected final boolean useAddressing;

    protected RemoteServiceClient(URL serviceURL, Properties callProperties, String sessionId, String testCaseIdentifier, Supplier<ClientConfiguration> clientConfigurationProvider, boolean useAddressing) {
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
        this.useAddressing = useAddressing;
    }

    protected RemoteServiceClient(URL serviceURL, Properties callProperties, String sessionId, String testCaseIdentifier, Supplier<ClientConfiguration> clientConfigurationProvider) {
        this(serviceURL, callProperties, sessionId, testCaseIdentifier, clientConfigurationProvider, true);
    }

    protected RemoteServiceClient(URL serviceURL, Properties callProperties, String sessionId, String testCaseIdentifier, ClientConfiguration clientConfiguration) {
        this(serviceURL, callProperties, sessionId, testCaseIdentifier, () -> clientConfiguration);
    }

    protected abstract String getServiceLocation();

    protected URL getServiceURL() {
        if (serviceURL == null) {
            var serviceLocation = getServiceLocation();
            if (serviceLocation == null) {
                throw new IllegalStateException("Remote service module found but with no provided URL or module definition");
            } else {
                try {
                    serviceURL = new URI(serviceLocation).toURL();
                } catch (MalformedURLException e) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote test module with an malformed URL ["+serviceLocation+"]"), e);
                } catch (URISyntaxException e) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote test module with an invalid URI syntax ["+serviceLocation+"]"), e);
                }
            }
        }
        return serviceURL;
    }

    protected <Y> Y call(Supplier<Y> supplier) {
        return call(supplier, null);
    }

    protected <Y> Y call(Supplier<Y> supplier, Map<String, String> extraCallProperties) {
        try {
            var propertiesToUse = new Properties();
            propertiesToUse.putAll(getCallProperties());
            if (extraCallProperties != null) {
                propertiesToUse.putAll(extraCallProperties);
            }
            RemoteCallContext.setCallProperties(propertiesToUse);
            return supplier.get();
        } catch (RuntimeException e) {
            if (isTimeoutException(e)) {
                throw new HandlerTimeoutException(e);
            } else {
                throw e;
            }
        } finally {
            RemoteCallContext.clearCallProperties();
        }
    }

    private boolean isTimeoutException(Throwable cause) {
        if (cause instanceof HttpTimeoutException) {
            return true;
        } else if (cause != null) {
            return isTimeoutException(cause.getCause());
        } else {
            return false;
        }
    }

    protected Properties getCallProperties() {
        return callProperties;
    }

    protected Map<String, String> stepIdMap(String stepId) {
        if (stepId != null) {
            return Map.of(PropertyConstants.TEST_STEP_ID, stepId);
        }
        return null;
    }

    protected void prepareClient(Client client, boolean configureTimeouts) {
        if (client.getConduit() instanceof HTTPConduit httpConduit) {
            var clientPolicy = new HTTPClientPolicy();
            clientPolicy.setConnectionTimeout(30000L);
            long receiveTimeout = 0L;
            if (configureTimeouts && clientConfigurationProvider != null) {
                var clientConfiguration = clientConfigurationProvider.get();
                if (clientConfiguration.timeout() != null) {
                    long specificTimeout = clientConfiguration.timeout();
                    if (specificTimeout > 0L) {
                        receiveTimeout = specificTimeout;
                    }
                }
            }
            clientPolicy.setReceiveTimeout(receiveTimeout);
            httpConduit.setClient(clientPolicy);
        }
    }

    protected void prepareRemoteServiceLookup(Properties stepProperties) {
        if (stepProperties != null && !StringUtils.isBlank(stepProperties.getProperty(PropertyConstants.AUTH_BASIC_USERNAME))) {
            /*
            The configuration specifies that we have basic authentication. To allow this to go through even if
            the WSDL is protected we use a thread-safe (via ThreadLocal) authenticator. This is because the
            new MessagingServiceClient(getServiceURL()) call results in a call to the WSDL (that needs authentication).
             */
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    Properties callProperties = RemoteCallContext.getCallProperties();
                    String username = callProperties.getProperty(PropertyConstants.AUTH_BASIC_USERNAME);
                    String password = callProperties.getProperty(PropertyConstants.AUTH_BASIC_PASSWORD);
                    return new PasswordAuthentication(
                            username,
                            password.toCharArray());
                }
            });
        }
    }

    protected void prepareClient(Client client) {
        prepareClient(client, true);
    }

}
