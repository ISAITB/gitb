package com.gitb.engine.actors.processors;

import com.gitb.core.*;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.TransactionContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.tdl.Binding;
import com.gitb.tdl.Expression;
import com.gitb.tdl.MessagingStep;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ConfigurationUtils;
import com.gitb.utils.ErrorUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by serbay.
 *
 * Base class for providing utility methods for messaging step processor
 * actors.
 */
public abstract class AbstractMessagingStepProcessorActor<T extends MessagingStep> extends AbstractTestStepActor<T> {

    private ExpressionHandler expressionHandler;

    public AbstractMessagingStepProcessorActor(T step, TestCaseScope scope) {
        super(step, scope);

        expressionHandler = new ExpressionHandler(scope);
    }

    public AbstractMessagingStepProcessorActor(T step, TestCaseScope scope, String stepId) {
        super(step, scope, stepId);

        expressionHandler = new ExpressionHandler(scope);
    }

    protected abstract MessagingContext getMessagingContext();

    protected abstract TransactionContext getTransactionContext();

    protected MapType generateOutputWithModuleDefinition(MessagingContext messagingContext, Message message) {
        final IMessagingHandler messagingHandler = messagingContext.getHandler();
        MapType map = new MapType();
        for(int i=0; i<messagingHandler.getModuleDefinition().getOutputs().getParam().size(); i++) {
            TypedParameter outputParam = messagingHandler.getModuleDefinition().getOutputs().getParam().get(i);

            DataType data = message.getFragments().get(outputParam.getName());

            map.addItem(outputParam.getName(), data);
        }
        return map;
    }

    protected void checkRequiredParametersAndSetDefaultValues(List<Parameter> parameters, List<Configuration> configurations) {
        List<Parameter> requiredParameters = new ArrayList<>();

        for(Parameter parameter : parameters) {
            if(parameter.getUse() == UsageEnumeration.R) {
                requiredParameters.add(parameter);
            }
        }

        for(Parameter requiredParameter : requiredParameters) {
            boolean found = false;
            for(Configuration configuration : configurations) {
                if(configuration.getName().equals(requiredParameter.getName())) {
                    found = true;
                    break;
                }
            }

            if(!found) {
                if (requiredParameter.getValue() == null) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Missing required configuration parameter [" + requiredParameter.getName() + "]."));
                } else {
                    configurations
                            .add(ConfigurationUtils.constructConfiguration(requiredParameter.getName(), requiredParameter.getValue()));
                }
            }
        }
    }

    protected MapType generateOutputWithNameBinding(Message message, List<Binding> output) {
        MapType map = new MapType();
        for(Binding binding : output) {
            DataType data = message.getFragments().get(binding.getName());

            map.addItem(binding.getValue().substring(1), data);
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

    protected List<TypedParameter> getRequiredOutputParameters(List<TypedParameter> outputParameters) {
        List<TypedParameter> requiredOutputParameters = new ArrayList<>();

        for(TypedParameter outputParameter : outputParameters) {
            if(outputParameter.getUse() == UsageEnumeration.R) {
                requiredOutputParameters.add(outputParameter);
            }
        }

        return requiredOutputParameters;
    }

    protected void setInputWithModuleDefinition(Message message, List<Binding> input, List<TypedParameter> requiredParameters) throws IOException {
        IMessagingHandler messagingHandler = getMessagingContext().getHandler();
        for(int i=0; i<messagingHandler.getModuleDefinition().getInputs().getParam().size(); i++) {
            TypedParameter parameter = messagingHandler.getModuleDefinition().getInputs().getParam().get(i);

            requiredParameters.remove(parameter);

            DataType data = getInputValue(parameter.getType(), input.get(i));
            message.getFragments().put(parameter.getName(), data);
        }

        setDefaultValuesForRequiredParameters(message, requiredParameters);
    }

    protected void setInputWithNameBinding(Message message, List<Binding> input, List<TypedParameter> requiredParameters) throws IOException {
        IMessagingHandler messagingHandler = getMessagingContext().getHandler();
        for(Binding binding : input) {
            TypedParameter parameter = getTypedParameterByName(messagingHandler.getModuleDefinition().getInputs().getParam(), binding.getName());
            if (parameter == null) {
                throw new IllegalStateException("Unexpected input found with name ["+binding.getName()+"]");
            } else {
                requiredParameters.remove(parameter);
                DataType data = getInputValue(parameter.getType(), binding);
                message.getFragments().put(binding.getName(), data);
            }
        }

        setDefaultValuesForRequiredParameters(message, requiredParameters);
    }

    protected DataType getInputValue(String type, Binding input) {
        return expressionHandler.processExpression(input, type);
    }

    protected TypedParameter getTypedParameterByName(List<TypedParameter> parameters, String name) {
        for(TypedParameter parameter : parameters) {
            if(parameter.getName().equals(name)) {
                return parameter;
            }
        }

        return null;
    }

    protected void setDefaultValuesForRequiredParameters(Message message, List<TypedParameter> requiredParameters) {
        for(TypedParameter missingRequiredParameter : requiredParameters) {
            if(missingRequiredParameter.getValue() == null) {
                throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Missing input parameter ["+missingRequiredParameter.getName()+"]"));
            } else { // set the default value
                DataType defaultValue = DataTypeFactory.getInstance().create(missingRequiredParameter.getValue().getBytes(), missingRequiredParameter.getType());
                message.getFragments().put(missingRequiredParameter.getName(), defaultValue);
            }
        }
    }

    protected List<TypedParameter> getRequiredInputs(IMessagingHandler messagingHandler) {
        List<TypedParameter> requiredInputs = new ArrayList<>();
        if(messagingHandler.getModuleDefinition().getInputs() != null) {
            for(TypedParameter input : messagingHandler.getModuleDefinition().getInputs().getParam()) {
                if(input.getUse() == UsageEnumeration.R) {
                    requiredInputs.add(input);
                }
            }
        }

        return requiredInputs;
    }

    protected Message getMessageFromBindings(List<Binding> bindings) throws IOException {
        final IMessagingHandler messagingHandler = getMessagingContext().getHandler();
        Message message = new Message();
        boolean isNameBinding = BindingUtils.isNameBinding(step.getInput());
        if(isNameBinding) {
            setInputWithNameBinding(message, step.getInput(), getRequiredInputs(messagingHandler));
        } else {
            setInputWithModuleDefinition(message, step.getInput(), getRequiredInputs(messagingHandler));
        }
        return message;
    }

}
