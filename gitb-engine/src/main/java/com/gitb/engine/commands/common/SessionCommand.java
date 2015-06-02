package com.gitb.engine.commands.common;

import java.io.Serializable;

/**
 * Created by serbay on 9/4/14.
 */
public class SessionCommand implements Serializable {
	private final String sessionId;

	public SessionCommand(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getSessionId() {
		return sessionId;
	}
}
