package com.gitb.engine.commands.interaction;

import com.gitb.engine.commands.common.SessionCommand;

/**
 * Created by serbay on 9/4/14.
 */
public class StopCommand extends SessionCommand {

	private boolean externalStop = false;

	public StopCommand(String sessionId) {
		super(sessionId);
	}

	public StopCommand(String sessionId, boolean externalStop) {
		super(sessionId);
		this.externalStop = externalStop;
	}

	public boolean isExternalStop() {
		return externalStop;
	}
}
