package com.gitb.messaging;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.layer.AbstractMessagingHandler;
import com.gitb.messaging.model.InitiateResponse;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.server.IMessagingServer;
import com.gitb.messaging.server.IMessagingServerWorker;
import com.gitb.messaging.server.exceptions.ExistingSessionException;
import com.gitb.utils.ConfigurationUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by serbay on 9/24/14.
 */
public class SessionManager {
	private static SessionManager instance;

	private Map<String, SessionContext> sessions;

	private SessionManager() {
		sessions = new ConcurrentHashMap<>();
	}

	public synchronized InitiateResponse createSession(String testSessionId, AbstractMessagingHandler messagingHandler, IMessagingServer server, List<ActorConfiguration> configurations) throws IOException, ExistingSessionException {
		String sessionId = UUID.randomUUID().toString();

		SessionContext sessionContext = new SessionContext(sessionId, messagingHandler, configurations, server);

		Map<ActorConfiguration, IMessagingServerWorker> availableWorkers = new HashMap<>();

		for (ActorConfiguration actorConfiguration : configurations) {
			InetAddress address = extractInetAddress(actorConfiguration);

			IMessagingServerWorker availableWorker = null;

			for (IMessagingServerWorker worker : server.getActiveWorkers()) {
				if (!worker.getNetworkingSessionManager().isSessionExists(address)) {
					availableWorker = worker;
					break;
				}
			}

			if (availableWorker == null) {
				availableWorker = server.listenNextAvailablePort();
			} else if(!availableWorker.isActive()) {
                availableWorker.start();
            }

			availableWorkers.put(actorConfiguration, availableWorker);

			if (availableWorker != null) {
				ActorConfiguration serverActorConfiguration = ServerUtils.getActorConfiguration(actorConfiguration.getActor(), actorConfiguration.getEndpoint(), availableWorker.getPort());

				availableWorker
					.getNetworkingSessionManager()
					.bindToSession(address, sessionId, testSessionId);

				sessionContext
					.getServerActorConfigurations()
					.add(serverActorConfiguration);
				sessionContext
					.setWorker(actorConfiguration.getActor(), actorConfiguration.getEndpoint(), availableWorker);
			}
		}

		for (Map.Entry<ActorConfiguration, IMessagingServerWorker> entry : availableWorkers.entrySet()) {
			if (entry.getValue() == null) {
				endAllSessions(availableWorkers);

				throw new GITBEngineInternalError("No available port is left for the actor at [" + extractInetAddress(entry.getKey()) + "]");
			}
		}

		sessions.put(sessionId, sessionContext);

		return new InitiateResponse(sessionId, sessionContext.getServerActorConfigurations());
	}

	private void endAllSessions(Map<ActorConfiguration, IMessagingServerWorker> availableWorkers) throws UnknownHostException {
		for (Map.Entry<ActorConfiguration, IMessagingServerWorker> entry : availableWorkers.entrySet()) {
			if (entry.getValue() != null) {
				InetAddress address = extractInetAddress(entry.getKey());
				entry.getValue().getNetworkingSessionManager().endSession(address);
			}
		}
	}

	private InetAddress extractInetAddress(ActorConfiguration actorConfiguration) throws UnknownHostException {
		Configuration ipAddressConfig = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.IP_ADDRESS_CONFIG_NAME);
		return InetAddress.getByName(ipAddressConfig.getValue());
	}

	public SessionContext getSession(String sessionId) {
		return sessions.get(sessionId);
	}

	public synchronized void endSession(String sessionId) throws UnknownHostException {
		SessionContext context = sessions.remove(sessionId);

		if (context != null) {
			context.end();

			for (ActorConfiguration actorConfiguration : context.getActorConfigurations()) {
				InetAddress address = extractInetAddress(actorConfiguration);

				context.getWorker(actorConfiguration.getActor(), actorConfiguration.getEndpoint())
					.getNetworkingSessionManager()
					.endSession(address);

				context.removeWorker(actorConfiguration.getActor(), actorConfiguration.getEndpoint());
			}
		}
	}

	public synchronized static SessionManager getInstance() {
		if (instance == null) {
			instance = new SessionManager();
		}
		return instance;
	}

}
