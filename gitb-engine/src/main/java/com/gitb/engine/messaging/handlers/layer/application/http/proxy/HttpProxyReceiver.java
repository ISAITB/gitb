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

import com.gitb.core.Configuration;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpReceiver;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.messaging.Message;

import java.util.List;

/**
 * Created by serbay on 9/23/14.
 */
public class HttpProxyReceiver extends HttpReceiver {

	public HttpProxyReceiver(SessionContext session, TransactionContext transaction) {
		super(session, transaction);
	}

    public Message receive(List<Configuration> configurations, Message inputs) throws Exception {
        return super.receive(configurations, inputs);
    }

}
