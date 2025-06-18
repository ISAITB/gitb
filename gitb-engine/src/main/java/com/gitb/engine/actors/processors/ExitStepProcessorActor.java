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

package com.gitb.engine.actors.processors;

import com.gitb.core.StepStatus;
import com.gitb.engine.actors.SessionActor;
import com.gitb.engine.commands.interaction.PrepareForStopCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.tdl.ExitStep;
import com.gitb.tr.SR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.utils.XMLDateTimeUtils;
import org.apache.pekko.actor.ActorRef;

/**
 * Created by serbay on 9/15/14.
 *
 * Exit step executor actor
 */
public class ExitStepProcessorActor extends AbstractTestStepActor<ExitStep> {

	public static final String NAME = "exit-s-p";

	public ExitStepProcessorActor(ExitStep step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
	}

	protected void start() throws Exception {
		processing();

		VariableResolver resolver = new VariableResolver(scope);
		boolean isSuccess;
		if (VariableResolver.isVariableReference(step.getSuccess())) {
			isSuccess = (Boolean)resolver.resolveVariableAsBoolean(step.getSuccess()).getValue();
		} else {
			isSuccess = Boolean.parseBoolean(step.getSuccess());
		}
		boolean isUndefined;
		if (VariableResolver.isVariableReference(step.getUndefined())) {
			isUndefined = (Boolean)resolver.resolveVariableAsBoolean(step.getUndefined()).getValue();
		} else {
			isUndefined = Boolean.parseBoolean(step.getUndefined());
		}
		TestStepReportType report = new SR();
		report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
		StatusEvent status;
		if (isSuccess) {
			report.setResult(TestResultType.SUCCESS);
			status = new StatusEvent(StepStatus.COMPLETED, scope, self());
			scope.getContext().setForcedFinalResult(TestResultType.SUCCESS);
		} else if (isUndefined) {
			report.setResult(TestResultType.SUCCESS);
			status = new StatusEvent(StepStatus.COMPLETED, scope, self());
			scope.getContext().setForcedFinalResult(TestResultType.UNDEFINED);
		} else {
			report.setResult(TestResultType.FAILURE);
			status = new StatusEvent(StepStatus.ERROR, scope, self());
			scope.getContext().setForcedFinalResult(TestResultType.FAILURE);
		}
		// Prepare the rest of the test case for the stop.
		if (scope.getContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPING && scope.getContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPED) {
			scope.getContext().setCurrentState(TestCaseContext.TestCaseStateEnum.STOPPING);
		}
		getContext().system().actorSelection(SessionActor.getPath(scope.getContext().getSessionId())).tell(new PrepareForStopCommand(scope.getContext().getSessionId(), self()), self());
		// Send the step's report.
		updateTestStepStatus(getContext(), status, report, true, false);
	}

	@Override
	protected void init() {
	}

	public static ActorRef create(ActorContext context, ExitStep step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception{
		return create(ExitStepProcessorActor.class, context, step, scope, stepId, stepContext);
	}

}
