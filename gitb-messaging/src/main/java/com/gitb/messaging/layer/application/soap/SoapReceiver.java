package com.gitb.messaging.layer.application.soap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.layer.application.http.HttpReceiver;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.BinaryType;
import com.gitb.types.DataType;
import com.gitb.types.ListType;
import com.gitb.types.MapType;
import com.gitb.types.NumberType;
import com.gitb.types.ObjectType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;

/**
 * Created by serbay on 9/23/14.
 */
public class SoapReceiver extends HttpReceiver {

	private static final Logger logger = LoggerFactory.getLogger(SoapReceiver.class);

	public SoapReceiver(SessionContext session, TransactionContext transaction) {
		super(session, transaction);
	}

	@Override
	public Message receive(List<Configuration> configurations) throws Exception {
		Message httpMessage = super.receive(configurations);

		logger.debug("Received http message");

		MapType httpHeaders = (MapType) httpMessage.getFragments().get(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME);

		BinaryType binaryContent = (BinaryType) httpMessage.getFragments().get(
				HttpMessagingHandler.HTTP_BODY_FIELD_NAME);

		byte[] content = (byte[]) binaryContent.getValue();

		InputStream inputStream = new ByteArrayInputStream(content);

		// initialize the message factory according to given configuration in
		// receive step
		String soapVersion = ConfigurationUtils.getConfiguration(configurations,
				SoapMessagingHandler.SOAP_VERSION_CONFIG_NAME).getValue();

		MessageFactory messageFactory = null;

		if (soapVersion.contentEquals(SoapMessagingHandler.SOAP_VERSION_1_1)) {
			messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
		} else if (soapVersion.contentEquals(SoapMessagingHandler.SOAP_VERSION_1_2)) {
			messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL); // double
																						  // check
		} else {
			// will not execute here, already handled in SoapMessagingHandler
		}

		// extract mimeHeaders if present
		MimeHeaders mimeHeaders = new MimeHeaders();
		StringType contentType = (StringType) httpHeaders.getItem(SoapMessagingHandler.HTTP_CONTENT_TYPE_HEADER);
		if (contentType != null) {
			mimeHeaders.addHeader(SoapMessagingHandler.HTTP_CONTENT_TYPE_HEADER, (String) contentType.getValue());
		}

		SOAPMessage soapMessage = messageFactory.createMessage(mimeHeaders, inputStream);

		logger.debug("Created soap message from the http message: " + soapMessage);

		// manage attachment parts
		ListType atts = new ListType(DataType.BINARY_DATA_TYPE);
		Iterator<?> itAtts = soapMessage.getAttachments();
		int sizeAtts = soapMessage.countAttachments();
		logger.debug("Found {} attachment(s).", sizeAtts);

		while (itAtts.hasNext()) {
			Object att = null;

			AttachmentPart itAtt = (AttachmentPart) itAtts.next();
			logger.debug("Found an attachment: " + itAtt);
			String typeAtt = itAtt.getContentType();
			if (typeAtt.contains("text/")) {
				String text = (String) itAtt.getContent();
				att = text.getBytes();
				// process text
				logger.debug("Found an attachment of type text: " + text);
			} else if (typeAtt.contains("image/")) {
				att = IOUtils.toByteArray((InputStream) itAtt.getContent());
				// process InputStream of image
				logger.debug("Found an attachment of type image: " + atts);
			} else if (typeAtt.contains("application/")) {
				att = IOUtils.toByteArray((InputStream) itAtt.getContent());
				// process InputStream of image
				logger.debug("Found an attachment of type binary: " + atts);
			}

			// add to the list
			if (att != null) {
				BinaryType attObject = new BinaryType();
				attObject.setValue(att);
				atts.append(attObject);
			}
		}

		SOAPHeader soapHeader = soapMessage.getSOAPHeader();
		SOAPBody soapBody = soapMessage.getSOAPBody();

		ObjectType headerObject = new ObjectType();
		headerObject.setValue(soapHeader);

		ObjectType bodyObject = new ObjectType();
		bodyObject.setValue(soapBody);

		ObjectType messageObject = new ObjectType();
		messageObject.setValue(soapMessage.getSOAPPart());

		ObjectType contentObject = new ObjectType();
		contentObject.setValue(getFirstElement(soapBody));

		NumberType sizeAttsObject = new NumberType();
		sizeAttsObject.setValue((double) sizeAtts);

		Message message = new Message();
		message.getFragments().put(SoapMessagingHandler.HTTP_HEADERS_FIELD_NAME, httpHeaders);
		message.getFragments().put(SoapMessagingHandler.SOAP_HEADER_FIELD_NAME, headerObject);
		message.getFragments().put(SoapMessagingHandler.SOAP_BODY_FIELD_NAME, bodyObject);
		message.getFragments().put(SoapMessagingHandler.SOAP_MESSAGE_FIELD_NAME, messageObject);
		message.getFragments().put(SoapMessagingHandler.SOAP_CONTENT_FIELD_NAME, contentObject);
		message.getFragments().put(SoapMessagingHandler.SOAP_ATTACHMENTS_FIELD_NAME, atts);
		message.getFragments().put(SoapMessagingHandler.SOAP_ATTACHMENTS_SIZE_FIELD_NAME, sizeAttsObject);

		return message;
	}

	/**
	 * Extract the first element of the soapBody
	 * 
	 * @param soapBody
	 * @return
	 */
	private Object getFirstElement(SOAPBody soapBody) {
		Iterator<?> it = soapBody.getChildElements();
		while (it.hasNext()) {
			Node el = (Node) it.next();
			if (el.getNodeType() == Node.ELEMENT_NODE) {
				return el;
			}
		}
		return null;
	}
}
