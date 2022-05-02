package com.gitb.engine.messaging.handlers.server;

import com.gitb.engine.messaging.handlers.server.exceptions.ExistingSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by serbay on 9/24/14.
 *
 * Class that keeps the session information related to each messaging server worker instance.
 */
public class NetworkingSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(NetworkingSessionManager.class);
	private Map<InetAddress, SessionInfo> sessions;
	private final int port;

	public NetworkingSessionManager(int port) {
		sessions = new ConcurrentHashMap<>();
		this.port = port;
	}

	public String bindToSession(InetAddress address, String messagingSessionId, String testSessionId) throws ExistingSessionException {
		if(sessions.containsKey(address)) {
			throw new ExistingSessionException(address, messagingSessionId);
		} else {
		    logger.info(MarkerFactory.getDetachedMarker(testSessionId), "Test session ["+testSessionId+"] listening on port ["+port+"] for connections from ["+address+"]");
			sessions.put(address, new SessionInfo(messagingSessionId, testSessionId));
			return messagingSessionId;
		}
	}

	public int getPort() {
		return port;
	}

	public int getNumberOfActiveSessions() {
		return sessions.size();
	}

	public boolean isSessionExists(InetAddress address) {
		return sessions.containsKey(address);
	}

	public SessionInfo getSessionInfo(InetAddress address) {
		return getSessionInfo(address, false);
	}

	public SessionInfo getSessionInfo(InetAddress address, boolean orFirstAvailable) {
		if (sessions.containsKey(address)) {
			return sessions.get(address);
		} else if (orFirstAvailable && !sessions.isEmpty()) {
			return sessions.values().iterator().next();
		}
		return null;
	}

	public void endSession(InetAddress address) {
		sessions.remove(address);
	}

	public Map<InetAddress, SessionInfo> getSessionMap() {
		return Collections.unmodifiableMap(sessions);
	}

	public List<String> getSessions() {
		List<String> addresses = new ArrayList<String>();
		if (sessions != null) {
			for (InetAddress address: sessions.keySet()) {
				addresses.add(address.getHostName());
			}
		}
		return Collections.unmodifiableList(addresses);
	}

	public static class SessionInfo {

		private final String messagingSessionId;
		private final String testSessionId;

		SessionInfo(String messagingSessionId, String testSessionId) {
			this.messagingSessionId = messagingSessionId;
			this.testSessionId = testSessionId;
		}

		public String getMessagingSessionId() {
			return messagingSessionId;
		}

		public String getTestSessionId() {
			return testSessionId;
		}

	}

}
