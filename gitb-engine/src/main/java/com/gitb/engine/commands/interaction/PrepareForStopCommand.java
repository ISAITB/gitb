package com.gitb.engine.commands.interaction;

import org.apache.pekko.actor.ActorRef;
import com.gitb.engine.commands.common.SessionCommand;

/**
 * Command to prepare the test session to be stopped.
 *
 * This records the original source of the request to avoid cyclic notifications.
 *
 * Created by simatosc.
 */
public class PrepareForStopCommand extends SessionCommand {

	private ActorRef originalSource;

	public PrepareForStopCommand(String sessionId, ActorRef originalSource) {
		super(sessionId);
		this.originalSource = originalSource;
	}

	public ActorRef getOriginalSource() {
		return originalSource;
	}
}
