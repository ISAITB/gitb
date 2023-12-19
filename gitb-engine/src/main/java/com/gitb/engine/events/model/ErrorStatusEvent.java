package com.gitb.engine.events.model;

import org.apache.pekko.actor.ActorRef;
import com.gitb.core.StepStatus;
import com.gitb.engine.testcase.TestCaseScope;

/**
 * Created by serbay on 9/17/14.
 */
public class ErrorStatusEvent extends StatusEvent {
	private final Throwable e;

	public ErrorStatusEvent(Throwable e, TestCaseScope scope, ActorRef sender) {
		super(StepStatus.ERROR, scope, sender);
		this.e = e;
	}

	public Throwable getException() {
		return e;
	}

	@Override
	public String toString() {
		return "ErrorStatusEvent{" +
			"e=" + e +
			'}';
	}
}
