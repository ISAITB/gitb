package com.gitb.engine.actors.processors;

import akka.dispatch.Futures;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.events.model.ErrorStatusEvent;
import com.gitb.engine.processors.IProcessor;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.utils.ErrorUtils;
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

	public AbstractProcessorActor(T step, TestCaseScope scope) {
		super(step, scope);
	}

	public AbstractProcessorActor(T step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
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
				updateTestStepStatus(context, new ErrorStatusEvent(failure), null, true);
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
			}, getContext().dispatcher());

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

	@Override
	protected void stop() {
        if(promise != null) {
            promise.tryFailure(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.CANCELLATION, "Test step ["+stepId+"] is cancelled.")));
        }
	}
}
