package com.gitb.engine.events.model;

import com.gitb.core.StepStatus;

/**
 * Created by serbay on 9/17/14.
 */
public class ErrorStatusEvent extends StatusEvent {
	private final Throwable e;

	public ErrorStatusEvent(Throwable e) {
		super(StepStatus.ERROR);
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
