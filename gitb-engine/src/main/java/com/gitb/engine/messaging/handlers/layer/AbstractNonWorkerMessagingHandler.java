package com.gitb.engine.messaging.handlers.layer;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.ms.InitiateResponse;

import java.util.List;

/**
 * Parent class for all new generation messaging handlers that don't use threading and direct
 * socket connections to send and receive messages.
 */
public abstract class AbstractNonWorkerMessagingHandler extends AbstractMessagingHandler {

    @Override
    public MessagingModule getModuleDefinition() {
        return new MessagingModule();
    }

    @Override
    public void beginTransaction(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations) {
        // Do nothing.
    }

    @Override
    public void endTransaction(String sessionId, String transactionId, String stepId) {
        // Do nothing.
    }

    @Override
    public void endSession(String sessionId) {
        // Do nothing.
    }

    @Override
    public MessagingReport listenMessage(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations, Message inputs) {
        throw new IllegalStateException("Only send and receive operations are supported by this handler");
    }

    @Override
    public boolean needsMessagingServerWorker() {
        return false;
    }

    @Override
    public InitiateResponse initiate(List<ActorConfiguration> actorConfigurations) {
        return new InitiateResponse();
    }

    @Override
    public InitiateResponse initiateWithSession(List<ActorConfiguration> actorConfigurations, String testSessionId) {
        var response = new InitiateResponse();
        /*
         * For simplicity, all these handlers use the test session ID as the messaging session ID. This simplifies
         * things like test session logging directly from the handler implementation.
         */
        response.setSessionId(testSessionId);
        return response;
    }

}
