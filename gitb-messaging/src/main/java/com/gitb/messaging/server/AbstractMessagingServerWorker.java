package com.gitb.messaging.server;

/**
 * Created by serbay.
 */
public abstract class AbstractMessagingServerWorker implements IMessagingServerWorker {

	protected final int port;
	protected final NetworkingSessionManager networkingSessionManager;

	protected AbstractMessagingServerWorker(int port) {
		this.port = port;
		this.networkingSessionManager = new NetworkingSessionManager(port);
	}

	public int getPort() {
		return port;
	}

	public NetworkingSessionManager getNetworkingSessionManager() {
		return networkingSessionManager;
	}
}
