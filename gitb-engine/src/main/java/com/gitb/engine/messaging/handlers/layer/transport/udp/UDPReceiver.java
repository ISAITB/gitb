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

import com.gitb.core.Configuration;
import com.gitb.engine.messaging.handlers.layer.AbstractDatagramReceiver;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.messaging.Message;
import com.gitb.types.BinaryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.util.List;

/**
 * Created by serbay.
 */
public class UDPReceiver extends AbstractDatagramReceiver {

	private static final Logger logger = LoggerFactory.getLogger(UDPReceiver.class);

	protected UDPReceiver(SessionContext session, TransactionContext transaction) {
		super(session, transaction);
	}

	@Override
	public Message receive(List<Configuration> configurations, Message inputs) throws Exception {
		waitUntilMessageReceived();

		DatagramPacket packet = transaction.getParameter(DatagramPacket.class);

		logger.debug(addMarker(), "Message received: " + packet);

		byte data[] = new byte[packet.getData().length];
		System.arraycopy(packet.getData(), 0, data, 0, packet.getData().length);

		BinaryType binaryData = new BinaryType();
		binaryData.setValue(data);

		Message message = new Message();
		message
			.getFragments()
			.put(UDPMessagingHandler.CONTENT_MESSAGE_FIELD_NAME, binaryData);

		return message;
	}
}
