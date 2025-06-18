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
