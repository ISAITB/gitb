package com.gitb.tbs.impl;

import com.gitb.engine.CallbackManager;
import com.gitb.vs.LogRequest;
import com.gitb.vs.ValidationClient;
import com.gitb.vs.Void;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ValidationClientImpl implements ValidationClient {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationClientImpl.class);

    @Override
    public Void log(LogRequest logRequest) {
        // The received session ID is the test session ID
        if (logRequest.getSessionId() != null) {
            CallbackManager.getInstance().logMessageReceived(logRequest.getSessionId(), logRequest.getMessage(), logRequest.getLevel());
        } else {
            LOG.warn("Received log message from validation service but no session ID was provided");
        }
        return new Void();
    }

}
