package com.gitb.messaging.layer.application.soap;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.application.https.HttpsSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by serbay on 9/23/14.
 */
public class SoapSenderHTTPS extends HttpsSender {

	private static final Logger logger = LoggerFactory.getLogger(SoapSenderHTTPS.class);

	public SoapSenderHTTPS(SessionContext session, TransactionContext transaction) {
		super(session, transaction);
	}

	@Override
	public Message send(List<Configuration> configurations, Message message) throws Exception {
		logger.debug("Sending soap message");
		SoapSenderCore impl = new SoapSenderCore();
		Message httpMessage = impl.send(configurations, message);
		super.send(configurations, httpMessage);
		logger.debug("Sent soap message");
		return httpMessage;
	}

}