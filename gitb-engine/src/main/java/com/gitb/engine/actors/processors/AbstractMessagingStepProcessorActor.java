package com.gitb.engine.actors.processors;

import com.gitb.tr.TestResultType;
import org.apache.pekko.dispatch.OnFailure;
import org.apache.pekko.dispatch.OnSuccess;
import com.gitb.core.*;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.TransactionContext;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.tdl.Binding;
import com.gitb.tdl.MessagingStep;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ConfigurationUtils;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import scala.concurrent.Promise;

import java.util.*;

/**
 * Created by serbay.
 *
 * Base class for providing utility methods for messaging step processor
 * actors.
 */
public abstract class AbstractMessagingStepProcessorActor<T extends MessagingStep> extends AbstractTestStepActor<T> {

    private final ExpressionHandler expressionHandler;

    public AbstractMessagingStepProcessorActor(T step, TestCaseScope scope, String stepId) {
        super(step, scope, stepId);

        expressionHandler = new ExpressionHandler(scope);
    }

    protected abstract MessagingContext getMessagingContext();

    protected MapType generateOutputWithModuleDefinition(MessagingContext messagingContext, Message message) {
        final IMessagingHandler messagingHandler = messagingContext.getHandler();
        MapType map = new MapType();
        var definition = messagingHandler.getModuleDefinition();
        if (definition != null) {
            List<TypedParameter> expectedOutputs = definition.getOutputs().getParam();
            for (TypedParameter outputParam : expectedOutputs) {
                DataType data = message.getFragments().get(outputParam.getName());
                map.addItem(outputParam.getName(), data);
            }
        }
        return map;
    }

