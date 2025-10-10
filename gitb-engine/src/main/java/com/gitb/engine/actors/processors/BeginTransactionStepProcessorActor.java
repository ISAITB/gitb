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

import com.gitb.core.Configuration;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.TransactionContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.tdl.BeginTransaction;
import org.apache.pekko.actor.ActorRef;

/**
 * Created by serbay on 9/29/14.
 * <p>
 * Begin transaction step executor actor
 */
public class BeginTransactionStepProcessorActor extends AbstractTestStepActor<BeginTransaction> {
	public static final String NAME = "begin-txn-p";

	public BeginTransactionStepProcessorActor(BeginTransaction step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
	}

	@Override
	protected void init() {
		// Do nothing.
	}

	@Override
	protected void start() {
		processing();

		String handlerIdentifier = step.getHandler();
		VariableResolver resolver = new VariableResolver(scope);
		if (VariableResolver.isVariableReference(handlerIdentifier)) {
			handlerIdentifier = resolver.resolveVariableAsString(handlerIdentifier).toString();
		}
		if (step.getConfig() != null) {
			for (Configuration config: step.getConfig()) {
				if (VariableResolver.isVariableReference(config.getValue())) {
					config.setValue(resolver.resolveVariableAsString(config.getValue()).toString());
				}
			}
		}

        MessagingContext messagingContext = scope.getContext().getMessagingContext(handlerIdentifier);
        String messagingSessionId = messagingContext.getSessionId();

        messagingContext
                .getHandler()
                .beginTransaction(messagingSessionId, step.getTxnId(), step.getId(), step.getFrom(), step.getTo(), step.getConfig());

        TransactionContext transactionContext = new TransactionContext(step.getTxnId());

        messagingContext.setTransaction(step.getTxnId(), transactionContext);

		completed();
	}

	public static ActorRef create(ActorContext context, BeginTransaction step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		return create(BeginTransactionStepProcessorActor.class, context, step, scope, stepId, stepContext);
	}
}
