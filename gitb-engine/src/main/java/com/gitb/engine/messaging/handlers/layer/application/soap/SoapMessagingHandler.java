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
import com.gitb.core.MessagingModule;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpMessagingHandler;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionListener;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionReceiver;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionSender;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.ConfigurationUtils;

import java.io.IOException;
import java.util.List;

/**
 * Created by serbay on 9/23/14.
 */
@MessagingHandler(name="SoapMessaging")
public class SoapMessagingHandler extends AbstractMessagingHandler {

    public static final String HTTP_HEADERS_FIELD_NAME = HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME;
    public static final String HTTP_PATH_FIELD_NAME = HttpMessagingHandler.HTTP_PATH_FIELD_NAME;
    public static final String SOAP_HEADER_FIELD_NAME = "soap_header";
    public static final String SOAP_BODY_FIELD_NAME = "soap_body";
    public static final String SOAP_MESSAGE_FIELD_NAME = "soap_message";
    public static final String SOAP_CONTENT_FIELD_NAME = "soap_content";
    public static final String SOAP_ATTACHMENTS_FIELD_NAME = "soap_attachments";
    public static final String SOAP_ATTACHMENTS_SIZE_FIELD_NAME = "soap_attachments_size";

    public static final String HTTP_URI_CONFIG_NAME = HttpMessagingHandler.HTTP_URI_CONFIG_NAME;
    public static final String HTTP_SSL_CONFIG_NAME = "http.ssl";

    public static final String SOAP_CHARACTER_SET_ENCODING_CONFIG_NAME = "soap.encoding";
    public static final String SOAP_VERSION_CONFIG_NAME = "soap.version";
    public static final String SOAP_VERSION_1_1 = "1.1";
    public static final String SOAP_VERSION_1_2 = "1.2";

    public static final String HTTP_CONTENT_TYPE_HEADER = "Content-Type";
    public static final String SOAP_START_HEADER = "<root>";

    public static final String MODULE_DEFINITION_XML = "/messaging/soap-messaging-definition.xml";

    private static final MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

    @Override
    public void beginTransaction(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations) {
        super.beginTransaction(sessionId, transactionId, stepId, from, to, configurations);

        Configuration sslConfig = ConfigurationUtils.getConfiguration(configurations, HTTP_SSL_CONFIG_NAME);
        if (sslConfig != null && sslConfig.getValue() != null && "true".equalsIgnoreCase(sslConfig.getValue())) {
            throw new IllegalStateException("This handler is deprecated and should not be used.");
        }
    }

    @Override
    public MessagingModule getModuleDefinition() {
        return module;
    }

    @Override
    public ITransactionReceiver getReceiver(SessionContext sessionContext, TransactionContext transactionContext) throws IOException {
        if (transactionContext.getConfigurations() != null) {
            Configuration sslConfig = ConfigurationUtils.getConfiguration(transactionContext.getConfigurations(), HTTP_SSL_CONFIG_NAME);
            if (sslConfig != null && sslConfig.getValue() != null && "true".equalsIgnoreCase(sslConfig.getValue())) {
                return new SoapReceiverHTTPS(sessionContext, transactionContext);
            }
        }
        return new SoapReceiver(sessionContext, transactionContext);
    }

    @Override
    public ITransactionSender getSender(SessionContext sessionContext, TransactionContext transactionContext) {
        if (transactionContext.getConfigurations() != null) {
            Configuration sslConfig = ConfigurationUtils.getConfiguration(transactionContext.getConfigurations(), HTTP_SSL_CONFIG_NAME);
            if (sslConfig != null && sslConfig.getValue() != null && "true".equalsIgnoreCase(sslConfig.getValue())) {
                return new SoapSenderHTTPS(sessionContext, transactionContext);
            }
        }
        return new SoapSender(sessionContext, transactionContext);
    }

    @Override
    public ITransactionListener getListener(SessionContext sessionContext, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        return new SoapListener(sessionContext, receiverTransactionContext, senderTransactionContext);
    }

    @Override
    protected void validateReceiveConfigurations(List<Configuration> configurations) {
        super.validateReceiveConfigurations(configurations);

        Configuration versionConfig = ConfigurationUtils.getConfiguration(configurations, SOAP_VERSION_CONFIG_NAME);
        if(versionConfig != null) {
            String version = versionConfig.getValue();
            if(!version.contentEquals(SOAP_VERSION_1_1) && !version.contentEquals(SOAP_VERSION_1_2)) {
                throw new GITBEngineInternalError("Invalid SOAP version [" + version + "]. It can be either " + SOAP_VERSION_1_1 +" or " + SOAP_VERSION_1_2 + "");
            }
        } else {
            //cannot be null
            throw new GITBEngineInternalError("Missing parameter ["+SOAP_VERSION_CONFIG_NAME+"] in [receive] step in the test case");
        }
    }

    @Override
    protected void validateSendConfigurations(List<Configuration> configurations) {
        super.validateSendConfigurations(configurations);

        Configuration versionConfig = ConfigurationUtils.getConfiguration(configurations, SOAP_VERSION_CONFIG_NAME);
        if(versionConfig != null) {
            String version = versionConfig.getValue();
            if(!version.contentEquals("1.1") && !version.contentEquals("1.2")) {
                throw new GITBEngineInternalError("Invalid SOAP version [" + version + "]. It can be either " + SOAP_VERSION_1_1 +" or " + SOAP_VERSION_1_2 + "");
            }
        } else {
            //cannot be null
            throw new GITBEngineInternalError("Missing parameter ["+SOAP_VERSION_CONFIG_NAME+"] in [send] step in the test case");
        }
    }
}
