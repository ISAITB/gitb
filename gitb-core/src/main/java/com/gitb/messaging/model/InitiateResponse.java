package com.gitb.messaging.model;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by serbay on 9/24/14.
 */
public class InitiateResponse {
	private final String sessionId;
	private final List<ActorConfiguration> actorConfigurations;

	public InitiateResponse(String sessionId, List<ActorConfiguration> actorConfigurations) {
		this.sessionId = sessionId;
		this.actorConfigurations = actorConfigurations;
	}

	public String getSessionId() {
		return sessionId;
	}

	public List<ActorConfiguration> getActorConfigurations() {
		return actorConfigurations;
	}
}
