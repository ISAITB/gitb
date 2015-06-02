package com.gitb.messaging.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serbay.
 */
public abstract class AbstractMessagingServerWorker implements IMessagingServerWorker {

	private static Logger logger = LoggerFactory.getLogger(AbstractMessagingServerWorker.class);

	protected final int port;
	protected final NetworkingSessionManager networkingSessionManager;

	protected AbstractMessagingServerWorker(int port) {
		this.port = port;
		this.networkingSessionManager = new NetworkingSessionManager();
	}

	public int getPort() {
		return port;
	}

	public NetworkingSessionManager getNetworkingSessionManager() {
		return networkingSessionManager;
	}
}
