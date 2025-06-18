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

package com.gitb.engine.expr;

import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.types.DataType;

/**
 * An expression handler that is expected to be static in nature. A static expression handler is one that will only
 * process basic expressions but raise errors when encountering sources or templates.
 */
public class StaticExpressionHandler extends ExpressionHandler {

    public StaticExpressionHandler(TestCaseScope scope) {
        super(scope, new VariableResolver(scope, true), null);
    }

    public StaticExpressionHandler(TestCaseScope scope, VariableResolver resolver) {
        super(scope, resolver, null);
    }

    @Override
    DataType processSourceExpression(String expectedReturnType, String sourceVariableExpression, String xpathExpression) {
        throw new IllegalStateException("Source definitions are not allowed when resolving expressions that are expected to be static");
    }

    @Override
    DataType processTemplate(String expectedReturnType, DataType expressionResult) {
        throw new IllegalStateException("Template processing is not allowed when resolving expressions that are expected to be static");
    }

}
