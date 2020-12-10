package com.gitb.engine.commands.interaction;

import com.gitb.engine.commands.common.SessionCommand;

public class ConnectionClosedEvent extends SessionCommand {
	public ConnectionClosedEvent(String sessionId) {
		super(sessionId);
	}
}
