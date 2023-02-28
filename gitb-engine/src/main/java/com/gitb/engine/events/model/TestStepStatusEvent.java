package com.gitb.engine.events.model;

import akka.actor.ActorRef;
import com.gitb.core.StepStatus;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tr.TestStepReportType;

/**
 * Created by serbay on 9/8/14.
 */
public class TestStepStatusEvent extends StatusEvent {
	private final String sessionId;
	private final String stepId;
	private final TestStepReportType report;
	private final Object step;

	public TestStepStatusEvent(String sessionId, String stepId, StepStatus status, TestStepReportType report, ActorRef actorRef, Object step, TestCaseScope scope) {
		super(status, scope, actorRef);
		this.sessionId = sessionId;
		this.stepId = stepId;
		this.report = report;
		this.step = step;
	}

	public TestStepStatusEvent(String sessionId, String stepId, StepStatus status, TestStepReportType report, Object step, TestCaseScope scope) {
		this(sessionId, stepId, status, report, null, step, scope);
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

	public Object getStep() {
		return step;
	}

	@Override
	public String toString() {
		return "TestStepStatusEvent: " + sessionId + " - " + stepId + " - " + getStatus().name() + " - " + report;
	}
}
