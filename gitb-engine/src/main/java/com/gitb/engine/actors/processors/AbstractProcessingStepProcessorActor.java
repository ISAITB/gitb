package com.gitb.engine.actors.processors;

import com.gitb.core.ErrorCode;
import com.gitb.core.TypedParameter;
import com.gitb.core.TypedParameters;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.processing.IProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.ps.ProcessingOperation;
import com.gitb.tdl.Binding;
import com.gitb.tdl.Process;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class AbstractProcessingStepProcessorActor<T extends Process> extends AbstractTestStepActor<T> {

    private final ExpressionHandler expressionHandler;

    public AbstractProcessingStepProcessorActor(T step, TestCaseScope scope, String stepId) {
        super(step, scope, stepId);
        expressionHandler = new ExpressionHandler(scope);
    }

    protected ProcessingData getData(IProcessingHandler handler, String operation) {
        ProcessingData data = new ProcessingData();
        List<TypedParameter> params = getParams(handler, operation);
        List<Binding> bindings = step.getInput();
        if (bindings.isEmpty()) {
            if (step.getInputAttribute() != null) {
                setInputWithInputValue(data, step.getInputAttribute(), params);
            }
        } else {
            boolean isNameBinding = BindingUtils.isNameBinding(bindings);
            if (isNameBinding) {
                setInputWithNameBinding(data, bindings, params);
            } else {
                setInputWithModuleDefinition(data, bindings, params);
            }
        }
        checkRequiredParameters(data, params);
        return data;
    }

    private void setInputWithInputValue(ProcessingData processingData, String inputValue, List<TypedParameter> params) {
        // Find the first required parameter or, if no required parameter exists, the first optional parameter.
        TypedParameter requiredParameter = null;
        TypedParameter optionalParameter = null;
        for (var param: params) {
            if (param.getUse() == UsageEnumeration.R && requiredParameter == null) {
                requiredParameter = param;
                if (optionalParameter != null) {
                    break;
                }
            } else if (param.getUse() == UsageEnumeration.O && optionalParameter == null) {
                optionalParameter = param;
                if (requiredParameter != null) {
                    break;
                }
            }
        }
        // Resolve the variable.
        var variableResolver = new VariableResolver(scope);
        DataType inputType;
        if (variableResolver.isVariableReference(inputValue)) {
            inputType = variableResolver.resolveVariable(inputValue);
        } else {
            inputType = new StringType(inputValue);
        }
        var parameterToUse = (requiredParameter == null)?optionalParameter:requiredParameter;
        if (parameterToUse != null) {
            inputType = inputType.convertTo(parameterToUse.getType());
            processingData.addInput(parameterToUse.getName(), inputType);
        } else {
            processingData.addInput("", inputType);
        }
    }

    private void setInputWithNameBinding(ProcessingData processingData, List<Binding> input, List<TypedParameter> params) {
        for (Binding binding : input) {
            TypedParameter parameter = getParam(params, binding.getName());
            DataType data;
            if (parameter == null) {
                data = expressionHandler.processExpression(binding);
            } else {
                data = expressionHandler.processExpression(binding, parameter.getType());
            }
            processingData.addInput(binding.getName(), data);
        }
    }

    private void setInputWithModuleDefinition(ProcessingData processingData, List<Binding> input, List<TypedParameter> params) {
        Iterator<TypedParameter> expectedParamsIterator = params.iterator();
        Iterator<Binding> inputsIterator = input.iterator();
        while (expectedParamsIterator.hasNext() && inputsIterator.hasNext()) {
            TypedParameter expectedParam = expectedParamsIterator.next();
            Binding inputExpression = inputsIterator.next();
            DataType result = expressionHandler.processExpression(inputExpression, expectedParam.getType());
            processingData.addInput(expectedParam.getName(), result);
        }
    }

    private void checkRequiredParameters(ProcessingData processingData, List<TypedParameter> expectedParameters) {
        for (TypedParameter param : expectedParameters) {
            if (param.getUse() == UsageEnumeration.R && !processingData.hasInput(param.getName())) {
                if (param.getValue() == null) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Missing input parameter [" + param.getName() + "]"));
                } else {
                    DataType defaultValue = DataTypeFactory.getInstance().create(param.getValue().getBytes(), param.getType());
                    processingData.getData().put(param.getName(), defaultValue);
                }
            }
        }
    }

    private TypedParameter getParam(List<TypedParameter> parameters, String name) {
        for (TypedParameter parameter : parameters) {
            if (parameter.getName().equals(name)) {
                return parameter;
            }
        }
        return null;
    }

    private List<TypedParameter> getParams(IProcessingHandler handler, String operation) {
        TypedParameters params = getInputs(handler, operation);
        return params != null ? params.getParam() : Collections.emptyList();
    }

    private TypedParameters getInputs(IProcessingHandler handler, String operation) {
        for (ProcessingOperation processingOperation : handler.getModuleDefinition().getOperation()) {
            if (processingOperation.getName().equals(operation)) {
                return processingOperation.getInputs();
            }
        }
        return null;
    }

    protected MapType getValue(ProcessingData data) {
        MapType map = new MapType();
        for (Map.Entry<String, DataType> entry : data.getData().entrySet()) {
            map.addItem(entry.getKey(), entry.getValue());
        }
        return map;
    }

}
