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

package com.gitb.engine.messaging.handlers.layer;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionListener;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionReceiver;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.List;

/**
 * Created by serbay.
 */
public abstract class AbstractTransactionListener implements ITransactionListener {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTransactionListener.class);

	protected final SessionContext session;
    protected final TransactionContext receiverTransactionContext;
    protected final TransactionContext senderTransactionContext;

	public AbstractTransactionListener(SessionContext session, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
		this.session = session;
        this.receiverTransactionContext = receiverTransactionContext;
        this.senderTransactionContext = senderTransactionContext;
    }

    protected Marker addMarker() {
        return MarkerFactory.getDetachedMarker(session.getTestSessionId());
    }

    public Message listen(List<Configuration> configurations, Message inputs) throws Exception {
        ITransactionReceiver receiver = receiverTransactionContext.getParameter(ITransactionReceiver.class);
        ITransactionSender sender = senderTransactionContext.getParameter(ITransactionSender.class);

        Message incomingMessage = receiver.receive(configurations, inputs);

        logger.debug(addMarker(), "Message received from the sender.");

        Message outgoingMessage = transformMessage(incomingMessage);
        List<Configuration> outgoingConfigurations = transformConfigurations(incomingMessage, configurations);

        logger.debug(addMarker(), "Incoming message is transformed to an outgoing message.");

        sender.send(outgoingConfigurations, outgoingMessage);

        logger.debug(addMarker(), "Message is forwarded to the receiver.");

        return incomingMessage;
    }

    @Override
    public Message transformMessage(Message incomingMessage) {
        return incomingMessage;
    }

    @Override
    public List<Configuration> transformConfigurations(Message incomingMessage, List<Configuration> configurations) {
        return configurations;
    }
}
