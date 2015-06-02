package com.gitb.messaging;

import com.gitb.core.Configuration;
import com.gitb.messaging.layer.transport.tcp.TCPMessagingHandler;
import com.gitb.types.BinaryType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by serbay on 9/25/14.
 */
public class TCPMessagingHandlerTest extends MessagingHandlerTest {

	private static Message message;

	@BeforeClass
	public static void init() {
		receiverHandler = new TCPMessagingHandler();
		senderHandler = new TCPMessagingHandler();
        listenerHandler = new TCPMessagingHandler();

		String test =  "test123";
		BinaryType data = new BinaryType();
		data.setValue(test.getBytes());

		message = new Message();
		message.getFragments().put(TCPMessagingHandler.CONTENT_MESSAGE_FIELD_NAME, data);
	}

	@Test
	public void handlerShouldNotBeNull() {
		assertNotNull(receiverHandler);
		assertNotNull(senderHandler);
	}


	@Override
	protected void messageReceived(Message message) {
		BinaryType data = (BinaryType) message.getFragments().get(TCPMessagingHandler.CONTENT_MESSAGE_FIELD_NAME);

		byte[] content = (byte[]) data.getValue();

		assertEquals(new String(content), "test123");
	}

	@Override
	protected Message constructMessage() {
		return message;
	}
}
