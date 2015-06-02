package com.gitb.messaging;

import com.gitb.messaging.layer.transport.tcp.TCPMessagingHandler;
import com.gitb.messaging.layer.transport.udp.UDPMessagingHandler;
import com.gitb.types.BinaryType;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by serbay on 9/25/14.
 */
public class UDPMessagingHandlerTest extends MessagingHandlerTest {

    private static Message message;

    @BeforeClass
    public static void init() {
        receiverHandler = new UDPMessagingHandler();
        senderHandler = new UDPMessagingHandler();
        listenerHandler = new UDPMessagingHandler();

        String test = "test123";
        BinaryType data = new BinaryType();
        data.setValue(test.getBytes());

        message = new Message();
        message.getFragments().put(UDPMessagingHandler.CONTENT_MESSAGE_FIELD_NAME, data);
    }

    @Test
    public void handlerShouldNotBeNull() {
        assertNotNull(receiverHandler);
        assertNotNull(senderHandler);
    }


    @Override
    protected void messageReceived(Message message) {
        BinaryType data = (BinaryType) message.getFragments().get(UDPMessagingHandler.CONTENT_MESSAGE_FIELD_NAME);

        byte[] content = (byte[]) data.getValue();

        assertEquals("test123", new String(content));
    }

    @Override
    protected Message constructMessage() {
        return message;
    }
}
