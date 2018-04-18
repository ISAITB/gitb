package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.gitb.core.StepStatus;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.commands.interaction.StopCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.FlowStep;
import com.gitb.tdl.Sequence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by serbay on 9/12/14.
 *
 * Flow step executor actor
 */
public class FlowStepProcessorActor extends AbstractTestStepActor<FlowStep> {

	public static final String NAME = "flow-p";

    public static final String THREAD_OPENING_TAG = "[";
    public static final String THREAD_CLOSING_TAG = "]";

	private AtomicInteger finishedChildren;
	private Map<Integer, ActorRef> stepIndexActorMap;

	private boolean childrenHasError;

	public FlowStepProcessorActor(FlowStep step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
	}

	@Override
	protected void init() throws Exception {
		stepIndexActorMap = new ConcurrentHashMap<>();

		int childCount = step.getThread().size();
		if(childCount > 0) {
			finishedChildren = new AtomicInteger(childCount);
			for(int i=0; i<step.getThread().size(); i++) {
				Sequence sequence = step.getThread().get(i);

				//ActorRef child = SequenceProcessorActor.create(getContext(), sequence, scope, stepId + STEP_SEPARATOR + (i + 1));
                ActorRef child = SequenceProcessorActor.create(getContext(), sequence, scope, stepId + THREAD_OPENING_TAG + (i + 1) + THREAD_CLOSING_TAG);

				stepIndexActorMap.put(i, child);
			}
		}

		childrenHasError = false;
	}

	@Override
	protected void start() throws Exception {
		for(ActorRef child : stepIndexActorMap.values()) {
				child.tell(new StartCommand(scope.getContext().getSessionId()), self());
		}
		processing();
	}

	@Override
	protected void stop() {
		StopCommand command = new StopCommand(scope.getContext().getSessionId());
		for(ActorRef child : getContext().getChildren()) {
			child.tell(command, self());
		}
		stepIndexActorMap.clear();
	}

	@Override
	protected void handleStatusEvent(StatusEvent event) throws Exception {
//		super.handleStatusEvent(event);

        StepStatus status = event.getStatus();

		switch (status) {
			case ERROR:
				childrenHasError = true; // intentional fall through - no break statement
			case COMPLETED:
				int remainingChildren = finishedChildren.decrementAndGet();

				if(remainingChildren == 0) {
					if(childrenHasError) {
						childrenHasError();
					} else {
						completed();
					}
				}
		}
	}

	public static ActorRef create(ActorContext context, FlowStep step, TestCaseScope scope, String stepId) throws Exception {
		return create(FlowStepProcessorActor.class, context, step, scope, stepId);
	}
}
