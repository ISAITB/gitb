package com.gitb.engine.actors.processors;

import com.gitb.core.ErrorCode;
import com.gitb.engine.TestEngineConfiguration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.ForEachStep;
import com.gitb.tdl.RepeatUntilStep;
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
}
