package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.gitb.core.StepStatus;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.commands.interaction.StopCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.RepeatUntilStep;
import com.gitb.types.DataType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by serbay on 9/15/14.
 *
 * Repeat until step executor actor
 */
public class RepeatUntilStepProcessorActor extends AbstractIterationStepActor<RepeatUntilStep> {
	public static final String NAME = "repeat-u-p";

	private ExpressionHandler expressionHandler;
	private Map<Integer, Integer> childActorUidIndexMap;
	private Map<Integer, ActorRef> iterationIndexActorMap;

	private boolean childrenHasError;
	private boolean childrenHasWarning;

	public RepeatUntilStepProcessorActor(RepeatUntilStep step, TestCaseScope scope, String stepId)  {
		super(step, scope, stepId);
	}

	@Override
	protected void init() throws Exception {
		expressionHandler = new ExpressionHandler(scope);
		childActorUidIndexMap = new ConcurrentHashMap<>();
		iterationIndexActorMap = new ConcurrentHashMap<>();

		childrenHasError = false;
	}

	@Override
	protected void start() throws Exception {
		processing();
		loop(0);
	}

	@Override
	protected void stop() {
		StopCommand command = new StopCommand(scope.getContext().getSessionId());
		for(ActorRef child : getContext().getChildren()) {
			child.tell(command, self());
		}
	}

	@Override
	protected void handleStatusEvent(StatusEvent event) throws Exception {
        StepStatus status = event.getStatus();
		if (status == StepStatus.ERROR) {
			childrenHasError = true;
		} else if (status == StepStatus.WARNING) {
			childrenHasWarning = true;
		}
		if (status == StepStatus.ERROR || status == StepStatus.WARNING || status == StepStatus.COMPLETED) {
			int senderUid = getSender().path().uid();
			int iteration = childActorUidIndexMap.get(senderUid);
			boolean condition = evaluateCondition();
			if (condition) {
				checkIteration(iteration+1);
				loop(iteration+1);
			} else {
				if (childrenHasError) {
					childrenHasError();
				} else if (childrenHasWarning) {
					childrenHasWarning();
				} else {
					completed();
				}
			}
		}
	}

	private void loop(int iteration) throws Exception {

		ActorRef iterationActor = SequenceProcessorActor.create(getContext(), step.getDo(), scope, stepId + ITERATION_OPENING_TAG + (iteration + 1) + ITERATION_CLOSING_TAG);

		childActorUidIndexMap.put(iterationActor.path().uid(), iteration);
		iterationIndexActorMap.put(iteration, iterationActor);

		StartCommand command = new StartCommand(scope.getContext().getSessionId());
		iterationActor.tell(command, self());
	}

	private boolean evaluateCondition() throws Exception {

		boolean condition = (boolean) expressionHandler
			.processExpression(step.getCond(), DataType.BOOLEAN_DATA_TYPE)
			.getValue();

		return condition;
	}

	public static ActorRef create(ActorContext context, RepeatUntilStep step, TestCaseScope scope, String stepId) throws Exception {
		return create(RepeatUntilStepProcessorActor.class, context, step, scope, stepId);
	}
}
