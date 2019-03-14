package com.gitb.messaging.layer.transport.udp;

import com.gitb.core.MessagingModule;
import com.gitb.messaging.layer.AbstractMessagingHandler;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.model.udp.IDatagramListener;
import com.gitb.messaging.model.udp.IDatagramReceiver;
import com.gitb.messaging.model.udp.IDatagramSender;
import com.gitb.messaging.server.IMessagingServer;
import com.gitb.messaging.server.udp.UDPMessagingServer;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by serbay.
 */
public class UDPMessagingHandler extends AbstractMessagingHandler {

	public static final String CONTENT_MESSAGE_FIELD_NAME = "content";
	public static final String MODULE_DEFINITION_XML = "/udp-messaging-definition.xml";

	private static final Logger logger = LoggerFactory.getLogger(UDPMessagingHandler.class);

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
