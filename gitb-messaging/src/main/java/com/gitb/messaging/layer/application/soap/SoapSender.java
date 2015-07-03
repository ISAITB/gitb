package com.gitb.messaging.layer.application.soap;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.layer.application.http.HttpSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.BinaryType;
import com.gitb.types.DataType;
import com.gitb.types.MapType;
import com.gitb.types.ObjectType;
import com.gitb.utils.ConfigurationUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.soap.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by serbay on 9/23/14.
 */
public class SoapSender extends HttpSender {

	private static final Logger logger = LoggerFactory.getLogger(SoapSender.class);

	public static final String DEFAULT_CHARACTER_SET_ENCODING = "UTF-8";
	public static final String SOAP_HTTP_METHOD = "POST";

	public SoapSender(SessionContext session, TransactionContext transaction) {
		super(session, transaction);
	}

	@Override
	public Message send(List<Configuration> configurations, Message message) throws Exception {
		logger.debug("Sending soap message");

		SOAPMessage soapMessage = constructSoapMessage(configurations, message);

		logger.debug("Constructed soap message");

		Message httpMessage = constructHttpMessageFromSoapMessage(configurations, message, soapMessage);

		logger.debug("Constructed http message from soap message");

		configurations
			.add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_METHOD_CONFIG_NAME, SOAP_HTTP_METHOD));

		super.send(configurations, httpMessage);

		logger.debug("Sent soap message");

        return httpMessage;
	}

	protected Message constructHttpMessageFromSoapMessage(List<Configuration> configurations, Message message, SOAPMessage soapMessage) throws IOException, SOAPException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		soapMessage.writeTo(outputStream);

		byte[] binaryMessage = outputStream.toByteArray();

		Message httpMessage = new Message();
		if(message.getFragments().containsKey(SoapMessagingHandler.HTTP_HEADERS_FIELD_NAME)) {
			httpMessage
				.getFragments()
				.put(SoapMessagingHandler.HTTP_HEADERS_FIELD_NAME, message.getFragments().get(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME));
		}

		BinaryType payload = new BinaryType();
		payload.setValue(binaryMessage);

		httpMessage
			.getFragments()
			.put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, payload);

		return httpMessage;
	}

	protected SOAPMessage constructSoapMessage(List<Configuration> configurations, Message message) throws SOAPException, IOException {
        //initialize the message factory according to given configuration in send step
        String soapVersion = ConfigurationUtils.getConfiguration(configurations, SoapMessagingHandler.SOAP_VERSION_CONFIG_NAME).getValue();

        MessageFactory messageFactory = null;

        if(soapVersion.contentEquals(SoapMessagingHandler.SOAP_VERSION_1_1)) {
            messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
        } else if(soapVersion.contentEquals(SoapMessagingHandler.SOAP_VERSION_1_2)) {
            messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL); //double check
        } else {
            //will not execute here, already handled in SoapMessagingHandler
        }

		ObjectType messageNode = getMessageNode(configurations, message);

		SOAPMessage soapMessage = messageFactory.createMessage(null, new ByteArrayInputStream(messageNode.serializeByDefaultEncoding()));
		
		// add attachments
		MapType attsObject = getAttachments(message);
		if (attsObject != null) {
			Map<String, DataType> atts = ((Map<String, DataType>) attsObject.getValue());
			for (String contentId : atts.keySet()) {
				ByteArrayDataSource ds = new ByteArrayDataSource(atts.get(contentId).serializeByDefaultEncoding(), "application/octet-stream");
				DataHandler dh = new DataHandler(ds);
				AttachmentPart ap = soapMessage.createAttachmentPart(dh);
				ap.setContentId(contentId);
				soapMessage.addAttachmentPart(ap);
			}
		}

		return soapMessage;
	}

	private ObjectType getMessageNode(List<Configuration> configurations, Message message) {
		ObjectType object = (ObjectType) message.getFragments().get(SoapMessagingHandler.SOAP_MESSAGE_FIELD_NAME);

		return object;
	}
	
	private MapType getAttachments(Message message) {
		MapType object = (MapType) message.getFragments().get(SoapMessagingHandler.SOAP_ATTACHMENTS_FIELD_NAME);

		return object;
	}


	private String getCharsetEncoding(List<Configuration> configurations, Message message) {
		Configuration configuration = ConfigurationUtils.getConfiguration(configurations, SoapMessagingHandler.SOAP_CHARACTER_SET_ENCODING_CONFIG_NAME);

		if(configuration != null) {
			return configuration.getValue();
		} else {
			return DEFAULT_CHARACTER_SET_ENCODING;
		}
	}
}
