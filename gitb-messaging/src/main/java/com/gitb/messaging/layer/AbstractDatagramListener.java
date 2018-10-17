package com.gitb.messaging.layer;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.model.udp.IDatagramListener;
import com.gitb.messaging.model.udp.IDatagramReceiver;
import com.gitb.messaging.model.udp.IDatagramSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.List;

/**
 * Created by serbay.
 */
public abstract class AbstractDatagramListener implements IDatagramListener {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDatagramListener.class);

	protected final SessionContext session;
    protected final TransactionContext receiverTransactionContext;
    protected final TransactionContext senderTransactionContext;

	public AbstractDatagramListener(SessionContext session, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
		this.session = session;
        this.receiverTransactionContext = receiverTransactionContext;
        this.senderTransactionContext = senderTransactionContext;
    }

    protected Marker addMarker() {
        return MarkerFactory.getDetachedMarker(session.getTestSessionId());
    }

    public Message listen(List<Configuration> configurations, Message inputs) throws Exception {
        IDatagramReceiver receiver = receiverTransactionContext.getParameter(IDatagramReceiver.class);
        IDatagramSender sender = senderTransactionContext.getParameter(IDatagramSender.class);

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
