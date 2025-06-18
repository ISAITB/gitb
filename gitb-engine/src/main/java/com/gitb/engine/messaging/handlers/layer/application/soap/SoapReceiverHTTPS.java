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
import com.gitb.engine.messaging.handlers.layer.application.https.HttpsReceiver;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;

import java.util.List;

/**
 * Created by serbay on 9/23/14.
 */
public class SoapReceiverHTTPS extends HttpsReceiver {

	public SoapReceiverHTTPS(SessionContext session, TransactionContext transaction) {
		super(session, transaction);
	}

	@Override
	public Message receive(List<Configuration> configurations, Message inputs) throws Exception {
		Message httpMessage = super.receive(configurations, inputs);
		SoapReceiverCore impl = new SoapReceiverCore(this);
		return impl.receive(httpMessage, configurations, inputs);
	}
}
