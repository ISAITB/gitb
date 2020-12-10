package com.gitb.engine.actors.processors;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.events.model.ErrorStatusEvent;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.processing.ProcessingContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.processing.IProcessingHandler;
import com.gitb.processing.ProcessingReport;
import com.gitb.tdl.Process;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.utils.ErrorUtils;
import scala.concurrent.Future;
import scala.concurrent.Promise;

public class ProcessStepProcessorActor extends AbstractProcessingStepProcessorActor<Process> {

    public static final String NAME = "process-p";

    private Promise<TestStepReportType> promise;

    public ProcessStepProcessorActor(Process step, TestCaseScope scope, String stepId) {
        super(step, scope, stepId);
    }

    public static ActorRef create(ActorContext context, Process step, TestCaseScope scope, String stepId) throws Exception {
        return create(ProcessStepProcessorActor.class, context, step, scope, stepId);
    }

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
        }, context.dispatcher());

        promise.future().failed().foreach(new OnFailure() {
            @Override
            public void onFailure(Throwable failure) {
                handleFutureFailure(failure);
            }
        }, context.dispatcher());
    }

    @Override
    protected void start() {
        processing();

        ProcessingContext context;
        if (step.getTxnId() == null) {
            // No processing transaction - this is a standalone process call.
            if (step.getHandler() == null) {
                throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Test step [" + stepId + "] is a process step with no transaction reference and no handler definition."));
            }
            String handlerIdentifier = step.getHandler();
            VariableResolver resolver = new VariableResolver(scope);
            if (resolver.isVariableReference(handlerIdentifier)) {
                handlerIdentifier = resolver.resolveVariableAsString(handlerIdentifier).toString();
            }
            context = new ProcessingContext(handlerIdentifier, null);
        } else {
            // A processing transaction is referenced.
            context = this.scope.getContext().getProcessingContext(step.getTxnId());
        }

        Future<TestStepReportType> future = Futures.future(() -> {
            IProcessingHandler handler = context.getHandler();
            String operation = step.getOperation();
            ProcessingReport report = handler.process(context.getSession(), operation, getData(handler, operation));
            if (step.getId() != null) {
                scope.createVariable(step.getId()).setValue(getValue(report.getData()));
            }
            return report.getReport();
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

    @Override
    protected void stop() {
        if (promise != null) {
            promise.tryFailure(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.CANCELLATION, "Test step [" + stepId + "] is cancelled.")));
        }
    }

}
