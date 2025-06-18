/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
