package com.gitb.engine.actors.processors;

import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
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

	public ForEachStepProcessorActor(ForEachStep step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
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
			completed(); // TODO send test step report
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

			ActorRef child = SequenceProcessorActor.create(getContext(), step.getDo(), childScope, stepId + ITERATION_OPENING_TAG + (iteration + 1) + ITERATION_CLOSING_TAG);
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

	public static ActorRef create(ActorContext context, ForEachStep step, TestCaseScope scope, String stepId) throws Exception {
		return create(ForEachStepProcessorActor.class, context, step, scope, stepId);
	}
}
