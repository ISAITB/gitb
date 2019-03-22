package com.gitb.engine.expr;

import com.gitb.core.ErrorCode;
import com.gitb.engine.expr.resolvers.FunctionResolver;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Expression;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.utils.ErrorUtils;
import net.sf.saxon.xpath.XPathFactoryImpl;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Created by senan on 9/5/14.
 */
public class ExpressionHandler{
    private final TestCaseScope scope;
    private VariableResolver variableResolver;
    private FunctionResolver functionResolver;
    private NamespaceContext namespaceContext;

    public ExpressionHandler(TestCaseScope scope) {
        this.scope = scope;
        variableResolver = new VariableResolver(scope);
        functionResolver = new FunctionResolver(scope);
        namespaceContext = new NamespaceContext(scope);
    }


    public DataType processExpression(Expression expression, String expectedReturnType) {
        String sourceVariableExpression = expression.getSource();
        String xpathExpression = expression.getValue();
        if(sourceVariableExpression != null) {
            DataType source = variableResolver.resolveVariable(sourceVariableExpression);
            if(xpathExpression == null || xpathExpression.equals("")) {
                //if nothing to process, return the source immediately
                return source;
            } else {
                return processExpression(source, xpathExpression, expectedReturnType);
            }
        } else {
            return processExpression(xpathExpression, expectedReturnType);
        }
    }

    private DataType processExpression(DataType source, String expression, String expectedReturnType)  {
        XPathExpression expr = createXPathExpression(expression);
        return source.processXPath(expr,expectedReturnType);
    }

    private DataType processExpression(String expression, String expectedReturnType)  {
        if (variableResolver.isVariableReference(expression)) {
            // This is a pure reference to a context variable
            DataType result = variableResolver.resolveVariable(expression);
            return result.convertTo(expectedReturnType);
        } else {
            // This is a complete XPath expression
            DataType emptySource = DataTypeFactory.getInstance().create(DataType.OBJECT_DATA_TYPE);
            return processExpression(emptySource, expression, expectedReturnType);
        }
    }

    private XPathExpression createXPathExpression(String expression) {
        try {
            //create an XPath processor
            XPathFactory factory = new XPathFactoryImpl();
            factory.setXPathFunctionResolver(functionResolver);
            factory.setXPathVariableResolver(variableResolver);
            XPath xPath = factory.newXPath();
            xPath.setNamespaceContext(namespaceContext);
            //compile the expression and return it
            return xPath.compile(expression);
        }catch (XPathExpressionException e){
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Invalid XPath expression"),e);
        }
    }

}
