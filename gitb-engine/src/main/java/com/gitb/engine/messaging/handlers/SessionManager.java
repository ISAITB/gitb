package com.gitb.engine.messaging.handlers;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.server.IMessagingServer;
import com.gitb.engine.messaging.handlers.server.IMessagingServerWorker;
import com.gitb.engine.messaging.handlers.server.exceptions.ExistingSessionException;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.ms.InitiateResponse;
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

	private final Map<String, SessionContext> sessions;

	private SessionManager() {
		sessions = new ConcurrentHashMap<>();
	}

	public synchronized InitiateResponse createSession(String testSessionId, AbstractMessagingHandler messagingHandler, IMessagingServer server, List<ActorConfiguration> configurations) throws IOException, ExistingSessionException {
		String sessionId = UUID.randomUUID().toString();
		SessionContext sessionContext = new SessionContext(sessionId, messagingHandler, configurations, server, testSessionId);
		if (messagingHandler.needsMessagingServerWorker()) {
			Map<ActorConfiguration, IMessagingServerWorker> availableWorkers = new HashMap<>();
			for (ActorConfiguration actorConfiguration : configurations) {
				var address = extractInetAddress(actorConfiguration);
				if (address.isPresent()) {
					IMessagingServerWorker availableWorker = null;
					for (IMessagingServerWorker worker : server.getActiveWorkers()) {
						if (!worker.getNetworkingSessionManager().isSessionExists(address.get())) {
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
								.bindToSession(address.get(), sessionId, testSessionId);

						sessionContext
								.getServerActorConfigurations()
								.add(serverActorConfiguration);
						sessionContext
								.setWorker(actorConfiguration.getActor(), actorConfiguration.getEndpoint(), availableWorker);
					}
				}
			}
			for (Map.Entry<ActorConfiguration, IMessagingServerWorker> entry : availableWorkers.entrySet()) {
				if (entry.getValue() == null) {
					endAllSessions(availableWorkers);
					throw new GITBEngineInternalError("No available port is left for the actor at [" + extractInetAddress(entry.getKey()) + "]");
				}
			}
		}
		sessions.put(sessionId, sessionContext);
		var response = new InitiateResponse();
		response.setSessionId(sessionId);
		response.getActorConfiguration().addAll(sessionContext.getServerActorConfigurations());
		return response;
	}

	private void endAllSessions(Map<ActorConfiguration, IMessagingServerWorker> availableWorkers) {
		for (Map.Entry<ActorConfiguration, IMessagingServerWorker> entry : availableWorkers.entrySet()) {
			if (entry.getValue() != null) {
				var address = extractInetAddress(entry.getKey());
                address.ifPresent(inetAddress -> entry.getValue().getNetworkingSessionManager().endSession(inetAddress));
			}
		}
	}

	private Optional<InetAddress> extractInetAddress(ActorConfiguration actorConfiguration) {
		InetAddress ipAddress = null;
		if (actorConfiguration != null && actorConfiguration.getConfig().isEmpty()) {
			Configuration ipAddressConfig = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.IP_ADDRESS_CONFIG_NAME);
			if (ipAddressConfig != null) {
				try {
					ipAddress = InetAddress.getByName(ipAddressConfig.getValue());
				} catch (UnknownHostException e) {
					throw new GITBEngineInternalError("Unable to lookup address ["+ipAddressConfig.getValue()+"]");
				}
			}
		}
		return Optional.ofNullable(ipAddress);
	}

	public SessionContext getSession(String sessionId) {
		return sessions.get(sessionId);
	}

	public synchronized void endSession(String sessionId) {
		SessionContext context = sessions.remove(sessionId);

		if (context != null) {
			context.end();

			for (ActorConfiguration actorConfiguration : context.getActorConfigurations()) {
				var address = extractInetAddress(actorConfiguration);
                address.ifPresent(inetAddress -> context.getWorker(actorConfiguration.getActor(), actorConfiguration.getEndpoint())
                        .getNetworkingSessionManager()
                        .endSession(inetAddress));
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
