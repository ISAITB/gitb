/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.engine.commands.interaction;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.AnyContent;
import com.gitb.engine.commands.common.SessionCommand;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by serbay on 9/4/14.
 */
public class ConfigureCommand extends SessionCommand {
	private final ActorConfiguration domainConfiguration;
	private final ActorConfiguration organisationConfiguration;
	private final ActorConfiguration systemConfiguration;
    private final List<ActorConfiguration> actorConfigurations;
	private final List<AnyContent> inputs;

	public ConfigureCommand(String sessionId, List<ActorConfiguration> actorConfigurations, ActorConfiguration domainConfiguration, ActorConfiguration organisationConfiguration, ActorConfiguration systemConfiguration, List<AnyContent> inputs) {
		super(sessionId);
		this.actorConfigurations = new CopyOnWriteArrayList<>(actorConfigurations);
		this.domainConfiguration = domainConfiguration;
		this.organisationConfiguration = organisationConfiguration;
		this.systemConfiguration = systemConfiguration;
		this.inputs = inputs;
	}

	public List<ActorConfiguration> getActorConfigurations() {
		return actorConfigurations;
	}

	public ActorConfiguration getDomainConfiguration() {
		return domainConfiguration;
	}

	public ActorConfiguration getOrganisationConfiguration() {
		return organisationConfiguration;
	}

	public ActorConfiguration getSystemConfiguration() {
		return systemConfiguration;
	}

	public List<AnyContent> getInputs() {
		return inputs;
	}
}
