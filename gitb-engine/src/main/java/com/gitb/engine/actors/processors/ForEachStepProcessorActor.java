package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.gitb.core.StepStatus;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.commands.interaction.StopCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.ForEachStep;
import com.gitb.types.NumberType;

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
	private boolean childrenHasError;

	public ForEachStepProcessorActor(ForEachStep step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
	}

	@Override
	protected void init() throws Exception {
		iteration = 0;
		childScope = createChildScope();
		childrenHasError = false;
	}

	@Override
	protected void start() throws Exception {
		processing();
		if(!loop()) {
			completed(); // TODO send test step report
		}
	}

	@Override
	protected void stop() {
		StopCommand command = new StopCommand(scope.getContext().getSessionId());
		for(ActorRef child : getContext().getChildren()) {
			child.tell(command, self());
		}

		childScope.destroy();
	}

	@Override
	protected void handleStatusEvent(StatusEvent event) throws Exception {
		StepStatus status = event.getStatus();

		switch (status) {
			case ERROR:
				childrenHasError = true; // intentional fall through - no break statement
			case COMPLETED:
				if(!loop()) {
					if(childrenHasError) {
						childrenHasError();
					} else {
						completed();
					}
				}
				break;
		}
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
		BigInteger number = BigInteger.valueOf(0L);
		if (resolver.isVariableReference(expression)) {
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
