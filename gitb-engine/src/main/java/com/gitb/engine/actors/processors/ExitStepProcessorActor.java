package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.gitb.engine.actors.SessionActor;
import com.gitb.engine.commands.interaction.StopCommand;
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

	@Override
	protected void init() throws Exception {
	}

	@Override
	protected void start() throws Exception {
		processing();

		String sessionId = scope.getContext().getSessionId();

		getContext()
			.system()
			.actorSelection(SessionActor.getPath(sessionId))
			.tell(new StopCommand(sessionId), self());

		completed(); // TODO send test step report
	}

	@Override
	protected void stop() {

	}

	public static ActorRef create(ActorContext context, ExitStep step, TestCaseScope scope, String stepId) throws Exception{
		return create(ExitStepProcessorActor.class, context, step, scope, stepId);
	}
}
