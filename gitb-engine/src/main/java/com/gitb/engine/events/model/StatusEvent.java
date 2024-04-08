package com.gitb.engine.events.model;

import org.apache.pekko.actor.ActorRef;
import com.gitb.core.StepStatus;
import com.gitb.engine.testcase.TestCaseScope;

/**
 * Created by serbay on 9/11/14.
 */
public class StatusEvent {
	private final StepStatus status;
	private final TestCaseScope scope;
	private final ActorRef sender;

	public StatusEvent(StepStatus status, TestCaseScope scope, ActorRef sender) {
		this.status = status;
		this.scope = scope;
		this.sender = sender;
	}

	public StepStatus getStatus() {
		return status;
	}

	public TestCaseScope getScope() {
		return scope;
	}

	public ActorRef getSender() {
		return sender;
	}

	@Override
	public String toString() {
		return "StatusEvent: " + status.name();
	}
}
