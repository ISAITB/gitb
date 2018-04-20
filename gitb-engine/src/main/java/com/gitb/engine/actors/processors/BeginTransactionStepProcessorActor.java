package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.TransactionContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.BeginTransaction;

/**
 * Created by serbay on 9/29/14.
 *
 * Begin transaction step executor actor
 */
public class BeginTransactionStepProcessorActor extends AbstractTestStepActor<BeginTransaction> {
	public static final String NAME = "begin-txn-p";

	public BeginTransactionStepProcessorActor(BeginTransaction step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
	}

	@Override
	protected void init() throws Exception {
	}

	@Override
	protected void start() throws Exception {
		processing();

		String handlerIdentifier = step.getHandler();
		VariableResolver resolver = new VariableResolver(scope);
		if (resolver.isVariableReference(handlerIdentifier)) {
			handlerIdentifier = resolver.resolveVariableAsString(handlerIdentifier).toString();
		}

        MessagingContext messagingContext = scope.getContext().getMessagingContext(handlerIdentifier);
        String messagingSessionId = messagingContext.getSessionId();

        messagingContext
                .getHandler()
                .beginTransaction(messagingSessionId, step.getTxnId(), step.getFrom(), step.getTo(), step.getConfig());

        TransactionContext transactionContext = new TransactionContext(step.getTxnId());

        messagingContext.setTransaction(step.getTxnId(), transactionContext);

		completed();
	}

	@Override
	protected void stop() {
        
	}

	public static ActorRef create(ActorContext context, BeginTransaction step, TestCaseScope scope, String stepId) throws Exception {
		return create(BeginTransactionStepProcessorActor.class, context, step, scope, stepId);
	}
}
