package com.gitb.messaging.server.dns;

import com.gitb.messaging.server.Configuration;
import com.gitb.messaging.server.udp.UDPMessagingServer;

import java.io.IOException;

/**
 * Created by serbay.
 */
public class DNSMessagingServer extends UDPMessagingServer {
	private static final int DNS_MESSAGING_SERVER_PORT = 53;
	private static final Configuration DEFAULT_CONFIGURATION = Configuration.defaultConfiguration();

	public DNSMessagingServer() throws IOException {
		super(new Configuration(
			DNS_MESSAGING_SERVER_PORT, DNS_MESSAGING_SERVER_PORT,
			DEFAULT_CONFIGURATION.getIpAddress(), DEFAULT_CONFIGURATION.getActorName(),
			null, null, null));
	}

	@Override
	protected boolean allowAllConnections() {
		return true;
	}
}
