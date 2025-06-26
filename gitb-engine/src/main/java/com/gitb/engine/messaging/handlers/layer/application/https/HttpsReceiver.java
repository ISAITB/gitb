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

package com.gitb.engine.messaging.handlers.layer.application.https;

import com.gitb.core.Configuration;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpReceiver;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.messaging.Message;

import java.util.List;

/**
 * Created by senan on 07.11.2014.
 */
public class HttpsReceiver extends HttpReceiver {

    public HttpsReceiver(SessionContext session, TransactionContext transaction) {
        super(session, transaction);
    }

    @Override
    public Message receive(List<Configuration> configurations, Message inputs) throws Exception {
        //use the socket retrieved from the transaction
        socket = getSocket();

        //if the socket is null, that means transaction has just begun, so create new
        //below code blocks until a socket is created
        if(socket == null) {
            waitUntilMessageReceived();
        }

        return super.receive(configurations, inputs);
    }
}
