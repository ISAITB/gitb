package com.gitb.engine.actors.processors;

import com.gitb.core.StepStatus;
import com.gitb.engine.commands.interaction.StartCommand;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.tdl.FlowStep;
import com.gitb.tdl.Sequence;
import org.apache.pekko.actor.ActorRef;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gitb.core.StepStatus.*;

/**
 * Flow step executor actor
 */
public class FlowStepProcessorActor extends AbstractTestStepActor<FlowStep> {

	public static final String NAME = "flow-p";

    public static final String THREAD_OPENING_TAG = "[";
    public static final String THREAD_CLOSING_TAG = "]";

	private Map<Integer, ActorRef> childMap;

	private boolean childrenHasError;
	private boolean childrenHasWarning;

	public FlowStepProcessorActor(FlowStep step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
	}

	@Override
	protected void init() throws Exception {
		childMap = new ConcurrentHashMap<>();

		if (!step.getThread().isEmpty()) {
			for (int i=0; i< step.getThread().size(); i++) {
				Sequence sequence = step.getThread().get(i);
                ActorRef child = SequenceProcessorActor.create(getContext(), sequence, scope, stepId + THREAD_OPENING_TAG + (i + 1) + THREAD_CLOSING_TAG, stepContext);
				childMap.put(child.path().uid(), child);
			}
		}

		childrenHasError = false;
	}

	@Override
	protected void start() throws Exception {
		for (ActorRef child : childMap.values()) {
			child.tell(new StartCommand(scope.getContext().getSessionId()), self());
		}
		processing();
	}

	@Override
	protected void handleStatusEvent(StatusEvent event) {
        StepStatus status = event.getStatus();
        if (status == ERROR) {
			childrenHasError = true;
		} else if (status == WARNING) {
			childrenHasWarning = true;
		}
        if (status == ERROR || status == WARNING || status == COMPLETED) {
			childMap.remove(event.getSender().path().uid());
			if (childMap.isEmpty()) {
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

	public static ActorRef create(ActorContext context, FlowStep step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		return create(FlowStepProcessorActor.class, context, step, scope, stepId, stepContext);
	}
}
