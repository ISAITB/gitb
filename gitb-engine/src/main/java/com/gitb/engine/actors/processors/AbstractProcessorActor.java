/*
 * Copyright (C) 2026 European Union
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
import com.gitb.engine.processors.IProcessor;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.utils.ErrorUtils;
import org.apache.pekko.dispatch.Futures;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.runtime.BoxedUnit;

/**
 * Created by serbay on 9/5/14.
 * <p>
 * Common methods for the steps using the {@link com.gitb.engine.processors.IProcessor} interface
 *
 */
public abstract class AbstractProcessorActor<T> extends AbstractTestStepActor<T> {

	private Promise<TestStepReportType> promise;

	public AbstractProcessorActor(T step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
	}

	protected abstract IProcessor getProcessor();

	@Override
	protected void init() {
		final ActorContext context = getContext();

		promise = Futures.promise();

		promise.future().onComplete(result -> {
			if (result.isSuccess()) {
				TestStepReportType report = result.get();
				if (report != null) {
					if (report.getResult() == TestResultType.SUCCESS) {
						updateTestStepStatus(context, StepStatus.COMPLETED, report);
					} else if (report.getResult() == TestResultType.WARNING) {
						updateTestStepStatus(context, StepStatus.WARNING, report);
					} else {
						updateTestStepStatus(context, StepStatus.ERROR, report);
					}
				} else {
					updateTestStepStatus(context, StepStatus.COMPLETED, null);
				}
			} else {
				handleFutureFailure(result.failed().get());
			}
			return BoxedUnit.UNIT;
		}, getContext().dispatcher());
	}

	@Override
	protected void start() {
		final IProcessor processor = getProcessor();

		if(processor != null) {
			Future<TestStepReportType> future = Futures.future(() -> {
				processing();

				return processor.process(step);
			}, stepDispatcher());

		future.onComplete(result -> {
			if (result.isSuccess()) {
				promise.trySuccess(result.get());
			} else {
				promise.tryFailure(result.failed().get());
			}
			return BoxedUnit.UNIT;
		}, getContext().dispatcher());
		}
	}

	protected ExecutionContext stepDispatcher() {
		return getContext().getDispatcher();
	}

	@Override
	protected void stop() {
		super.stop();
        if(promise != null) {
            promise.tryFailure(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.CANCELLATION, "Test step ["+stepId+"] is cancelled.")));
        }
	}
}
