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

package com.gitb.engine.messaging.handlers.layer.application.http;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.layer.AbstractTransactionListener;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by serbay.
 */
public class HttpListener extends AbstractTransactionListener {

    public HttpListener(SessionContext session, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        super(session, receiverTransactionContext, senderTransactionContext);
    }

    @Override
    public List<Configuration> transformConfigurations(Message incomingMessage, List<Configuration> configurations) {
        List<Configuration> transformed = new ArrayList<>(configurations);

        StringType method = (StringType) incomingMessage.getFragments().get(HttpMessagingHandler.HTTP_METHOD_FIELD_NAME);
        StringType path = (StringType) incomingMessage.getFragments().get(HttpMessagingHandler.HTTP_PATH_FIELD_NAME);

        transformed.add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_METHOD_CONFIG_NAME, method.getValue()));
        transformed.add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_URI_CONFIG_NAME, path.getValue()));

        return transformed;
    }
}
