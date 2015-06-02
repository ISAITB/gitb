package com.gitb.messaging.server;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by serbay.
 */
public interface IMessagingServer {
	public Collection<IMessagingServerWorker> getActiveWorkers();
	public IMessagingServerWorker listenNextAvailablePort() throws IOException;
	public void close(int port);
}
