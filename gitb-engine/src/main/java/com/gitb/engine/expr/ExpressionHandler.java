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
import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;
import com.sun.org.apache.xpath.internal.jaxp.XPathImpl;

import javax.xml.xpath.*;

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
        if (DataType.BINARY_DATA_TYPE.equals(expectedReturnType)
                || DataType.isListType(expectedReturnType)) {
            DataType result = variableResolver.resolveVariable(expression);
            return result;
        } else {
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
    }

    /*
    public DataType processExpression(String expression, String targetDataType) throws Exception {
        DataType returnData = null;

        //expression starts with a variable
        if(expression.startsWith("$")) {
            //expression contains XPath expressions
            if(expression.contains("/")) {
                String varName    = expression.substring(1, expression.indexOf("/"));
                String xpath      = expression.substring(expression.indexOf("/") + 1);
                DataType variable = scope.getVariable(varName).getValue();
                //process the xpath expression
                XPathExpression expr = createXPathExpression(xpath);
                returnData = variable.processXPath(expr, targetDataType);
            }
            //expression evaluates to a value of a variable
            else{
                //remove initial $
                String varName = expression.substring(1);
                returnData = scope.getVariable(varName).getValue();
            }
        }
        //expression evaluates to a value or a direct function call
        else{
            //expression is a function call
            if(expression.contains(":")){
                Document empty = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                //process the xpath expression which is, in fact, an XPath function call
                XPathExpression expr = createXPathExpression(expression);
                Object object  = expr.evaluate(empty, ObjectType.XPathConstantForDataType(targetDataType));
                //get the data, convert to appropriate DataType and return it
                returnData = DataType.create(targetDataType);
                returnData.setValue(object);
            }
            //expression is a literal
            else{
                returnData = DataType.create(expression.getBytes(), targetDataType); // TODO encoding?
            }
        }

        return returnData;
    }*/

    private XPathExpression createXPathExpression(String expression) {
        try {
            //create an XPath processor
            XPathImpl xPath = (XPathImpl) new XPathFactoryImpl().newXPath();
            //set namespace context and resolvers
            xPath.setNamespaceContext(namespaceContext);
            xPath.setXPathVariableResolver(variableResolver);
            xPath.setXPathFunctionResolver(functionResolver);
            //compile the expression and return it
            return xPath.compile(expression);
        }catch (XPathExpressionException e){
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Invalid XPath expression"),e);
        }
    }

    /*public static void main(String args[]) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        TestCaseContext context = new TestCaseContext(new TestCase(), "session");
        context.setScope(new TestCaseScope(context));
        context.getTestCase().setNamespaces(new Namespaces());
        context.getTestCase().getNamespaces().getNs().add(new Namespace());
        context.getTestCase().getNamespaces().getNs().get(0).setPrefix("srdc");
        context.getTestCase().getNamespaces().getNs().get(0).setValue("http://www.srdc.com.tr");
        StringType string = new StringType();
        string.setValue("5");
        StringType string2 = new StringType();
        string2.setValue("6");
        context.getScope().createVariable("xxx").setValue(string);
        context.getScope().createVariable("yyy").setValue(string2);

        String xml = "<?xml version=\"1.0\"?><A id=\"5\" xmlns=\"www.srdc.com.tr\"> senan </A>";

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xml)));

        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext(context.getScope()));
        xpath.setXPathVariableResolver(new VariableResolver(context.getScope()));
        xpath.setXPathFunctionResolver(new FunctionResolver(context.getScope()));
        XPathExpression expr = xpath.compile("/");

        Object x = expr.evaluate(document, XPathConstants.STRING);

        System.out.println(x);
    }*/


}
