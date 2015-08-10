package com.gitb.messaging.layer.transport.tcp;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.AbstractTransactionReceiver;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.BinaryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

/**
 * Created by serbay on 9/23/14.
 */
public class TCPReceiver extends AbstractTransactionReceiver {

	private static final Logger logger = LoggerFactory.getLogger(TCPReceiver.class);

	public TCPReceiver(SessionContext session, TransactionContext transaction) throws IOException {
		super(session, transaction);
	}

	@Override
	public Message receive(List<Configuration> configurations) throws Exception {
		waitUntilMessageReceived();

		logger.debug("Message received: " + socket);

		InputStream inputStream = socket.getInputStream();

        byte[] data = TCPMessagingHandler.readBytes(inputStream);

		BinaryType binaryData = new BinaryType();
		binaryData.setValue(data);

		Message message = new Message();
		message
			.getFragments()
			.put(TCPMessagingHandler.CONTENT_MESSAGE_FIELD_NAME, binaryData);

		return message;
	}
}
