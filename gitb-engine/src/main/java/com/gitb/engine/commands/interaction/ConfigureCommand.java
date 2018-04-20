package com.gitb.engine.commands.interaction;

import com.gitb.core.ActorConfiguration;
import com.gitb.engine.commands.common.SessionCommand;
import com.gitb.core.Configuration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by serbay on 9/4/14.
 */
public class ConfigureCommand extends SessionCommand {
	private final ActorConfiguration domainConfiguration;
    private final List<ActorConfiguration> actorConfigurations;

	public ConfigureCommand(String sessionId, List<ActorConfiguration> actorConfigurations, ActorConfiguration domainConfiguration) {
		super(sessionId);
		this.actorConfigurations = new CopyOnWriteArrayList<>(actorConfigurations);
		this.domainConfiguration = domainConfiguration;
	}

	public List<ActorConfiguration> getActorConfigurations() {
		return actorConfigurations;
	}

	public ActorConfiguration getDomainConfiguration() {
		return domainConfiguration;
	}
}
