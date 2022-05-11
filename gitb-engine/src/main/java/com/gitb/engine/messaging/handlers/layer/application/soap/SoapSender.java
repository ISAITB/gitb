package com.gitb.engine.messaging.handlers.layer.application.soap;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpSender;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by serbay on 9/23/14.
 */
public class SoapSender extends HttpSender {

	private static final Logger logger = LoggerFactory.getLogger(SoapSender.class);

	public static final String DEFAULT_CHARACTER_SET_ENCODING = "UTF-8";
	public static final String SOAP_HTTP_METHOD = "POST";

	public SoapSender(SessionContext session, TransactionContext transaction) {
		super(session, transaction);
	}

	@Override
	public Message send(List<Configuration> configurations, Message message) throws Exception {
		logger.debug(addMarker(), "Sending soap message");
		SoapSenderCore impl = new SoapSenderCore(this);
		Message httpMessage = impl.send(configurations, message);
		super.send(configurations, httpMessage);
		logger.debug(addMarker(), "Sent soap message");
		return httpMessage;
	}

}
