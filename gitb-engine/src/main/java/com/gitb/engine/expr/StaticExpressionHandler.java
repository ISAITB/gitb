package com.gitb.engine.expr;

import com.gitb.types.DataType;

/**
 * An expression handler that is expected to be static in nature. A static expression handler is one that will only
 * process basic expressions but raise errors when encountering sources, templates or variable references.
 */
public class StaticExpressionHandler extends ExpressionHandler {

    public StaticExpressionHandler() {
        super(null, null, null);
    }

    @Override
    DataType processSourceExpression(String expectedReturnType, String sourceVariableExpression, String xpathExpression) {
        throw new IllegalStateException("Source definitions are not allowed when resolving expressions that are expected to be static");
    }

    @Override
    DataType processTemplate(String expectedReturnType, DataType expressionResult) {
        throw new IllegalStateException("Template processing is not allowed when resolving expressions that are expected to be static");
    }

    @Override
    DataType processVariableReference(String expression, String expectedReturnType) {
        throw new IllegalStateException("Variable references are not allowed when resolving expressions that are expected to be static");
    }
}
