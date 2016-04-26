package com.gitb.messaging.layer.application.http.proxy;

import com.gitb.core.Configuration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.AbstractTransactionReceiver;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.layer.application.http.HttpReceiver;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.BinaryType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;
import org.apache.http.*;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.BHttpConnectionBase;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by serbay on 9/23/14.
 */
public class HttpProxyReceiver extends HttpReceiver {
	private Logger logger = LoggerFactory.getLogger(HttpProxyReceiver.class);

	public HttpProxyReceiver(SessionContext session, TransactionContext transaction) {
		super(session, transaction);
	}

    public Message receive(List<Configuration> configurations) throws Exception {
        return super.receive(configurations);
    }

}
