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

import com.gitb.core.ErrorCode;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.TemplateUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Expression;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.TestSessionNamespaceContext;
import net.sf.saxon.xpath.XPathFactoryImpl;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

/**
 * Created by senan on 9/5/14.
 */
public class ExpressionHandler {

    private final TestCaseScope scope;
    private final VariableResolver variableResolver;
    private final TestSessionNamespaceContext namespaceContext;

    public ExpressionHandler(TestCaseScope scope) {
        this(scope, new VariableResolver(scope), new TestSessionNamespaceContext(scope.getNamespaceDefinitions()));
    }

    ExpressionHandler(TestCaseScope scope, VariableResolver variableResolver, TestSessionNamespaceContext namespaceContext) {
        this.scope = scope;
        this.variableResolver = variableResolver;
        this.namespaceContext = namespaceContext;
    }

    public VariableResolver getVariableResolver() {
        return variableResolver;
    }

    public DataType processExpression(Expression expression) {
        return processExpression(expression, null);
    }

    public DataType processExpression(Expression expression, String expectedReturnType) {
        String sourceVariableExpression = expression.getSource();
        String xpathExpression = expression.getValue();
        boolean asTemplate = expression.isAsTemplate();
        DataType expressionResult;
        if (sourceVariableExpression != null) {
            expressionResult = processSourceExpression(expectedReturnType, sourceVariableExpression, xpathExpression);
        } else {
            expressionResult = processExpression(xpathExpression, expectedReturnType);
        }
        if (asTemplate) {
            expressionResult = processTemplate(expectedReturnType, expressionResult);
        }
        return expressionResult;
    }

    DataType processTemplate(String expectedReturnType, DataType expressionResult) {
        if (expectedReturnType == null) {
            expectedReturnType = expressionResult.getType();
        }
        expressionResult = TemplateUtils.generateDataTypeFromTemplate(scope, expressionResult, expectedReturnType);
        return expressionResult;
    }

    DataType processSourceExpression(String expectedReturnType, String sourceVariableExpression, String xpathExpression) {
        DataType expressionResult;
        DataType source = variableResolver.resolveVariable(sourceVariableExpression);
        if(xpathExpression == null || xpathExpression.equals("")) {
            //if nothing to process, return the source immediately
            expressionResult = source;
        } else {
            expressionResult = processExpression(source, xpathExpression, expectedReturnType);
        }
        return expressionResult;
    }

    DataType processVariableReference(String expression, String expectedReturnType) {
        DataType result = variableResolver.resolveVariable(expression);
        if (result == null) {
            throw new IllegalStateException("Expression ["+ expression +"] did not resolve an existing session variable");
        }
        return result.convertTo(expectedReturnType);
    }

    private DataType processExpression(DataType source, String expression, String expectedReturnType)  {
        XPathExpression expr = createXPathExpression(expression);
        return source.processXPath(expr, expectedReturnType);
    }

    private DataType processExpression(String expression, String expectedReturnType)  {
        if (VariableResolver.isVariableReference(expression)) {
            // This is a pure reference to a context variable
            return processVariableReference(expression, expectedReturnType);
        } else {
            // This is a complete XPath expression
            DataType emptySource = DataTypeFactory.getInstance().create(DataType.OBJECT_DATA_TYPE);
            return processExpression(emptySource, expression, expectedReturnType);
        }
    }

    private XPathExpression createXPathExpression(String expression) {
        expression = VariableResolver.toLegalXPath(expression);
        try {
            // Create an XPath processor
            var factory = new XPathFactoryImpl();
            if (variableResolver != null) {
                factory.setXPathVariableResolver(variableResolver);
            }
            XPath xPath = factory.newXPath();
            if (namespaceContext != null) {
                xPath.setNamespaceContext(namespaceContext);
            }
            // Compile the expression and return it
            return xPath.compile(expression);
        }catch (XPathExpressionException e){
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Invalid XPath expression"),e);
        }
    }

}
