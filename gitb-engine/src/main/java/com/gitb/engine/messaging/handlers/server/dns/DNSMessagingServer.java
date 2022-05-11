package com.gitb.engine.messaging.handlers.server.dns;

import com.gitb.engine.messaging.handlers.server.Configuration;
import com.gitb.engine.messaging.handlers.server.udp.UDPMessagingServer;

import java.io.IOException;

import static com.gitb.engine.TestEngineConfiguration.DEFAULT_MESSAGING_CONFIGURATION;

/**
 * Created by serbay.
 */
public class DNSMessagingServer extends UDPMessagingServer {
	private static final int DNS_MESSAGING_SERVER_PORT = 53;

	public DNSMessagingServer() throws IOException {
		super(new Configuration(
			DNS_MESSAGING_SERVER_PORT, DNS_MESSAGING_SERVER_PORT,
			DEFAULT_MESSAGING_CONFIGURATION.getIpAddress(), DEFAULT_MESSAGING_CONFIGURATION.getActorName(),
			null, null, null));
	}

	@Override
	protected boolean allowAllConnections() {
		return true;
	}
}
