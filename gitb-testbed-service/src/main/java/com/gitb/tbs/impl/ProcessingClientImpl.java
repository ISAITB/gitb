package com.gitb.tbs.impl;

import com.gitb.engine.CallbackManager;
import com.gitb.engine.SessionManager;
import com.gitb.ps.ProcessingClient;
import com.gitb.ps.LogRequest;
import com.gitb.ps.Void;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcessingClientImpl implements ProcessingClient {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessingClientImpl.class);

    @Override
    public Void log(LogRequest logRequest) {
        // The received session ID is a processing session ID
        if (logRequest.getSessionId() != null) {
            var testSessionId = SessionManager.getInstance().getTestSessionForProcessingSession(logRequest.getSessionId());
            if (testSessionId == null) {
                LOG.warn(String.format("Could not determine test session for processing session [%s]", logRequest.getSessionId()));
            } else {
                CallbackManager.getInstance().logMessageReceived(testSessionId, logRequest.getMessage(), logRequest.getLevel());
            }
        } else {
            LOG.warn("Received log message from processing service but no session ID was provided");
        }
        return new Void();
    }

}
