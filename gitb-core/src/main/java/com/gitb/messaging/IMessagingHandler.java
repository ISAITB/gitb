package com.gitb.messaging;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.messaging.model.InitiateResponse;

import java.util.List;

/**
 * Created by tuncay on 9/1/14.
 */
public interface IMessagingHandler {

    /**
     * Returns the messaging module definition
     * @return module definition
     */
    public MessagingModule getModuleDefinition();

    /**
     * Does initial configuration for the messaging module to begin
     * transactions and returns the session id
     * @param actorConfigurations actor configurations for the transaction that will be created
     * @return initial configuration object that consists some configurations
     * and session id
     */
    public InitiateResponse initiate(List<ActorConfiguration> actorConfigurations);

    /**
     * Initiate a transaction with Messaging Handler
     * @param sessionId
     * @param transactionId transaction id
     * @param from
     * @param to
     * @param configurations configurations related to the transaction
     * @return the unique transactionId
     */
    public void beginTransaction(String sessionId, String transactionId, String from, String to, List<Configuration> configurations);

    /**
     *
     * @param sessionId
     * @param transactionId
     * @param configurations
     * @param message
     * @return
     */
    public MessagingReport sendMessage(String sessionId, String transactionId, List<Configuration> configurations, Message message);

    /**
     *
     * @param sessionId
     * @param transactionId
     * @param configurations
     * @return
     */
    public MessagingReport receiveMessage(String sessionId, String transactionId, List<Configuration> configurations, Message message);


    /**
     *
     * @param sessionId
     * @param transactionId
     * @param from
     * @param to
     * @param configurations
     * @return
     */
    public MessagingReport listenMessage(String sessionId, String transactionId, String from, String to, List<Configuration> configurations, Message inputs);

    /**
     * Close the transaction (Connection)
     * @param transactionId transaction id
     */
    public void endTransaction(String sessionId, String transactionId);

    /**
     * Close the session with the transactions in it
     * @param sessionId session id
     */
    public void endSession(String sessionId);

}
