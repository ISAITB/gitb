package com.gitb.engine.remote;

import com.gitb.core.ErrorCode;
import com.gitb.engine.PropertyConstants;
import com.gitb.engine.SessionManager;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.ErrorUtils;

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

}
