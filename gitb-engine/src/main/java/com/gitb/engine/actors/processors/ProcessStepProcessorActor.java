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

import com.gitb.core.AnyContent;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.processing.ProcessingContext;
import com.gitb.engine.processing.handlers.XPathProcessor;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.HandlerUtils;
import com.gitb.engine.utils.StepContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.processing.DeferredProcessingReport;
import com.gitb.processing.IProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.tdl.ErrorLevel;
import com.gitb.tdl.Process;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.types.MapType;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.dispatch.OnFailure;
import org.apache.pekko.dispatch.OnSuccess;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ProcessStepProcessorActor extends AbstractProcessingStepProcessorActor<Process> {

    public static final String NAME = "process-p";

    private Promise<TestStepReportType> promise;

    public ProcessStepProcessorActor(Process step, TestCaseScope scope, String stepId, StepContext stepContext) {
        super(step, scope, stepId, stepContext);
    }

    public static ActorRef create(ActorContext context, Process step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
        return create(ProcessStepProcessorActor.class, context, step, scope, stepId, stepContext);
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
            VariableResolver resolver = new VariableResolver(scope);
            String handlerIdentifier = resolveProcessingHandler(step.getHandler(), () -> resolver);
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
            ProcessingData input = getData(handler, operation);
            if (handler instanceof XPathProcessor) {
                input.addInput(HandlerUtils.NAMESPACE_MAP_INPUT, MapType.fromMap(scope.getNamespaceDefinitions()));
            }
            ProcessingReport report = handler.process(context.getSession(), step.getId(), operation, input);
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
        Optional<VariableResolver> resolver = Optional.empty();
        if (report.getData() != null && (step.getId() != null || step.getOutput() != null)) {
            if (step.getOutput() != null && report.getData().getData() != null) {
                int outputCount = report.getData().getData().size();
                if (outputCount == 1) {
                    // Single output - set as direct result.
                    scope.createVariable(step.getOutput()).setValue(report.getData().getData().values().iterator().next());
                } else if (outputCount > 1) {
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
            boolean isHidden = TestCaseUtils.resolveBooleanFlag(step.getHidden(), true, () -> new VariableResolver(scope));
            if (!isHidden) {
                // We only add to the report's context the created data if this is visible and
                // if the handler is not a custom one (for custom ones you can return anything
                // you like).
                completeProcessingReportContext(report);
            }
        }
        ErrorLevel errorLevel = TestCaseUtils.resolveReportErrorLevel(step.getLevel(), scope.getContext().getSessionId(), resolver.orElse(new VariableResolver(scope)));
        TestCaseUtils.postProcessReport(step.isInvert(), errorLevel, report.getReport());
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
