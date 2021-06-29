package com.gitb.engine.events.model;

import akka.actor.ActorRef;
import com.gitb.core.StepStatus;
import com.gitb.tr.TestStepReportType;

/**
 * Created by serbay on 9/8/14.
 */
public class TestStepStatusEvent extends StatusEvent {
	private final String sessionId;
	private final String stepId;
	private final TestStepReportType report;
	private final ActorRef actorRef;
	private final Object step;

	public TestStepStatusEvent(String sessionId, String stepId, StepStatus status, TestStepReportType report, ActorRef actorRef, Object step) {
		super(status);
		this.sessionId = sessionId;
		this.stepId = stepId;
		this.report = report;
		this.actorRef = actorRef;
		this.step = step;
	}

	public TestStepStatusEvent(String sessionId, String stepId, StepStatus status, TestStepReportType report, Object step) {
		this(sessionId, stepId, status, report, null, step);
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getStepId() {
		return stepId;
	}

	public TestStepReportType getReport() {
		return report;
	}

	public ActorRef getActorRef() {
		return actorRef;
	}

	public Object getStep() {
		return step;
	}

	@Override
	public String toString() {
		return "TestStepStatusEvent: " + sessionId + " - " + stepId + " - " + getStatus().name() + " - " + report;
	}
}
