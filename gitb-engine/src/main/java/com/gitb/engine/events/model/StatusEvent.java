package com.gitb.engine.events.model;

import com.gitb.core.StepStatus;
import com.gitb.engine.testcase.TestCaseScope;

/**
 * Created by serbay on 9/11/14.
 */
public class StatusEvent {
	private final StepStatus status;
	private final TestCaseScope scope;

	public StatusEvent(StepStatus status, TestCaseScope scope) {
		this.status = status;
		this.scope = scope;
	}

	public StepStatus getStatus() {
		return status;
	}

	public TestCaseScope getScope() {
		return scope;
	}

	@Override
	public String toString() {
		return "StatusEvent: " + status.name();
	}
}
