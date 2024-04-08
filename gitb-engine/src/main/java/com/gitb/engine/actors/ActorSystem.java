package com.gitb.engine.actors;

import org.apache.pekko.actor.ActorRef;
import com.gitb.engine.actors.supervisors.SessionSupervisor;

/**
 * Created by serbay on 9/4/14.
 *
 * Test engine Akka actor system management & accessor class
 */
public class ActorSystem {
	public static final String ACTOR_SYSTEM_NAME = "test-engine-as";

	public static final String BLOCKING_DISPATCHER = "blocking-processor-dispatcher";

	private org.apache.pekko.actor.ActorSystem system;

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
