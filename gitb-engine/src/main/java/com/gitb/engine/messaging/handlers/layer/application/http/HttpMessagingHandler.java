package com.gitb.engine.messaging.handlers.layer.application.http;

import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.SecurityUtils;
import com.gitb.engine.messaging.handlers.SessionManager;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionListener;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionReceiver;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionSender;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.utils.ConfigurationUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.List;

/**
 * Created by serbay on 9/23/14.
 */
@MessagingHandler(name="HttpMessaging")
public class HttpMessagingHandler extends AbstractMessagingHandler {

	public static final String HTTP_HEADERS_FIELD_NAME = "http_headers";
	public static final String HTTP_BODY_FIELD_NAME = "http_body";
	public static final String HTTP_PARTS_FIELD_NAME = "http_parts";
	public static final String HTTP_METHOD_FIELD_NAME = "http_method";
	public static final String HTTP_PATH_FIELD_NAME = "http_path";
    public static final String HTTP_PROTOCOL_VERSION_FIELD_NAME = "http_version";
	public static final String HTTP_STATUS_FIELD_NAME = "http_status";

	public static final String HTTP_URI_CONFIG_NAME = "http.uri";
	public static final String HTTP_METHOD_CONFIG_NAME = "http.method";
	public static final String HTTP_URI_EXTENSION_CONFIG_NAME = "http.uri.extension";
	public static final String HTTP_SSL_CONFIG_NAME = "http.ssl";

    public static final String HTTP_STATUS_CODE_CONFIG_NAME = "status.code";

	public static final String MODULE_DEFINITION_XML = "/messaging/http-messaging-definition.xml";

	private static MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

	@Override
	public void beginTransaction(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations) {
		super.beginTransaction(sessionId, transactionId, stepId, from, to, configurations);

		Configuration sslConfig = ConfigurationUtils.getConfiguration(configurations, HTTP_SSL_CONFIG_NAME);
		if (sslConfig != null && sslConfig.getValue() != null && "true".equalsIgnoreCase(sslConfig.getValue())) {
			SessionContext sessionContext = SessionManager.getInstance().getSession(sessionId);
			List<TransactionContext> transactions = sessionContext.getTransactions(transactionId);

			//create an SSLContext and save it to the transaction context
			SSLContext sslContext = SecurityUtils.createSSLContext();

			for(TransactionContext transactionContext : transactions) {
				transactionContext.setParameter(SSLContext.class, sslContext);
			}
		}
	}

	@Override
	public MessagingModule getModuleDefinition() {
		return module;
	}

	@Override
	public ITransactionReceiver getReceiver(SessionContext sessionContext, TransactionContext transactionContext) throws IOException {
		return new HttpReceiver(sessionContext, transactionContext);
	}

	@Override
	public ITransactionSender getSender(SessionContext sessionContext, TransactionContext transactionContext) {
		return new HttpSender(sessionContext, transactionContext);
	}

    @Override
    public ITransactionListener getListener(SessionContext sessionContext, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        return new HttpListener(sessionContext, receiverTransactionContext, senderTransactionContext);
    }
}
