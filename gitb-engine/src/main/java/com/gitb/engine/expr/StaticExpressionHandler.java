/*
 * Copyright (C) 2026 European Union
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

package com.gitb.engine.expr;

import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.CallStep;
import com.gitb.tdl.Scriptlet;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.StringType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * An expression handler that is expected to be static in nature. A static expression handler is one that will only
 * process basic expressions but raise errors when encountering sources or templates.
 */
public class StaticExpressionHandler extends ExpressionHandler {

    public StaticExpressionHandler(TestCaseScope scope) {
        super(scope, new TolerantVariableResolver(scope), null);
    }

    @Override
    DataType processSourceExpression(String expectedReturnType, String sourceVariableExpression, String xpathExpression) {
        throw new IllegalStateException("Source definitions are not allowed when resolving expressions that are expected to be static");
    }

    @Override
    DataType processTemplate(String expectedReturnType, DataType expressionResult) {
        throw new IllegalStateException("Template processing is not allowed when resolving expressions that are expected to be static");
    }

    public StaticExpressionHandler newForScriptlet(Scriptlet scriptlet, CallStep callStep) {
        TestCaseScope newScope = scope.createChildScope();
        if (scriptlet.getParams() != null && !scriptlet.getParams().getVar().isEmpty()) {
            var parameters = new HashMap<String, Supplier<DataType>>();
            // Record the parameters and their default values.
            scriptlet.getParams().getVar().forEach(parameter -> {
                Supplier<DataType> defaultValueFn = null;
                if (!parameter.getValue().isEmpty()) {
                    defaultValueFn = () -> DataTypeFactory.getInstance().create(parameter);
                }
                parameters.put(parameter.getName(), defaultValueFn);
            });
            // For parameters with corresponding inputs, replace the values based on the input values.
            callStep.getInput().forEach((input) -> {
                /*
                 * Only process the inputs that are named, don't involve template processing and don't have source definitions.
                 */
                if (input.getName() != null && parameters.containsKey(input.getName())) {
                    DataType value = null;
                    if (input.getValue() != null && !input.isAsTemplate() && input.getSource() == null) {
                        value = processExpression(input);
                    }
                    DataType valueToUse = value;
                    parameters.put(input.getName(), () -> Objects.requireNonNullElseGet(valueToUse, () -> new StringType(StringUtils.defaultString(input.getValue()))));
                }
            });
            // Add the parameters with resolved values to the scope.
            parameters.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .forEach(entry -> {
                        var scopedVariable = newScope.createVariable(entry.getKey());
                        scopedVariable.setValue(entry.getValue().get());
                    });
        }
        return new StaticExpressionHandler(newScope);
    }

    public void close() {
        if (scope != null && scope.getParent() != null) {
            scope.getParent().removeChildScope(scope);
        }
    }

    /**
     * A variable resolver that will never throw errors or log warnings when resolving expressions.
     */
    private static class TolerantVariableResolver extends VariableResolver {

        private TolerantVariableResolver(TestCaseScope scope) {
            super(scope, true);
        }

        @Override
        protected GITBEngineInternalError raiseError(Supplier<String> messageSupplier, Throwable cause) {
            return new GITBEngineIgnoredError();
        }
    }

}
