package com.gitb.messaging.server.exceptions;

import java.net.InetAddress;

/**
 * Created by serbay on 9/24/14.
 */
public class ExistingSessionException extends Exception {
	private final InetAddress address;
	private final String sessionId;

	public ExistingSessionException(InetAddress address) {
		this.address = address;
		this.sessionId = null;
	}

	public ExistingSessionException(InetAddress address, String sessionId) {
		this.address = address;
		this.sessionId = sessionId;
	}

	@Override
	public String toString() {
		return "ExistingSessionException{" +
			"address=" + address +
			", sessionId='" + sessionId + '\'' +
			'}';
	}
}
