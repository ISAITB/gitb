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

import com.gitb.core.Configuration;
import com.gitb.core.ErrorCode;
import com.gitb.core.MessagingModule;
import com.gitb.core.StepStatus;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.TransactionContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
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
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.dispatch.OnFailure;
import org.apache.pekko.dispatch.OnSuccess;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.Objects;

import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.getMessageFromBindings;

/**
 * Created by serbay.
 * <p>
 * Listen step executor actor
 */
public class ListenStepProcessorActor extends AbstractMessagingStepProcessorActor<Listen> {

    public static final String NAME = "listen-p";

    private MessagingContext messagingContext;
    private TransactionContext transactionContext;

    private Promise<TestStepReportType> promise;

    public ListenStepProcessorActor(Listen step, TestCaseScope scope, String stepId, StepContext stepContext) {
        super(step, scope, stepId, stepContext);
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
        VariableResolver resolver = new VariableResolver(scope);

        var contexts = determineMessagingContexts(resolver);
        messagingContext = contexts.getLeft();
        transactionContext = contexts.getRight();

        final IMessagingHandler messagingHandler = messagingContext.getHandler();
        final ActorContext context = getContext();

        waiting();

        if(messagingHandler != null) {
            Future<TestStepReportType> future = Futures.future(() -> {
                if (step.getConfig() != null) {
                    for (Configuration config : step.getConfig()) {
                        if (VariableResolver.isVariableReference(config.getValue())) {
                            config.setValue(resolver.resolveVariableAsString(config.getValue()).toString());
                        }
                    }
                }
                MessagingModule moduleDefinition = messagingHandler.getModuleDefinition();
                if (moduleDefinition != null && moduleDefinition.getReceiveConfigs() != null) {
                    checkRequiredConfigsAndSetDefaultValues(moduleDefinition.getReceiveConfigs().getParam(), step.getConfig());
                }

                Message inputMessage = getMessageFromBindings(messagingHandler, step.getInput(), expressionHandler);
                var from = VariableResolver.isVariableReference(getFrom())? resolver.resolveVariableAsString(getFrom()).getValue() :getFrom();
                var to = VariableResolver.isVariableReference(getTo())? resolver.resolveVariableAsString(getTo()).getValue() :getTo();
                MessagingReport report =
                        messagingHandler
                                .listenMessage(
                                        messagingContext.getSessionId(),
                                        transactionContext.getTransactionId(),
                                        step.getId(),
                                        from,
                                        to,
                                        step.getConfig(),
                                        inputMessage
                                );

                if (report != null && report.getMessage() != null) {
                    Message message = report.getMessage();

                    if (step.getId() != null) {
                        MapType map;

                        if (step.getOutput().isEmpty()) {
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

            future.foreach(handleSuccess(promise), getContext().dispatcher());
            future.failed().foreach(handleFailure(promise), getContext().dispatcher());
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

    public static ActorRef create(ActorContext context, Listen step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
        return context.actorOf(props(ListenStepProcessorActor.class, step, scope, stepId, stepContext).withDispatcher(ActorSystem.BLOCKING_DISPATCHER), getName(NAME));
    }

    @Override
    protected String getFrom() {
        return Objects.requireNonNullElseGet(super.getFrom(), () -> scope.getContext().getDefaultSutActor());
    }

    @Override
    protected String getTo() {
        return Objects.requireNonNullElseGet(super.getTo(), () -> scope.getContext().getDefaultNonSutActor());
    }

}
