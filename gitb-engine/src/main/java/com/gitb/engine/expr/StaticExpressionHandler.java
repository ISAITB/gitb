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
