package com.gitb.engine.actors.processors;

import com.gitb.engine.utils.StepContext;
import org.apache.pekko.actor.ActorRef;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.processors.AssignProcessor;
import com.gitb.engine.processors.IProcessor;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.Assign;

/**
 * Created by serbay on 9/10/14.
 *
 * Assign step executor actor
 */
public class AssignStepProcessorActor extends AbstractProcessorActor<Assign> {

	public static final String NAME = "assign-p";

	private AssignProcessor processor;

	public AssignStepProcessorActor(Assign step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
		initialize();
	}

	protected void initialize() {
		processor = new AssignProcessor(scope);
	}

	@Override
	protected IProcessor getProcessor() {
		return processor;
	}

	public static ActorRef create(ActorContext context, Assign step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception{
		return context.actorOf(props(AssignStepProcessorActor.class, step, scope, stepId, stepContext).withDispatcher(ActorSystem.BLOCKING_DISPATCHER), getName(NAME));
	}
}
