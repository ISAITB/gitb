package com.gitb.engine.actors.processors;

import akka.actor.ActorRef;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.EndTransaction;

/**
 * Created by serbay on 9/29/14.
 *
 * End transaction step executor actor
 */
public class EndTransactionStepProcessorActor extends AbstractTestStepActor<EndTransaction> {
	public static final String NAME = "end-txn-p";

	public EndTransactionStepProcessorActor(EndTransaction step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
	}

	@Override
	protected void init() throws Exception {

	}

	@Override
	protected void start() throws Exception {
		processing();

		TestCaseContext context = scope.getContext();

		for(MessagingContext messagingContext : context.getMessagingContexts()) {
			if(messagingContext.getTransaction(step.getTxnId()) != null) {

				messagingContext
					.getHandler()
					.endTransaction(messagingContext.getSessionId(), step.getTxnId(), step.getId());

				messagingContext.removeTransaction(step.getTxnId());
				break;
			}
		}

		completed();
	}

	@Override
	protected void stop() {

	}

	public static ActorRef create(ActorContext context, EndTransaction step, TestCaseScope scope, String stepId) throws Exception {
		return create(EndTransactionStepProcessorActor.class, context, step, scope, stepId);
	}
}
