package com.gitb.engine.messaging.handlers.layer.application.dns;

import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.engine.messaging.handlers.model.udp.IDatagramReceiver;
import com.gitb.engine.messaging.handlers.model.udp.IDatagramSender;
import com.gitb.engine.messaging.handlers.server.IMessagingServer;
import com.gitb.engine.messaging.handlers.server.dns.DNSMessagingServer;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by serbay.
 */
@MessagingHandler(name="DNSMessaging")
public class DNSMessagingHandler extends AbstractMessagingHandler {

	public static final String DNS_DOMAIN_CONFIG_NAME = "dns.domain";
	public static final String DNS_ADDRESS_FIELD_NAME = "dns.address";

	public static final String MODULE_DEFINITION_XML = "/messaging/dns-messaging-definition.xml";

	private static final Logger logger = LoggerFactory.getLogger(DNSMessagingHandler.class);

	private static final MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

	/**
	 * DNS server that listens UDP/53.
	 * Access using the {@link #getMessagingServer()}
	 */
	private static IMessagingServer dnsServer;

	@Override
	public MessagingModule getModuleDefinition() {
		return module;
	}

	@Override
	public MessagingReport listenMessage(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations, Message inputs) {
		throw new GITBEngineInternalError("Operation is not supported for the ["+getModuleDefinition().getId()+"]");
	}

	@Override
	public IDatagramReceiver getDatagramReceiver(SessionContext sessionContext, TransactionContext transactionContext) {
		return new DNSReceiver(sessionContext, transactionContext);
	}

	@Override
	public IDatagramSender getDatagramSender(SessionContext sessionContext, TransactionContext transactionContext) {
		return new DNSSender(sessionContext, transactionContext);
	}

	protected IMessagingServer getMessagingServer() {
		if(dnsServer == null) {
			try {
				dnsServer = new DNSMessagingServer();
			} catch (IOException e) {
				logger.error(addMarker(), "An error occurred while creating a DNS server instance", e);
			}
		}

		return dnsServer;
	}

}
