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
import com.gitb.tdl.Expression;
import com.gitb.tdl.Process;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractProcessingStepProcessorActor<T extends Process> extends AbstractTestStepActor<T> {

    private ExpressionHandler expressionHandler;
    private VariableResolver variableResolver;

    public AbstractProcessingStepProcessorActor(T step, TestCaseScope scope, String stepId) {
        super(step, scope, stepId);
        expressionHandler = new ExpressionHandler(scope);
        variableResolver = new VariableResolver(scope);
    }

    protected ProcessingData getData(IProcessingHandler handler, String operation) {
        ProcessingData data = new ProcessingData();
        List<Binding> bindings = step.getInput();
        boolean isNameBinding = BindingUtils.isNameBinding(bindings);
        if (isNameBinding) {
            setInputWithNameBinding(data, bindings, operation, handler);
        } else {
            setInputWithModuleDefinition(data, bindings, operation, handler);
        }
        return data;
    }

    protected void setInputWithNameBinding(ProcessingData processingData, List<Binding> input, String operation, IProcessingHandler handler) {
        List<TypedParameter> params = getParams(handler, operation);
        List<TypedParameter> requiredParams = getRequiredParams(params);
        for (Binding binding : input) {
            TypedParameter parameter = getParam(params, binding.getName());
            requiredParams.remove(parameter);
            DataType data = getDataType(parameter.getType(), binding);
            processingData.getData().put(binding.getName(), data);
        }
        setDefaultValuesForRequiredParameters(processingData, requiredParams);
    }

    protected void setInputWithModuleDefinition(ProcessingData processingData, List<Binding> input, String operation, IProcessingHandler handler) {
        List<TypedParameter> params = getParams(handler, operation);
        List<TypedParameter> requiredParams = getRequiredParams(params);
        for (int i = 0; i < params.size(); i++) {
            TypedParameter parameter = params.get(i);
            requiredParams.remove(parameter);
            DataType data = getDataType(parameter.getType(), input.get(i));
            processingData.getData().put(parameter.getName(), data);
        }
        setDefaultValuesForRequiredParameters(processingData, requiredParams);
    }

    protected void setDefaultValuesForRequiredParameters(ProcessingData processingData, List<TypedParameter> requiredParams) {
        for (TypedParameter param : requiredParams) {
            if (param.getValue() == null) {
                throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Missing input parameter [" + param.getName() + "]"));
            } else {
                DataType defaultValue = DataTypeFactory.getInstance().create(param.getValue().getBytes(), param.getType());
                processingData.getData().put(param.getName(), defaultValue);
            }
        }
    }

    protected DataType getDataType(String type, Binding binding) {
        DataType dataType;
        if (VariableResolver.isVariableExpression(binding.getValue())) {
            if (variableResolver.isVariableReference(binding.getValue())) {
                dataType = variableResolver.resolveVariable(binding.getValue());
            } else {
                Expression expression = new Expression();
                expression.setSource(binding.getSource());
                expression.setValue(binding.getValue());
                dataType = expressionHandler.processExpression(expression, type);
            }
        } else {
            dataType = DataTypeFactory.getInstance().create(type);
            dataType.setValue(binding.getValue());
        }
        return dataType;
    }

    protected TypedParameter getParam(List<TypedParameter> parameters, String name) {
        for (TypedParameter parameter : parameters) {
            if (parameter.getName().equals(name)) {
                return parameter;
            }
        }
        return null;
    }

    protected List<TypedParameter> getRequiredParams(List<TypedParameter> params) {
        List<TypedParameter> result = new ArrayList<>();
        for (TypedParameter param : params) {
            if (param.getUse() == UsageEnumeration.R) {
                result.add(param);
            }
        }
        return result;
    }

    protected List<TypedParameter> getParams(IProcessingHandler handler, String operation) {
        TypedParameters params = getInputs(handler, operation);
        return params != null ? params.getParam() : Collections.<TypedParameter>emptyList();
    }

    protected TypedParameters getInputs(IProcessingHandler handler, String operation) {
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
