package com.gitb.engine.events.model;

import com.gitb.core.StepStatus;

/**
 * Created by serbay on 9/11/14.
 */
public class StatusEvent {
	private final StepStatus status;

	public StatusEvent(StepStatus status) {
		this.status = status;
	}

	public StepStatus getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return "StatusEvent: " + status.name();
	}
}
