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
import com.gitb.engine.processors.IProcessor;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.utils.ErrorUtils;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.dispatch.OnFailure;
import org.apache.pekko.dispatch.OnSuccess;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * Created by serbay on 9/5/14.
 *
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

		promise.future().foreach(new OnSuccess<>() {
			@Override
			public void onSuccess(TestStepReportType result) {
				if (result != null) {
					if (result.getResult() == TestResultType.SUCCESS) {
						updateTestStepStatus(context, StepStatus.COMPLETED, result);
					} else if (result.getResult() == TestResultType.WARNING) {
						updateTestStepStatus(context, StepStatus.WARNING, result);
					} else {
						updateTestStepStatus(context, StepStatus.ERROR, result);
					}
				} else {
					updateTestStepStatus(context, StepStatus.COMPLETED, null);
				}
			}
		}, getContext().dispatcher());

		promise.future().failed().foreach(new OnFailure() {
			@Override
			public void onFailure(Throwable failure) {
				handleFutureFailure(failure);
			}
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

			future.foreach(new OnSuccess<>() {

				@Override
				public void onSuccess(TestStepReportType result) {
					promise.trySuccess(result);
				}
			}, getContext().dispatcher());

			future.failed().foreach(new OnFailure() {
				@Override
				public void onFailure(Throwable failure) {
					promise.tryFailure(failure);
				}
			}, getContext().dispatcher());
		}
	}

	protected ExecutionContext stepDispatcher() {
		return getContext().getDispatcher();
	}

	@Override
	protected void stop() {
        if(promise != null) {
            promise.tryFailure(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.CANCELLATION, "Test step ["+stepId+"] is cancelled.")));
        }
	}
}
