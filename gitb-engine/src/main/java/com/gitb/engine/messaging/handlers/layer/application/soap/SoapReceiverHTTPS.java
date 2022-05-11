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
