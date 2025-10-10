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

package com.gitb.engine.actors;

import org.apache.pekko.actor.ActorRef;
import com.gitb.engine.actors.supervisors.SessionSupervisor;

/**
 * Created by serbay on 9/4/14.
 * <p>
 * Test engine Akka actor system management & accessor class
 */
public class ActorSystem {
	public static final String ACTOR_SYSTEM_NAME = "test-engine-as";

	public static final String BLOCKING_DISPATCHER = "blocking-processor-dispatcher";
	public static final String BLOCKING_IO_DISPATCHER = "blocking-io-dispatcher";

	private final org.apache.pekko.actor.ActorSystem system;

	private ActorRef sessionSupervisor;

	public ActorSystem() {
		system = org.apache.pekko.actor.ActorSystem.create(ACTOR_SYSTEM_NAME);

		init();
	}

	private void init() {
		sessionSupervisor = SessionSupervisor.create(system);
	}

	public org.apache.pekko.actor.ActorSystem getActorSystem() {
		return system;
	}

	public ActorRef getSessionSupervisor() {
		return sessionSupervisor;
	}

	public void shutdown() {
		system.terminate();
	}
}
