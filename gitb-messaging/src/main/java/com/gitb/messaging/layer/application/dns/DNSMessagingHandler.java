package com.gitb.messaging.layer.application.dns;

import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.messaging.layer.AbstractMessagingHandler;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.model.udp.IDatagramReceiver;
import com.gitb.messaging.model.udp.IDatagramSender;
import com.gitb.messaging.server.IMessagingServer;
import com.gitb.messaging.server.dns.DNSMessagingServer;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by serbay.
 */
@MetaInfServices(IMessagingHandler.class)
public class DNSMessagingHandler extends AbstractMessagingHandler {

	public static final String DNS_DOMAIN_CONFIG_NAME = "dns.domain";
	public static final String DNS_ADDRESS_FIELD_NAME = "dns.address";

	public static final String MODULE_DEFINITION_XML = "/dns-messaging-definition.xml";

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
	public MessagingReport listenMessage(String sessionId, String transactionId, String from, String to, List<Configuration> configurations, Message inputs) {
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
