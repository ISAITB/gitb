package com.gitb.engine.messaging.handlers.layer.transport.udp;

import com.gitb.core.MessagingModule;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.engine.messaging.handlers.model.udp.IDatagramListener;
import com.gitb.engine.messaging.handlers.model.udp.IDatagramReceiver;
import com.gitb.engine.messaging.handlers.model.udp.IDatagramSender;
import com.gitb.engine.messaging.handlers.server.IMessagingServer;
import com.gitb.engine.messaging.handlers.server.udp.UDPMessagingServer;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;

import java.io.IOException;

/**
 * Created by serbay.
 */
public class UDPMessagingHandler extends AbstractMessagingHandler {

	public static final String CONTENT_MESSAGE_FIELD_NAME = "content";
	public static final String MODULE_DEFINITION_XML = "/messaging/udp-messaging-definition.xml";

	private static final MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

	@Override
	public IDatagramReceiver getDatagramReceiver(SessionContext sessionContext, TransactionContext transactionContext) {
		return new UDPReceiver(sessionContext, transactionContext);
	}

	@Override
	public IDatagramSender getDatagramSender(SessionContext sessionContext, TransactionContext transactionContext) {
		return new UDPSender(sessionContext, transactionContext);
	}

    @Override
    public IDatagramListener getDatagramListener(SessionContext sessionContext, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        return new UDPListener(sessionContext, receiverTransactionContext, senderTransactionContext);
    }

    @Override
	protected IMessagingServer getMessagingServer() throws IOException {
		return UDPMessagingServer.getInstance();
	}

	@Override
	public MessagingModule getModuleDefinition() {
		return module;
	}
}
