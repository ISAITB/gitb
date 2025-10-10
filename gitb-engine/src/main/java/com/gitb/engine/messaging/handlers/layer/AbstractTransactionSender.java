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

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.ServerUtils;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionSender;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Objects;

/**
 * Created by serbay on 9/25/14.
 */
public abstract class AbstractTransactionSender implements ITransactionSender {
	private static final Logger logger = LoggerFactory.getLogger(AbstractTransactionSender.class);

	protected final SessionContext session;
	protected final TransactionContext transaction;

	public AbstractTransactionSender(SessionContext session, TransactionContext transaction) {
		this.session = session;
		this.transaction = transaction;
	}

	public Marker addMarker() {
		return MarkerFactory.getDetachedMarker(session.getTestSessionId());
	}

	@Override
	public Message send(List<Configuration> configurations, Message message) throws Exception {
		Socket socket = getSocket();
		if(socket == null) {
			// create a client socket
			ActorConfiguration actorConfiguration = transaction.getWith();

			Configuration ipAddressConfig = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.IP_ADDRESS_CONFIG_NAME);
			Configuration portConfig = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.PORT_CONFIG_NAME);

			socket = new Socket(InetAddress.getByName(Objects.requireNonNull(ipAddressConfig).getValue()), Integer.parseInt(Objects.requireNonNull(portConfig).getValue()));

            transaction.setParameter(Socket.class, socket);
		}

        return null;
	}

	@Override
	public void onEnd() throws Exception {
		Socket socket = getSocket();
		if(socket != null && !socket.isClosed()) {
            logger.debug(addMarker(), "Closing socket: {}", socket);
			socket.close();
		}
	}

	protected Socket getSocket() {
		return transaction.getParameter(Socket.class);
	}
}
