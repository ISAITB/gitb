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

	public TestStepStatusEvent(String sessionId, String stepId, StepStatus status, TestStepReportType report, ActorRef actorRef) {
		super(status);
		this.sessionId = sessionId;
		this.stepId = stepId;
		this.report = report;
		this.actorRef = actorRef;
	}

	public TestStepStatusEvent(String sessionId, String stepId, StepStatus status, TestStepReportType report) {
		super(status);
		this.sessionId = sessionId;
		this.stepId = stepId;
		this.report = report;
		this.actorRef = null;
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

	@Override
	public String toString() {
		return "TestStepStatusEvent: " + sessionId + " - " + stepId + " - " + getStatus().name() + " - " + report;
	}
}
