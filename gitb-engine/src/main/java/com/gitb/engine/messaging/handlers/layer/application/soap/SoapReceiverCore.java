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
import com.gitb.engine.messaging.handlers.layer.application.http.HttpReceiver;
import com.gitb.types.*;
import com.gitb.utils.ConfigurationUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.soap.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

public class SoapReceiverCore {

    private final HttpReceiver parent;

    public SoapReceiverCore(HttpReceiver parent) {
        this.parent = parent;
    }

    private static final Logger logger = LoggerFactory.getLogger(SoapReceiverCore.class);

    public Message receive(Message httpMessage, List<Configuration> configurations, Message inputs) throws Exception {
        logger.debug(parent.addMarker(), "Received http message");

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

        logger.debug(parent.addMarker(), "Created soap message from the http message: " + soapMessage);

        // manage attachment parts
        ListType atts = new ListType(DataType.BINARY_DATA_TYPE);
        Iterator<?> itAtts = soapMessage.getAttachments();
        int sizeAtts = soapMessage.countAttachments();
        logger.debug(parent.addMarker(), "Found {} attachment(s).", sizeAtts);

        while (itAtts.hasNext()) {
            Object att = null;

            AttachmentPart itAtt = (AttachmentPart) itAtts.next();
            logger.debug(parent.addMarker(), "Found an attachment: " + itAtt);
            String typeAtt = itAtt.getContentType();
            if (typeAtt.contains("text/")) {
                String text = (String) itAtt.getContent();
                att = text.getBytes();
                // process text
                logger.debug(parent.addMarker(), "Found an attachment of type text: " + text);
            } else if (typeAtt.contains("image/")) {
                att = IOUtils.toByteArray((InputStream) itAtt.getContent());
                // process InputStream of image
                logger.debug(parent.addMarker(), "Found an attachment of type image: " + atts);
            } else if (typeAtt.contains("application/")) {
                att = IOUtils.toByteArray((InputStream) itAtt.getContent());
                // process InputStream of image
                logger.debug(parent.addMarker(), "Found an attachment of type binary: " + atts);
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
