package com.gitb.messaging.layer.transport.tcp;

import com.gitb.core.MessagingModule;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.layer.AbstractMessagingHandler;
import com.gitb.messaging.model.tcp.ITransactionListener;
import com.gitb.messaging.model.tcp.ITransactionReceiver;
import com.gitb.messaging.model.tcp.ITransactionSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import org.kohsuke.MetaInfServices;

import java.io.IOException;

/**
 * Created by serbay on 9/24/14.
 */
@MetaInfServices(IMessagingHandler.class)
public class TCPMessagingHandler extends AbstractMessagingHandler {

	public static final String CONTENT_MESSAGE_FIELD_NAME = "content";
	public static final String MODULE_DEFINITION_XML = "/tcp-messaging-definition.xml";

	public static final int TCP_STOP_CHARACTER = -1;

	private static MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

	@Override
	public MessagingModule getModuleDefinition() {
		return module;
	}

	@Override
	public ITransactionReceiver getReceiver(SessionContext sessionContext, TransactionContext transactionContext) throws IOException {
		return new TCPReceiver(sessionContext, transactionContext);
	}

	@Override
	public ITransactionSender getSender(SessionContext sessionContext, TransactionContext transactionContext) {
		return new TCPSender(sessionContext, transactionContext);
	}

    @Override
    public ITransactionListener getListener(SessionContext sessionContext, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        return new TCPListener(sessionContext, receiverTransactionContext, senderTransactionContext);
    }
}
