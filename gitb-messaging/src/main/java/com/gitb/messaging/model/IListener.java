package com.gitb.messaging.model;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;

import java.util.List;

/**
 * Created by serbay.
 */
public interface IListener {
    /**
     * Transform incoming message to outgoing message
     * @param incomingMessage Incoming message
     * @return Outgoing message
     */
	public Message transformMessage(Message incomingMessage) throws Exception;

    /**
     * Transform listen configurations to be used when sending the message
     * to the receiver SUT.
     * @param incomingMessage Incoming message
     * @param configurations
     * @return Transformed configurations
     */
    public List<Configuration> transformConfigurations(Message incomingMessage, List<Configuration> configurations);

    /**
     * Listens incoming message and forwards it to the relevant actor
     * after transforming it using {@link #transformMessage(com.gitb.messaging.Message)}.
     * @param configurations
     * @return
     * @throws Exception
     */
    public Message listen(List<com.gitb.core.Configuration> configurations, Message inputs) throws Exception;
}
