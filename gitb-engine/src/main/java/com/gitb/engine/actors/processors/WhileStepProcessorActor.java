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

import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.tdl.WhileStep;
import com.gitb.types.DataType;
import org.apache.pekko.actor.ActorRef;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by serbay on 9/12/14.
 *
 * While step executor actor
 */
public class WhileStepProcessorActor extends AbstractIterationStepActor<WhileStep> {

	public static final String NAME = "while-s-p";

	private ExpressionHandler expressionHandler;
	private Map<Integer, Integer> childActorUidIndexMap;
	private Map<Integer, ActorRef> iterationIndexActorMap;

	public WhileStepProcessorActor(WhileStep step, TestCaseScope scope, String stepId, StepContext stepContext){
		super(step, scope, stepId, stepContext);
	}

	@Override
	protected void init() throws Exception {
		expressionHandler = new ExpressionHandler(scope);
		childActorUidIndexMap = new ConcurrentHashMap<>();
		iterationIndexActorMap = new ConcurrentHashMap<>();
	}

	@Override
	protected void start() throws Exception {
		processing();
		boolean started = loop(0);
		if(!started) {
			completed(); // TODO send test step report
		}
	}

	@Override
	protected boolean handleStatusEventInternal(StatusEvent event) throws Exception {
		int senderUid = getSender().path().uid();
		int iteration = childActorUidIndexMap.get(senderUid);
		return loop(iteration+1);
    }

	private boolean loop(int iteration) throws Exception {
		checkIteration(iteration);

		boolean condition = (boolean) expressionHandler.processExpression(step.getCond(), DataType.BOOLEAN_DATA_TYPE).getValue();

		if(condition) {
			ActorRef iterationActor = SequenceProcessorActor.create(getContext(), step.getDo(), scope, stepId + ITERATION_OPENING_TAG + (iteration + 1) + ITERATION_CLOSING_TAG, stepContext);

			childActorUidIndexMap.put(iterationActor.path().uid(), iteration);
			iterationIndexActorMap.put(iteration, iterationActor);

			StartCommand command = new StartCommand(scope.getContext().getSessionId());
			iterationActor.tell(command, self());
		}

		return condition;
	}

	public static ActorRef create(ActorContext context, WhileStep step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		return create(WhileStepProcessorActor.class, context, step, scope, stepId, stepContext);
	}
}
