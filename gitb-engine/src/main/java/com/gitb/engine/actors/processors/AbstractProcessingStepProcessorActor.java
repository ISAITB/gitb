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

import com.gitb.common.AliasManager;
import com.gitb.core.ErrorCode;
import com.gitb.core.TypedParameter;
import com.gitb.core.TypedParameters;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.HandlerUtils;
import com.gitb.engine.utils.StepContext;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.processing.IProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.remote.HandlerTimeoutException;
import com.gitb.remote.processing.RemoteProcessingModuleClient;
import com.gitb.tdl.Binding;
import com.gitb.tdl.Process;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;

import java.util.*;
import java.util.function.Function;

public abstract class AbstractProcessingStepProcessorActor<T extends Process> extends AbstractTestStepActor<T> {

    private final ExpressionHandler expressionHandler;

    @Override
    protected void handleFutureFailure(Throwable failure) {
        if (failure instanceof HandlerTimeoutException) {
            HandlerUtils.recordHandlerTimeout(step.getHandlerTimeoutFlag(), scope, true);
        }
        super.handleFutureFailure(failure);
    }

    public AbstractProcessingStepProcessorActor(T step, TestCaseScope scope, String stepId, StepContext stepContext) {
        super(step, scope, stepId, stepContext);
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
                if (handler instanceof RemoteProcessingModuleClient || handler.getModuleDefinition() == null || handler.getModuleDefinition().getId() == null) {
                    setInputWithNameBinding(data, bindings, params, (inputName) -> inputName);
                } else {
                    setInputWithNameBinding(data, bindings, params, (inputName) -> AliasManager.getInstance().resolveProcessingHandlerInput(handler.getModuleDefinition().getId(), inputName));
                }
            } else {
                setInputWithModuleDefinition(data, bindings, params);
            }
        }
        checkRequiredParameters(data, params);
        return data;
    }

    private void setInputWithInputValue(ProcessingData processingData, String inputValue, List<TypedParameter> params) {
        // Resolve the variable.
        var variableResolver = new VariableResolver(scope);
        DataType inputType;
        if (VariableResolver.isVariableReference(inputValue)) {
            inputType = variableResolver.resolveVariable(inputValue);
        } else {
            inputType = new StringType(inputValue);
        }
        // Find the first required parameter matching the input's type.
        var locatedParam = params.stream().filter(p -> p.getUse() == UsageEnumeration.R && Objects.equals(p.getType(), inputType.getType())).findFirst();
        if (locatedParam.isEmpty()) {
            // Find the first required parameter regardless of type.
            locatedParam = params.stream().filter(p -> p.getUse() == UsageEnumeration.R).findFirst();
            if (locatedParam.isEmpty()) {
                // Find the first optional parameter matching the input's type.
                locatedParam = params.stream().filter(p -> p.getUse() == UsageEnumeration.O && Objects.equals(p.getType(), inputType.getType())).findFirst();
                if (locatedParam.isEmpty()) {
                    // Find the first optional parameter regardless of type.
                    locatedParam = params.stream().filter(p -> p.getUse() == UsageEnumeration.O).findFirst();
                }
            }
        }
        if (locatedParam.isPresent()) {
            processingData.addInput(locatedParam.get().getName(), inputType.convertTo(locatedParam.get().getType()));
        } else {
            processingData.addInput("", inputType);
        }
    }

    private void setInputWithNameBinding(ProcessingData processingData, List<Binding> input, List<TypedParameter> params, Function<String, String> inputResolver) {
        for (Binding binding : input) {
            String inputName = inputResolver.apply(binding.getName());
            TypedParameter parameter = getParam(params, inputName);
            DataType data;
            if (parameter == null) {
                data = expressionHandler.processExpression(binding);
            } else {
                data = expressionHandler.processExpression(binding, parameter.getType());
            }
            processingData.addInput(inputName, data);
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
        var definition = handler.getModuleDefinition();
        if (definition != null) {
            if (operation == null) {
                if (definition.getOperation().size() == 1) {
                    return definition.getOperation().get(0).getInputs();
                }
            } else {
                for (var processingOperation : definition.getOperation()) {
                    if (processingOperation.getName().equals(operation)) {
                        return processingOperation.getInputs();
                    }
                }
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
