/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.engine.messaging.handlers.layer.application.soap;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpMessagingHandler;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpSender;
import com.gitb.types.*;
import com.gitb.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.soap.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SoapSenderCore {

    private final HttpSender parent;

    public SoapSenderCore(HttpSender parent) {
        this.parent = parent;
    }

    private static final Logger logger = LoggerFactory.getLogger(SoapSenderCore.class);

    public Message send(List<Configuration> configurations, Message message) throws Exception {
        logger.debug(parent.addMarker(), "Sending soap message");

        SOAPMessage soapMessage = constructSoapMessage(configurations, message);

        logger.debug(parent.addMarker(), "Constructed soap message");

        Message httpMessage = constructHttpMessageFromSoapMessage(configurations, message, soapMessage);

        logger.debug(parent.addMarker(), "Constructed http message from soap message");

        configurations
                .add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_METHOD_CONFIG_NAME, SoapSender.SOAP_HTTP_METHOD));

        return httpMessage;
    }

    protected Message constructHttpMessageFromSoapMessage(List<Configuration> configurations, Message message, SOAPMessage soapMessage) throws IOException, SOAPException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        soapMessage.writeTo(outputStream);

        byte[] binaryMessage = outputStream.toByteArray();

        Message httpMessage = new Message();

        // compute Content-Type
        StringBuilder soapContentType = new StringBuilder("start-info=\"application/soap+xml\"; start=\"" + SoapMessagingHandler.SOAP_START_HEADER + "\";");
        String[] soapHeaders = soapMessage.getMimeHeaders().getHeader(SoapMessagingHandler.HTTP_CONTENT_TYPE_HEADER);
        if (soapMessage.countAttachments() != 0 && soapHeaders != null) {
            // add MTOM specific Content-Type
            soapContentType.insert(0, "multipart/related; type=\"application/xop+xml\"; ");

            // add boundary
            for (String soapHeader : soapHeaders[0].split(";")) {
                if (soapHeader.contains("boundary")) {
                    soapContentType.append(soapHeader).append(";");
                }
            }
        }

        // add Content-Type
        MapType soapHeaderType = new MapType();
        soapHeaderType.addItem(SoapMessagingHandler.HTTP_CONTENT_TYPE_HEADER, new StringType(soapContentType.toString()));
        httpMessage
                .getFragments()
                .put(SoapMessagingHandler.HTTP_HEADERS_FIELD_NAME, soapHeaderType);

        // add header from parameter
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
        String soapVersion = Objects.requireNonNull(ConfigurationUtils.getConfiguration(configurations, SoapMessagingHandler.SOAP_VERSION_CONFIG_NAME)).getValue();

        MessageFactory messageFactory = null;

        if(soapVersion.contentEquals(SoapMessagingHandler.SOAP_VERSION_1_1)) {
            messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
        } else if(soapVersion.contentEquals(SoapMessagingHandler.SOAP_VERSION_1_2)) {
            messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL); //double check
        }

        ObjectType messageNode = getMessageNode(configurations, message);

        SOAPMessage soapMessage = Objects.requireNonNull(messageFactory).createMessage(null, new ByteArrayInputStream(messageNode.serializeByDefaultEncoding()));

        // add a content-id
        soapMessage.getSOAPPart().setContentId(SoapMessagingHandler.SOAP_START_HEADER);

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
        return (ObjectType) message.getFragments().get(SoapMessagingHandler.SOAP_MESSAGE_FIELD_NAME);
    }

    private MapType getAttachments(Message message) {
        return (MapType) message.getFragments().get(SoapMessagingHandler.SOAP_ATTACHMENTS_FIELD_NAME);
    }


    private String getCharsetEncoding(List<Configuration> configurations, Message message) {
        Configuration configuration = ConfigurationUtils.getConfiguration(configurations, SoapMessagingHandler.SOAP_CHARACTER_SET_ENCODING_CONFIG_NAME);

        if(configuration != null) {
            return configuration.getValue();
        } else {
            return SoapSender.DEFAULT_CHARACTER_SET_ENCODING;
        }
    }


}
