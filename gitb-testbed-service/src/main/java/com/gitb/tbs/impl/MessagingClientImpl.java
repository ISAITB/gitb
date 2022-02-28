package com.gitb.tbs.impl;

import com.gitb.engine.CallbackManager;
import com.gitb.engine.SessionManager;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import com.gitb.ms.LogRequest;
import com.gitb.ms.MessagingClient;
import com.gitb.ms.NotifyForMessageRequest;
import com.gitb.ms.Void;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by simatosc on 25/11/2016.
 */
@Component
public class MessagingClientImpl implements MessagingClient {

    private static final Logger LOG = LoggerFactory.getLogger(MessagingClientImpl.class);

    @Override
    public Void notifyForMessage(NotifyForMessageRequest parameters) {
        CallbackManager.getInstance().callbackReceived(
                parameters.getSessionId(),
                parameters.getCallId(),
                MessagingHandlerUtils.getMessagingReport(parameters.getReport()));
        return new Void();
    }

    @Override
    public Void log(LogRequest logRequest) {
        // The received session ID is a messaging session ID
        if (logRequest.getSessionId() != null) {
            var testSessionId = SessionManager.getInstance().getTestSessionForMessagingSession(logRequest.getSessionId());
            if (testSessionId == null) {
                LOG.warn(String.format("Could not determine test session for messaging session [%s]", logRequest.getSessionId()));
            } else {
                CallbackManager.getInstance().logMessageReceived(testSessionId, logRequest.getMessage(), logRequest.getLevel());
            }
        } else {
            LOG.warn("Received log message from messaging service but no session ID was provided");
        }
        return new Void();
    }

}
