package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.gitb.core.StepStatus;
import com.gitb.engine.actors.SessionActor;
import com.gitb.engine.actors.util.ActorUtils;
import com.gitb.engine.commands.interaction.PrepareForStopCommand;
import com.gitb.engine.commands.interaction.StopCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.ExitStep;

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
			isSuccess = Boolean.valueOf(step.getSuccess());
		}
		StatusEvent status;
		if (isSuccess) {
			status = new StatusEvent(StepStatus.COMPLETED);
		} else {
			status = new StatusEvent(StepStatus.ERROR);
		}
		// Prepare the rest of the test case for the stop.
		ActorUtils.askBlocking(getContext().system().actorFor(SessionActor.getPath(sessionId)), new PrepareForStopCommand(sessionId, self()));
		// Send the step's report.
		updateTestStepStatus(getContext(), status, null, true, false);
		// Stop the rest of the test case.
		getContext()
				.system()
				.actorSelection(SessionActor.getPath(sessionId))
				.tell(new StopCommand(sessionId), self());
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
