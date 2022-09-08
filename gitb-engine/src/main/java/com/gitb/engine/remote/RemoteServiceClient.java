package com.gitb.engine.remote;

import com.gitb.core.BaseTestModule;
import com.gitb.core.ErrorCode;
import com.gitb.engine.PropertyConstants;
import com.gitb.engine.SessionManager;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.ErrorUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

public abstract class RemoteServiceClient <T extends BaseTestModule> {

    private final Properties callProperties;
    protected URL serviceURL;
    protected final String testSessionId;
    protected T serviceModule;

    protected RemoteServiceClient(URL serviceURL, Properties callProperties, String sessionId) {
        this.serviceURL = serviceURL;
        this.testSessionId = sessionId;
        this.callProperties = Objects.requireNonNullElseGet(callProperties, Properties::new);
        this.callProperties.put(PropertyConstants.TEST_SESSION_ID, sessionId);
        this.callProperties.put(PropertyConstants.TEST_CASE_ID, SessionManager.getInstance().getContext(sessionId).getTestCaseIdentifier());
    }

    protected URL getServiceURL() {
        if (serviceURL == null) {
            if (serviceModule == null) {
                throw new IllegalStateException("Remote service module found but with no provided URL or module definition");
            } else {
                try {
                    serviceURL = new URI(serviceModule.getServiceLocation()).toURL();
                } catch (MalformedURLException e) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote test module found named ["+serviceModule.getId()+"] with an malformed URL ["+serviceModule.getServiceLocation()+"]"), e);
                } catch (URISyntaxException e) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote test module found named ["+serviceModule.getId()+"] with an invalid URI syntax ["+serviceModule.getServiceLocation()+"]"), e);
                }
            }
        }
        return serviceURL;
    }

    protected <Y> Y call(Supplier<Y> supplier) {
        try {
            RemoteCallContext.setCallProperties(getCallProperties());
            return supplier.get();
        } finally {
            RemoteCallContext.clearCallProperties();
        }
    }

    protected Properties getCallProperties() {
        return callProperties;
    }

}
