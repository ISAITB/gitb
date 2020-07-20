package com.gitb.engine.actors.processors;

import com.gitb.core.ErrorCode;
import com.gitb.core.TypedParameter;
import com.gitb.core.TypedParameters;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.expr.ExpressionHandler;
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
        List<Binding> bindings = step.getInput();
        boolean isNameBinding = BindingUtils.isNameBinding(bindings);
        if (isNameBinding) {
            setInputWithNameBinding(data, bindings, operation, handler);
        } else {
            setInputWithModuleDefinition(data, bindings, operation, handler);
        }
        return data;
    }

    private void setInputWithNameBinding(ProcessingData processingData, List<Binding> input, String operation, IProcessingHandler handler) {
        List<TypedParameter> params = getParams(handler, operation);
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
        checkRequiredParameters(processingData, params);
    }

    private void setInputWithModuleDefinition(ProcessingData processingData, List<Binding> input, String operation, IProcessingHandler handler) {
        List<TypedParameter> params = getParams(handler, operation);
        Iterator<TypedParameter> expectedParamsIterator = params.iterator();
        Iterator<Binding> inputsIterator = input.iterator();
        while (expectedParamsIterator.hasNext() && inputsIterator.hasNext()) {
            TypedParameter expectedParam = expectedParamsIterator.next();
            Binding inputExpression = inputsIterator.next();
            DataType result = expressionHandler.processExpression(inputExpression, expectedParam.getType());
            processingData.addInput(expectedParam.getName(), result);
        }
        checkRequiredParameters(processingData, params);
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
