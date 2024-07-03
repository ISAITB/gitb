package com.gitb.engine.actors.processors;

import com.gitb.engine.actors.ActorSystem;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.dispatch.OnFailure;
import org.apache.pekko.dispatch.OnSuccess;
import com.gitb.core.AnyContent;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.processing.ProcessingContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.processing.DeferredProcessingReport;
import com.gitb.processing.IProcessingHandler;
import com.gitb.processing.ProcessingReport;
import com.gitb.tdl.Process;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.Map;
import java.util.concurrent.TimeUnit;

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
            if (VariableResolver.isVariableReference(handlerIdentifier)) {
                handlerIdentifier = resolver.resolveVariableAsString(handlerIdentifier).toString();
            }
            context = new ProcessingContext(handlerIdentifier, TestCaseUtils.getStepProperties(step.getProperty(), resolver), scope.getContext().getSessionId());
        } else {
            // A processing transaction is referenced.
            context = this.scope.getContext().getProcessingContext(step.getTxnId());
        }

        Future<Future<TestStepReportType>> future = Futures.future(() -> {
            IProcessingHandler handler = context.getHandler();
            String operation = null;
            if (step.getOperation() != null) {
                operation = step.getOperation();
            } else if (step.getOperationAttribute() != null) {
                operation = step.getOperationAttribute();
            }
            ProcessingReport report = handler.process(context.getSession(), step.getId(), operation, getData(handler, operation));
            Promise<TestStepReportType> taskPromise = Futures.promise();
            if (report instanceof DeferredProcessingReport deferredReport) {
                getContext().getSystem().getScheduler().scheduleOnce(
                        scala.concurrent.duration.Duration.apply(deferredReport.getDelayToApply(), TimeUnit.MILLISECONDS),
                        () -> taskPromise.success(produceReport(report, handler)),
                        getContext().dispatcher()
                );
            } else {
                taskPromise.success(produceReport(report, handler));
            }
            return taskPromise.future();
        }, getContext().getSystem().dispatchers().lookup(ActorSystem.BLOCKING_IO_DISPATCHER));

        future.foreach(new OnSuccess<>() {
            @Override
            public void onSuccess(Future<TestStepReportType> reportFuture) {
                reportFuture.foreach(new OnSuccess<>() {
                    @Override
                    public void onSuccess(TestStepReportType report) {
                        promise.trySuccess(report);
                    }
                }, getContext().dispatcher());
                reportFuture.failed().foreach(new OnFailure() {
                    @Override
                    public void onFailure(Throwable failure) {
                        promise.tryFailure(failure);
                    }
                }, getContext().dispatcher());
            }
        }, getContext().dispatcher());

        future.failed().foreach(new OnFailure() {
            @Override
            public void onFailure(Throwable failure) {
                promise.tryFailure(failure);
            }
        }, getContext().dispatcher());
    }

    private TAR produceReport(ProcessingReport report, IProcessingHandler handler) {
        if (report.getData() != null && (step.getId() != null || step.getOutput() != null)) {
            if (step.getOutput() != null) {
                if (report.getData().getData() != null && report.getData().getData().size() == 1) {
                    // Single output - set as direct result.
                    scope.createVariable(step.getOutput()).setValue(report.getData().getData().values().iterator().next());
                } else {
                    // Multiple outputs - set as map result.
                    scope.createVariable(step.getOutput()).setValue(getValue(report.getData()));
                }
            }
            if (step.getId() != null) {
                // Backwards compatibility - set output map linked to id.
                scope.createVariable(step.getId()).setValue(getValue(report.getData()));
            }
        }
        if (step.getHidden() != null && !handler.isRemote()) {
            var isHidden = true;
            if (VariableResolver.isVariableReference(step.getHidden())) {
                var hiddenVariable = new VariableResolver(scope).resolveVariable(step.getHidden());
                isHidden = hiddenVariable != null && Boolean.TRUE.equals(hiddenVariable.convertTo(DataType.BOOLEAN_DATA_TYPE).getValue());
            } else {
                isHidden = Boolean.parseBoolean(step.getHidden());
            }
            if (!isHidden) {
                // We only add to the report's context the created data if this is visible and
                // if the handler is not a custom one (for custom ones you can return anything
                // you like).
                completeProcessingReportContext(report);
            }
        }
        return report.getReport();
    }

    @Override
    protected void stop() {
        if (promise != null) {
            promise.tryFailure(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.CANCELLATION, "Test step [" + stepId + "] is cancelled.")));
        }
    }

    private void completeProcessingReportContext(ProcessingReport report) {
        if (report != null && report.getReport() != null && report.getData() != null && report.getData().getData() != null) {
            if (report.getReport().getContext() == null) {
                report.getReport().setContext(new AnyContent());
            }
            for (Map.Entry<String, DataType> dataEntry: report.getData().getData().entrySet()) {
                report.getReport().getContext().getItem().add(DataTypeUtils.convertDataTypeToAnyContent(dataEntry.getKey(), dataEntry.getValue()));
            }
        }
    }
}
