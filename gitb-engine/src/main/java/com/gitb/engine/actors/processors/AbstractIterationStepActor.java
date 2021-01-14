package com.gitb.engine.actors.processors;

import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.TestEngineConfiguration;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.ForEachStep;
import com.gitb.tdl.RepeatUntilStep;
import com.gitb.tdl.TestConstruct;
import com.gitb.tdl.WhileStep;
import com.gitb.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serbay on 9/15/14.
 *
 * Checks if the iteration limit is reached for the while/foreach/repeatuntil steps
 *
 */
public abstract class AbstractIterationStepActor<T> extends AbstractTestStepActor<T> {
    private static Logger logger = LoggerFactory.getLogger(AbstractIterationStepActor.class);
    public static final String ITERATION_OPENING_TAG = "[";
	public static final String ITERATION_CLOSING_TAG = "]";

	private boolean childrenHasError = false;
	private boolean childrenHasWarning = false;

	public AbstractIterationStepActor(T step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
		initialize();
	}

	public AbstractIterationStepActor(T step, TestCaseScope scope) {
		super(step, scope);
		initialize();
	}

	private void initialize() {
		boolean iterableStep = false;
		if(step instanceof WhileStep
			|| step instanceof ForEachStep
			|| step instanceof RepeatUntilStep) {
			iterableStep = true;
		}

		if(!iterableStep) {
			throw new GITBEngineInternalError("Wrong step supplied for Iteration Processor!");
		}
	}

	protected void checkIteration(int iteration) {
		if(iteration > TestEngineConfiguration.ITERATION_LIMIT) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Maximum iteration limit is "+TestEngineConfiguration.ITERATION_LIMIT+", iteration exceeds this limit!"));
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
			boolean shouldContinue = true;
			if (scope.getContext().getCurrentState() == TestCaseContext.TestCaseStateEnum.STOPPING
					|| (status == StepStatus.ERROR && ((step instanceof TestConstruct && ((TestConstruct) step).isStopOnError())))
			) {
				shouldContinue = false;
			}
			if (shouldContinue) {
				shouldContinue = handleStatusEventInternal(event);
			}
			if (!shouldContinue) {
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

	protected abstract boolean handleStatusEventInternal(StatusEvent event) throws Exception;

}
