package com.gitb.engine.actors.processors;

import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.processors.IProcessor;
import com.gitb.engine.processors.VerifyProcessor;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.Verify;
import org.apache.pekko.actor.ActorRef;
import scala.concurrent.ExecutionContext;

/**
 * Created by serbay on 9/10/14.
 *
 * Verify test step executor actor
 */
public class VerifyStepProcessorActor extends AbstractProcessorActor<Verify> {

	public static final String NAME = "verify-p";

	private VerifyProcessor processor;

	public VerifyStepProcessorActor(Verify step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
		initialize();
	}

	protected void initialize() {
		processor = new VerifyProcessor(scope);
	}

	@Override
	protected IProcessor getProcessor() {
		return processor;
	}

	public static ActorRef create(ActorContext context, Verify step, TestCaseScope scope, String stepId) throws Exception {
		return context.actorOf(props(VerifyStepProcessorActor.class, step, scope, stepId).withDispatcher(ActorSystem.BLOCKING_DISPATCHER), getName(NAME));
	}

	@Override
	protected ExecutionContext stepDispatcher() {
		return getContext().getSystem().dispatchers().lookup(ActorSystem.BLOCKING_IO_DISPATCHER);
	}
}
