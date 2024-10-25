package com.gitb.engine.commands.interaction;

import com.gitb.engine.commands.common.SessionCommand;

/**
 * Created by serbay on 9/4/14.
 */
public class RestartCommand extends SessionCommand {

	private final String newSessionId;

	public RestartCommand(String sessionId, String newSessionId) {
		super(sessionId);
		this.newSessionId = newSessionId;
	}

	public String getNewSessionId() {
		return newSessionId;
	}
}
