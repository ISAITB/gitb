package com.gitb.engine.actors.processors;

import akka.actor.ActorRef;
import com.gitb.core.StepStatus;
import com.gitb.engine.actors.SessionActor;
import com.gitb.engine.actors.util.ActorUtils;
import com.gitb.engine.commands.interaction.PrepareForStopCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.ExitStep;
import com.gitb.tr.SR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.utils.XMLDateTimeUtils;

/**
 * Created by serbay on 9/15/14.
 *
 * Exit step executor actor
 */
public class ExitStepProcessorActor extends AbstractTestStepActor<ExitStep> {

	public static final String NAME = "exit-s-p";

	public ExitStepProcessorActor(ExitStep step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
	}

	protected void start() throws Exception {
		processing();
		String sessionId = scope.getContext().getSessionId();

		VariableResolver resolver = new VariableResolver(scope);
		boolean isSuccess;
		if (resolver.isVariableReference(step.getSuccess())) {
			isSuccess = (Boolean)resolver.resolveVariableAsBoolean(step.getSuccess()).getValue();
		} else {
			isSuccess = Boolean.parseBoolean(step.getSuccess());
		}
		TestStepReportType report = new SR();
		report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
		StatusEvent status;
		if (isSuccess) {
			report.setResult(TestResultType.SUCCESS);
			status = new StatusEvent(StepStatus.COMPLETED);
		} else {
			report.setResult(TestResultType.FAILURE);
			status = new StatusEvent(StepStatus.ERROR);
		}
		// Prepare the rest of the test case for the stop.
		ActorUtils.askBlocking(getContext().system().actorSelection(SessionActor.getPath(sessionId)), new PrepareForStopCommand(sessionId, self()));
		// Send the step's report.
		updateTestStepStatus(getContext(), status, report, true, false);
	}

	@Override
	protected void init() {
	}

	@Override
	protected void stop() {
	}

	public static ActorRef create(ActorContext context, ExitStep step, TestCaseScope scope, String stepId) throws Exception{
		return create(ExitStepProcessorActor.class, context, step, scope, stepId);
	}

}
