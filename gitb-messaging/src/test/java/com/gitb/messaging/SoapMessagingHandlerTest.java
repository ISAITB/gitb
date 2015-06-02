package com.gitb.messaging;

import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.layer.application.soap.SoapMessagingHandler;
import com.gitb.types.BinaryType;
import com.gitb.types.ObjectType;
import com.gitb.types.StringType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;

/**
 * Created by serbay on 9/29/14.
 */
public class SoapMessagingHandlerTest extends MessagingHandlerTest {
	private static Message message;

	private static final String MESSAGE_STR = "" +
		"<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:v1=\"http://www.gitb.com/tbs/v1/\">\n" +
		"   <soapenv:Header/>\n"+
		"   <soapenv:Body>\n" +
		"      <v1:GetTestCaseDefinitionRequest>\n" +
		"         <!--Optional:-->\n" +
		"         <tcid>1</tcid>\n" +
		"      </v1:GetTestCaseDefinitionRequest>\n" +
		"   </soapenv:Body>\n" +
		"</soapenv:Envelope>";

	@BeforeClass
	public static void init() {
		receiverHandler = new SoapMessagingHandler();
		senderHandler = new SoapMessagingHandler();
        listenerHandler = new SoapMessagingHandler();
	}

	@Test
	public void handlersShouldNotBeNull() throws IOException, SOAPException {
		assertNotNull(receiverHandler);
		assertNotNull(senderHandler);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(MESSAGE_STR.getBytes());

		MimeHeaders mimeHeaders = new MimeHeaders();
		mimeHeaders.addHeader("Content-Type", SoapMessagingHandler.SOAP_MESSAGE_CONTENT_TYPE);

		MessageFactory messageFactory = MessageFactory.newInstance();
		SOAPMessage soapMessage = messageFactory.createMessage(mimeHeaders, inputStream);

		ObjectType messageObject = new ObjectType();
		messageObject.setValue(soapMessage.getSOAPPart());

		message = new Message();
		message
			.getFragments()
			.put(SoapMessagingHandler.SOAP_MESSAGE_FIELD_NAME, messageObject);
	}

	@Override
	protected void messageReceived(Message message) {
		assertNotNull(message);
	}

	@Override
	protected Message constructMessage() {
		return message;
	}
}
