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

package com.gitb.engine.events.model;

import org.apache.pekko.actor.ActorRef;
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
