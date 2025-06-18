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

package com.gitb.engine.remote;

import com.gitb.core.ErrorCode;
import com.gitb.engine.PropertyConstants;
import com.gitb.engine.SessionManager;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.ErrorUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.transport.http.HTTPConduit;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

public abstract class RemoteServiceClient {

    private final Properties callProperties;
    protected URL serviceURL;
    protected final String testSessionId;

    protected RemoteServiceClient(URL serviceURL, Properties callProperties, String sessionId) {
        this.serviceURL = serviceURL;
        this.testSessionId = sessionId;
        this.callProperties = Objects.requireNonNullElseGet(callProperties, Properties::new);
        this.callProperties.put(PropertyConstants.TEST_SESSION_ID, sessionId);
        this.callProperties.put(PropertyConstants.TEST_CASE_ID, SessionManager.getInstance().getContext(sessionId).getTestCaseIdentifier());
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
        } finally {
            RemoteCallContext.clearCallProperties();
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

    protected void prepareClient(Client client) {
        /*
         * The receiveTimeout applies when the service is reached but is taking long to respond.
         * In this case we deactivate the timeout.
         */
        ((HTTPConduit)client.getConduit()).getClient().setReceiveTimeout(0L);
    }

}
