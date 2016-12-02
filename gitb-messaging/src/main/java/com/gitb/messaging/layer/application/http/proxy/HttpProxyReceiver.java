package com.gitb.messaging.layer.application.http.proxy;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.application.http.HttpReceiver;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by serbay on 9/23/14.
 */
public class HttpProxyReceiver extends HttpReceiver {
	private Logger logger = LoggerFactory.getLogger(HttpProxyReceiver.class);

	public HttpProxyReceiver(SessionContext session, TransactionContext transaction) {
		super(session, transaction);
	}

    public Message receive(List<Configuration> configurations, Message inputs) throws Exception {
        return super.receive(configurations, inputs);
    }

}
