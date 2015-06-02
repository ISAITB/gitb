package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
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

	public AssignStepProcessorActor(Assign step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
		initialize();
	}

	protected void initialize() {
		processor = new AssignProcessor(scope);
	}

	@Override
	protected IProcessor getProcessor() {
		return processor;
	}

	public static ActorRef create(ActorContext context, Assign step, TestCaseScope scope, String stepId) throws Exception{
		return context.actorOf(props(AssignStepProcessorActor.class, step, scope, stepId).withDispatcher(ActorSystem.BLOCKING_DISPATCHER), getName(NAME));
	}
}
