package com.gitb.messaging.layer.application.soap;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.layer.application.http.HttpReceiver;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.BinaryType;
import com.gitb.types.MapType;
import com.gitb.types.ObjectType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.soap.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

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

        BinaryType binaryContent = (BinaryType) httpMessage.getFragments().get(HttpMessagingHandler.HTTP_BODY_FIELD_NAME);

        byte[] content = (byte[]) binaryContent.getValue();

        InputStream inputStream = new ByteArrayInputStream(content);

        //initialize the message factory according to given configuration in receive step
        String soapVersion = ConfigurationUtils.getConfiguration(configurations, SoapMessagingHandler.SOAP_VERSION_CONFIG_NAME).getValue();

        MessageFactory messageFactory = null;

        if(soapVersion.contentEquals(SoapMessagingHandler.SOAP_VERSION_1_1)) {
            messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
        } else if(soapVersion.contentEquals(SoapMessagingHandler.SOAP_VERSION_1_2)) {
            messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL); //double check
        } else {
            //will not execute here, already handled in SoapMessagingHandler
        }

        SOAPMessage soapMessage = messageFactory.createMessage(null, inputStream);

        logger.debug("Created soap message from the http message: " + soapMessage);

        SOAPHeader soapHeader = soapMessage.getSOAPHeader();
        SOAPBody soapBody = soapMessage.getSOAPBody();

        ObjectType headerObject = new ObjectType();
        headerObject.setValue(soapHeader);

        ObjectType bodyObject = new ObjectType();
        bodyObject.setValue(soapBody);

        ObjectType messageObject = new ObjectType();
        messageObject.setValue(soapMessage.getSOAPPart());

        Message message = new Message();
        message.getFragments().put(SoapMessagingHandler.HTTP_HEADERS_FIELD_NAME, httpHeaders);
        message.getFragments().put(SoapMessagingHandler.SOAP_HEADER_FIELD_NAME, headerObject);
        message.getFragments().put(SoapMessagingHandler.SOAP_BODY_FIELD_NAME, bodyObject);
        message.getFragments().put(SoapMessagingHandler.SOAP_MESSAGE_FIELD_NAME, messageObject);

        return message;
    }
}
