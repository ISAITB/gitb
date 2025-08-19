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

package com.gitb.engine.messaging.handlers.layer.application.http.proxy;

import com.gitb.core.MessagingModule;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionReceiver;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionSender;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;

import java.io.IOException;

/**
 * Created by serbay on 9/23/14.
 */
@MessagingHandler(name="HttpProxyMessaging")
public class HttpProxyMessagingHandler extends AbstractMessagingHandler {

	public static final String HTTP_HEADERS_FIELD_NAME = "http_headers";
	public static final String HTTP_BODY_FIELD_NAME = "http_body";
	public static final String HTTP_METHOD_FIELD_NAME = "http_method";
	public static final String HTTP_PATH_FIELD_NAME = "http_path";
    public static final String HTTP_PROTOCOL_VERSION_FIELD_NAME = "http_version";

	public static final String HTTP_METHOD_CONFIG_NAME = "http.method";
	public static final String HTTP_URI_EXTENSION_CONFIG_NAME = "http.uri.extension";

    public static final String PROXY_ADDRESS_CONFIG_NAME = "proxy.address";
	public static final String HTTP_REQUEST_DATA = "request_data";

	public static final String MODULE_DEFINITION_XML = "/messaging/http-proxy-messaging-definition.xml";

	private static final MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

	@Override
	public MessagingModule getModuleDefinition() {
		return module;
	}

	@Override
	public ITransactionReceiver getReceiver(SessionContext sessionContext, TransactionContext transactionContext) {
		return new HttpProxyReceiver(sessionContext, transactionContext);
	}

	@Override
	public ITransactionSender getSender(SessionContext sessionContext, TransactionContext transactionContext) {
		return new HttpProxySender(sessionContext, transactionContext);
	}

}