    protected void checkRequiredConfigsAndSetDefaultValues(List<Parameter> expectedConfigs, List<Configuration> providedConfigs) {
        List<Parameter> requiredParameters = new ArrayList<>();

        for(Parameter parameter : expectedConfigs) {
            if(parameter.getUse() == UsageEnumeration.R) {
                requiredParameters.add(parameter);
            }
        }

        for(Parameter requiredParameter : requiredParameters) {
            boolean found = false;
            for(Configuration configuration : providedConfigs) {
                if(configuration.getName().equals(requiredParameter.getName())) {
                    found = true;
                    break;
                }
            }

            if(!found) {
                if (requiredParameter.getValue() == null) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Missing required configuration parameter [" + requiredParameter.getName() + "]."));
                } else {
                    providedConfigs
                            .add(ConfigurationUtils.constructConfiguration(requiredParameter.getName(), requiredParameter.getValue()));
                }
            }
        }
    }

    protected MapType generateOutputWithNameBinding(Message message, List<Binding> output) {
        MapType map = new MapType();
        for (Binding binding : output) {
            DataType data = message.getFragments().get(binding.getName());
            if (binding.getValue() != null) {
                map.addItem(binding.getValue().substring(1), data);
            }
        }
        return map;
    }

    protected MapType generateOutputWithMessageFields(Message message) {
        MapType map = new MapType();
        for(Map.Entry<String, DataType> entry : message.getFragments().entrySet()) {
            map.addItem(entry.getKey(), entry.getValue());
        }

        return map;
    }

    private void setInputWithModuleDefinition(Message message, List<Binding> input, List<TypedParameter> expectedParameters) {
        Iterator<TypedParameter> expectedParamsIterator = expectedParameters.iterator();
        Iterator<Binding> inputsIterator = input.iterator();
        while (expectedParamsIterator.hasNext() && inputsIterator.hasNext()) {
            TypedParameter expectedParam = expectedParamsIterator.next();
            Binding inputExpression = inputsIterator.next();
            DataType result = expressionHandler.processExpression(inputExpression, expectedParam.getType());
            message.addInput(expectedParam.getName(), result);
        }
        checkRequiredParameters(message, expectedParameters);
    }

    private void setInputWithNameBinding(Message message, List<Binding> input, List<TypedParameter> expectedParameters) {
        for (Binding binding : input) {
            TypedParameter parameter = getTypedParameterByName(expectedParameters, binding.getName());
            DataType data;
            if (parameter == null) {
                data = expressionHandler.processExpression(binding);
            } else {
                data = expressionHandler.processExpression(binding, parameter.getType());
            }
            message.addInput(binding.getName(), data);
        }
        checkRequiredParameters(message, expectedParameters);
    }

    private TypedParameter getTypedParameterByName(List<TypedParameter> parameters, String name) {
        for(TypedParameter parameter : parameters) {
            if(parameter.getName().equals(name)) {
                return parameter;
            }
        }

        return null;
    }

    private void checkRequiredParameters(Message message, List<TypedParameter> expectedParameters) {
        for (TypedParameter expectedParameter : expectedParameters) {
            if (expectedParameter.getUse() == UsageEnumeration.R && !message.hasInput(expectedParameter.getName())) {
                if (expectedParameter.getValue() == null) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Missing input parameter ["+expectedParameter.getName()+"]"));
                } else { // set the default value
                    DataType defaultValue = DataTypeFactory.getInstance().create(expectedParameter.getValue().getBytes(), expectedParameter.getType());
                    message.getFragments().put(expectedParameter.getName(), defaultValue);
                }
            }
        }
    }

    private List<TypedParameter> getExpectedInputs(IMessagingHandler messagingHandler) {
        List<TypedParameter> expectedInputs = new ArrayList<>();
        var definition = messagingHandler.getModuleDefinition();
        if (definition != null && definition.getInputs() != null) {
            expectedInputs.addAll(definition.getInputs().getParam());
        }
        return expectedInputs;
    }

    protected Message getMessageFromBindings(List<Binding> bindings) {
        final IMessagingHandler messagingHandler = getMessagingContext().getHandler();
        Message message = new Message();
        boolean isNameBinding = BindingUtils.isNameBinding(bindings);
        if (isNameBinding) {
            setInputWithNameBinding(message, bindings, getExpectedInputs(messagingHandler));
        } else {
            setInputWithModuleDefinition(message, bindings, getExpectedInputs(messagingHandler));
        }
        return message;
    }

    protected Pair<MessagingContext, TransactionContext> determineMessagingContexts(VariableResolver resolver) {
        var testCaseContext = scope.getContext();
        // Find the applicable messaging context.
        Optional<MessagingContext> messagingContext;
        if (StringUtils.isNotBlank(step.getTxnId())) {
            // Find by transaction ID
            messagingContext = testCaseContext.getMessagingContexts().stream().filter(ctx -> ctx.getTransaction(step.getTxnId()) != null).findFirst();
        } else if (StringUtils.isNotBlank(step.getHandler())) {
            // Find by handler.
            var handler = VariableResolver.isVariableReference(step.getHandler())?resolver.resolveVariableAsString(step.getHandler()).toString():step.getHandler();
            messagingContext = testCaseContext.getMessagingContexts().stream().filter(ctx -> handler.equals(ctx.getHandlerIdentifier())).findFirst();
        } else {
            throw new IllegalStateException("Messaging step missing both a transaction ID and handler identifier");
        }
        if (messagingContext.isPresent()) {
            // Find the applicable transaction context.
            TransactionContext transactionContext;
            if (StringUtils.isNotBlank(step.getTxnId())) {
                transactionContext = messagingContext.get().getTransaction(step.getTxnId());
                if (transactionContext == null) {
                    throw new IllegalStateException("Transaction not found for ID ["+step.getTxnId()+"]");
                }
            } else {
                // Check if an implicit transaction has already been created.
                transactionContext = messagingContext.get().getTransaction(messagingContext.get().getHandlerIdentifier());
                if (transactionContext == null) {
                    var txIdToUse = messagingContext.get().getHandlerIdentifier();
                    if (messagingContext.get().getHandler() instanceof AbstractMessagingHandler) {
                        // We need to signal a start
                        messagingContext.get().getHandler().beginTransaction(
                                messagingContext.get().getSessionId(),
                                txIdToUse,
                                step.getId(),
                                step.getFrom(),
                                step.getTo(),
                                step.getConfig()
                        );
                    }
                    // Create an implicit transaction context.
                    transactionContext = new TransactionContext(txIdToUse);
                    messagingContext.get().setTransaction(txIdToUse, transactionContext);
                }
            }
            return Pair.of(messagingContext.get(), transactionContext);
        } else {
            throw new IllegalStateException("Unable to determine the messaging context for a messaging step");
        }
    }

    protected OnSuccess<TestStepReportType> handleSuccess(Promise<TestStepReportType> promise, IMessagingHandler handler, TransactionContext transaction) {
        return new OnSuccess<>() {
            @Override
            public void onSuccess(TestStepReportType result) {
                promise.trySuccess(result);
            }
        };
    }

    protected OnFailure handleFailure(Promise<TestStepReportType> promise, IMessagingHandler handler, TransactionContext transaction) {
        return new OnFailure() {
            @Override
            public void onFailure(Throwable failure) {
                promise.tryFailure(failure);
            }
        };
    }

    protected void signalStepStatus(TestStepReportType result) {
        if (result != null) {
            if (result.getResult() == TestResultType.SUCCESS) {
                updateTestStepStatus(getContext(), StepStatus.COMPLETED, result);
            } else if (result.getResult() == TestResultType.WARNING) {
                updateTestStepStatus(getContext(), StepStatus.WARNING, result);
            } else {
                updateTestStepStatus(getContext(), StepStatus.ERROR, result);
            }
        }
    }
}
