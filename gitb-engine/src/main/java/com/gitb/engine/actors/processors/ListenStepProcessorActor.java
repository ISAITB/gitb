package com.gitb.engine.actors.processors;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import com.gitb.core.Configuration;
import com.gitb.core.ErrorCode;
import com.gitb.core.MessagingModule;
import com.gitb.core.StepStatus;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.events.model.ErrorStatusEvent;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.TransactionContext;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.tdl.Listen;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.MapType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * Created by serbay.
 *
 * Listen step executor actor
 */
public class ListenStepProcessorActor extends AbstractMessagingStepProcessorActor<Listen> {

    public static final String NAME = "listen-p";

    private MessagingContext messagingContext;
    private TransactionContext transactionContext;

    private Promise<TestStepReportType> promise;

    public ListenStepProcessorActor(Listen step, TestCaseScope scope, String stepId) {
        super(step, scope, stepId);
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
                updateTestStepStatus(context, new ErrorStatusEvent(failure), null, true);
            }
        }, context.dispatcher());
    }

    @Override
    protected void start() {
        processing();

        TestCaseContext testCaseContext = scope.getContext();

        for(MessagingContext mc : testCaseContext.getMessagingContexts()) {
            if(mc.getTransaction(step.getTxnId()) != null) {
                messagingContext = mc;
                break;
            }
        }

        transactionContext = messagingContext.getTransaction(step.getTxnId());

        final IMessagingHandler messagingHandler = messagingContext.getHandler();
        final ActorContext context = getContext();

        waiting();

        if(messagingHandler != null) {
            Future<TestStepReportType> future = Futures.future(() -> {

                VariableResolver resolver = new VariableResolver(scope);
                if (step.getConfig() != null) {
                    for (Configuration config : step.getConfig()) {
                        if (resolver.isVariableReference(config.getValue())) {
                            config.setValue(resolver.resolveVariableAsString(config.getValue()).toString());
                        }
                    }
                }
                MessagingModule moduleDefinition = messagingHandler.getModuleDefinition();
                if (moduleDefinition.getReceiveConfigs() != null) {
                    checkRequiredConfigsAndSetDefaultValues(moduleDefinition.getReceiveConfigs().getParam(), step.getConfig());
                }

                Message inputMessage = getMessageFromBindings(step.getInput());

                MessagingReport report =
                        messagingHandler
                                .listenMessage(
                                        messagingContext.getSessionId(),
                                        transactionContext.getTransactionId(),
                                        step.getFrom(),
                                        step.getTo(),
                                        step.getConfig(),
                                        inputMessage
                                );

                if (report != null && report.getMessage() != null) {
                    Message message = report.getMessage();

                    if (step.getId() != null) {
                        MapType map;

                        if (step.getOutput().size() == 0) {
                            map = generateOutputWithMessageFields(message);
                        } else {
                            boolean isNameBinding = BindingUtils.isNameBinding(step.getOutput());
                            if (isNameBinding) {
                                map = generateOutputWithNameBinding(message, step.getOutput());
                            } else {
                                map = generateOutputWithModuleDefinition(messagingContext, message);
                            }
                        }

                        scope
                                .createVariable(step.getId())
                                .setValue(map);
                    }
                    return report.getReport();
                } else if (report != null) {
                    return report.getReport();
                } else {
                    return null;
                }

            }, context.dispatcher());

            future.foreach(new OnSuccess<>() {
                @Override
                public void onSuccess(TestStepReportType result) {
                    promise.trySuccess(result);
                }
            }, context.dispatcher());

            future.failed().foreach(new OnFailure() {
                @Override
                public void onFailure(Throwable failure) {
                    messagingHandler.endTransaction(messagingContext.getSessionId(), transactionContext.getTransactionId());
                    promise.tryFailure(failure);
                }
            }, context.dispatcher());
        } else {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Messaging handler is not available"));
        }
    }

    @Override
    protected void stop() {
        if(promise != null && !promise.isCompleted()) {
            promise.tryFailure(new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.CANCELLATION, "Test step [" + stepId + "] is cancelled.")));
        }
    }

    @Override
    protected MessagingContext getMessagingContext() {
        return messagingContext;
    }

    @Override
    protected TransactionContext getTransactionContext() {
        return transactionContext;
    }

    public static ActorRef create(ActorContext context, Listen step, TestCaseScope scope, String stepId) throws Exception {
        return context.actorOf(props(ListenStepProcessorActor.class, step, scope, stepId).withDispatcher(ActorSystem.BLOCKING_DISPATCHER), getName(NAME));
    }
}
