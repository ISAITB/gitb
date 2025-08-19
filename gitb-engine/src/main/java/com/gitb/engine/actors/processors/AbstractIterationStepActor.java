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

import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.TestEngineConfiguration;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.ForEachStep;
import com.gitb.tdl.RepeatUntilStep;
import com.gitb.tdl.TestConstruct;
import com.gitb.tdl.WhileStep;
import com.gitb.utils.ErrorUtils;

/**
 * Created by serbay on 9/15/14.
 * <p>
 * Checks if the iteration limit is reached for the while/foreach/repeatuntil steps
 *
 */
public abstract class AbstractIterationStepActor<T> extends AbstractTestStepActor<T> {

    public static final String ITERATION_OPENING_TAG = "[";
	public static final String ITERATION_CLOSING_TAG = "]";

	private boolean childrenHasError = false;
	private boolean childrenHasWarning = false;

	public AbstractIterationStepActor(T step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
		initialize();
	}

	private void initialize() {
		boolean iterableStep = step instanceof WhileStep
                || step instanceof ForEachStep
                || step instanceof RepeatUntilStep;
        if (!iterableStep) {
			throw new GITBEngineInternalError("Wrong step supplied for Iteration Processor!");
		}
	}

	protected void checkIteration(int iteration) {
		if (iteration > TestEngineConfiguration.ITERATION_LIMIT) {
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
		if (status == StepStatus.ERROR || status == StepStatus.WARNING || status == StepStatus.COMPLETED || status == StepStatus.SKIPPED) {
			// Final state for the step.
			boolean stopping = scope.getContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPING && scope.getContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.STOPPED;
			boolean shouldContinue = !stopping && status != StepStatus.ERROR || !(step instanceof TestConstruct construct) || !Boolean.TRUE.equals(construct.isStopOnError());
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
