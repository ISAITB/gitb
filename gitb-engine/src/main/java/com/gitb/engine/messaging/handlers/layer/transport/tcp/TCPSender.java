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
