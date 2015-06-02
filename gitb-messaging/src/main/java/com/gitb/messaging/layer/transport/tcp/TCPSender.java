package com.gitb.messaging.layer.transport.tcp;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.AbstractTransactionSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
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

		logger.debug("Socket created: " + socket);

		OutputStream outputStream = socket.getOutputStream();

		BinaryType binaryData = (BinaryType) message.getFragments().get(TCPMessagingHandler.CONTENT_MESSAGE_FIELD_NAME);

		outputStream.write((byte[]) binaryData.getValue());

		outputStream.flush();

		logger.debug("Flushed output stream: " + socket);

        return message;
	}
}
