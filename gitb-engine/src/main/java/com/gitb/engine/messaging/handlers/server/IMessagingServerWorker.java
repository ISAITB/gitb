package com.gitb.engine.messaging.handlers.server;

import java.io.IOException;

/**
 * Created by serbay.
 */
public interface IMessagingServerWorker {
	public void start() throws IOException;
	public void stop();
	public boolean isActive();
	public int getPort();
	public NetworkingSessionManager getNetworkingSessionManager();
}
