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

package com.gitb.engine.messaging.handlers.layer.transport.tcp;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.layer.AbstractTransactionSender;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.types.BinaryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

/**
 * Created by serbay on 9/23/14.
 */
public class TCPSender extends AbstractTransactionSender {
	private static final Logger logger = LoggerFactory.getLogger(TCPSender.class);

	public TCPSender(SessionContext session, TransactionContext transaction) {
		super(session, transaction);
	}

	@Override
	public Message send(List<Configuration> configurations, Message message) throws Exception {
		super.send(configurations, message);

		Socket socket = getSocket();

		logger.debug(addMarker(), "Socket created: " + socket);

		OutputStream outputStream = socket.getOutputStream();

		BinaryType binaryData = (BinaryType) message.getFragments().get(TCPMessagingHandler.CONTENT_MESSAGE_FIELD_NAME);

        TCPMessagingHandler.sendBytes(outputStream, (byte[]) binaryData.getValue());

		logger.debug(addMarker(), "Flushed output stream: " + socket);

        return message;
	}
}
