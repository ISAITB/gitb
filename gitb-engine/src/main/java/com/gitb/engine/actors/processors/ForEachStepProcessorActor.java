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
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.tdl.ForEachStep;
import com.gitb.types.NumberType;
import org.apache.pekko.actor.ActorRef;

import java.math.BigInteger;

/**
 * Created by serbay on 9/18/14.
 *
 * For each step executor actor
 */
public class ForEachStepProcessorActor extends AbstractIterationStepActor<ForEachStep> {

	public static final String NAME = "foreach-s-p";

	private TestCaseScope childScope;
	private int iteration;

	public ForEachStepProcessorActor(ForEachStep step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
	}

	@Override
	protected void init() throws Exception {
		iteration = 0;
		childScope = createChildScope();
	}

	@Override
	protected void start() throws Exception {
		processing();
		if(!loop()) {
			completed();
		}
	}

	@Override
	protected boolean handleStatusEventInternal(StatusEvent event) throws Exception {
		return loop();
	}

	private boolean loop() throws Exception {
		checkIteration(iteration);

		BigInteger endValue = getNumber(step.getEnd());
		BigInteger startValue = getNumber(step.getStart());

		if(iteration < endValue.intValue() - startValue.intValue()) {
			TestCaseScope.ScopedVariable counter = childScope
				.getVariable(step.getCounter());

			NumberType val = (NumberType) counter.getValue();

			val.setValue((double)(startValue.intValue()+iteration));

			ActorRef child = SequenceProcessorActor.create(getContext(), step.getDo(), childScope, stepId + ITERATION_OPENING_TAG + (iteration + 1) + ITERATION_CLOSING_TAG, stepContext);
			child.tell(new StartCommand(scope.getContext().getSessionId()), self());

			iteration++;
			return true;
		} else {
			return false;
		}
	}

	private BigInteger getNumber(String expression) {
		VariableResolver resolver = new VariableResolver(scope);
		BigInteger number;
		if (VariableResolver.isVariableReference(expression)) {
			number = BigInteger.valueOf(resolver.resolveVariableAsNumber(expression).longValue());
		} else {
			number = BigInteger.valueOf(Long.parseLong(expression));
		}
		return number;
	}

	private TestCaseScope createChildScope() {
		TestCaseScope childScope = scope.createChildScope();

		NumberType start = new NumberType();

		BigInteger startValue = getNumber(step.getStart());
		start.setValue(startValue.doubleValue());

		childScope
			.createVariable(step.getCounter())
			.setValue(start);

		return childScope;
	}

	public static ActorRef create(ActorContext context, ForEachStep step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		return create(ForEachStepProcessorActor.class, context, step, scope, stepId, stepContext);
	}
}
