package com.gitb.messaging;

import com.gitb.messaging.layer.application.as2.peppol.PeppolAS2MessagingHandler;
import com.gitb.types.ObjectType;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.soap.SOAPException;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;

/**
 * Created by senan on 12.11.2014.
 */
public class PeppolAS2MessagingHandlerTest extends AS2MessagingHandlerTest {

    @BeforeClass
    public static void init() {
        receiverHandler = new PeppolAS2MessagingHandler();
        senderHandler = new PeppolAS2MessagingHandler();
    }

    @Test
    public void handlersShouldNotBeNull() throws IOException, SOAPException {
        assertNotNull(receiverHandler);
        assertNotNull(senderHandler);

        assertNotNull(receiverHandler);
        assertNotNull(senderHandler);

        ObjectType messageObject = new ObjectType();
        messageObject.deserialize(MESSAGE_STR.getBytes());

        AS2MessagingHandlerTest.message = new Message();
        AS2MessagingHandlerTest.message
                .getFragments()
                .put(PeppolAS2MessagingHandler.BUSINESS_DOCUMENT_FIELD_NAME, messageObject);
    }


}
