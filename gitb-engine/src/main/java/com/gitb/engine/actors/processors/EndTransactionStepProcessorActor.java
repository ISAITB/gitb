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

import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.tdl.EndTransaction;
import org.apache.pekko.actor.ActorRef;

/**
 * Created by serbay on 9/29/14.
 * <p>
 * End transaction step executor actor
 */
public class EndTransactionStepProcessorActor extends AbstractTestStepActor<EndTransaction> {
	public static final String NAME = "end-txn-p";

	public EndTransactionStepProcessorActor(EndTransaction step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId,stepContext);
	}

	@Override
	protected void init() {
		// Do nothing.
	}

	@Override
	protected void start() {
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

	public static ActorRef create(ActorContext context, EndTransaction step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		return create(EndTransactionStepProcessorActor.class, context, step, scope, stepId, stepContext);
	}
}
