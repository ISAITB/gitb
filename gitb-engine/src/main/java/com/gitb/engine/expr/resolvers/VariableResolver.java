package com.gitb.engine.expr.resolvers;

import com.gitb.core.ErrorCode;
import com.gitb.core.ErrorInfo;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.types.*;
import com.gitb.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathVariableResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by senan on 9/8/14.
 */
public class VariableResolver implements XPathVariableResolver{
    private static Logger logger = LoggerFactory.getLogger(VariableResolver.class);
    //Regular expression for Variable Expressions and L-Values (Assignment to variables, Expression.source) in TDL
    // Valid Examples ('x' is the variable name):
    //      $x for any type of variable,
    //      $x{2} for accessing the element of a list  with index,
    //      $x{item} for accessing the element of a map with key 'item',
    //      $x{$i} for accessing the element of a map or list with index or key coming from the value of variable 'i' (either String or Number type)

	private static String VARIABLE = "[a-zA-Z][a-zA-Z\\-_0-9]*";
	private static String LITERAL_OR_VARIABLE = "[a-zA-Z\\-\\._0-9]*";
	private static String NUMBERS = "[0-9]+";

	private static String VARIABLE_EXPRESSION = "\\$([a-zA-Z][a-zA-Z\\-_0-9]*)(?:\\{(?:[\\$\\{\\}a-zA-Z\\-\\._0-9]*)\\})*";
	private static String INDEX_OR_KEY = "(?:(?:\\{((?:"+ LITERAL_OR_VARIABLE +")" +
		"|(?:"+ NUMBERS +")" +
		"|(?:"+ VARIABLE_EXPRESSION +"))\\})(.*))";

    public static final Pattern VARIABLE_EXPRESSION_PATTERN = Pattern.compile(VARIABLE_EXPRESSION);
	public static final Pattern INDEX_OR_KEY_PATTERN = Pattern.compile(INDEX_OR_KEY);
	public static final Pattern INDEX_PATTERN = Pattern.compile("("+NUMBERS+")");
	public static final Pattern KEY_PATTERN = Pattern.compile("("+LITERAL_OR_VARIABLE+")");


    private TestCaseScope scope;

    private DocumentBuilder documentBuilder;

    public VariableResolver(TestCaseScope scope) {
        this.scope = scope;
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used by XPath Expression Evaluator to resolve the values of the variables referred in the XPath itself
     * @param name
     * @return
     *      Returns Java correspondents for GITB Primitive types.
     *      Return Node for Object Type
     *      Try to return Node for other plugged-in types (by trying to convert them into XML representation)
     *      Return NodeList of Text nodes for list or map of primitive types (ex: list[number]).
     *      Return NodeList for list[object] type
     *      Try to return NodeList for list of other types (by trying to convert them into XML representation)
     */
    @Override
    public Object resolveVariable(QName name) {
        String variableExpression = "$"+name.getLocalPart();
        DataType value = resolveVariable(variableExpression);
        if(value instanceof PrimitiveType){
            if(value instanceof BinaryType){
                throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Invalid variable reference in expression, binary variables can not be used in expressions!"));
            }
            return value.getValue();
        }else if(value instanceof ObjectType){
            return value.getValue();
        }else if(value instanceof ListType){
            ListType list = (ListType)value;
            List itemValues = new ArrayList();
            for(int i=0;i<list.getSize();i++) {
                itemValues.add(list.getItem(i).getValue());
            }
            NodeList result = null;
            switch(list.getContainedType()){
                case DataType.NUMBER_DATA_TYPE:
                case DataType.STRING_DATA_TYPE:
                case DataType.BOOLEAN_DATA_TYPE:
                    result = convertPrimitiveListToNodeList(itemValues);
                    break;
                case DataType.OBJECT_DATA_TYPE:
                    result = convertListOfNodesToNodeList(itemValues);
                    break;
                default:
                    List<DataType> objects = (List<DataType>)list.getValue();
                    result = convertListOfNodesToNodeList(convertListOfOthersToListOfNodes(objects));
            }
            return result;
        } else if(value instanceof  MapType){
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "MapType cannot be used in expressions!"));
        }
        throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Variable ["+variableExpression +"] is not resolved!"));
    }

	/**
	 * Resolve variable value from the given variable expression ex: $x{2}, $y
	 * This can be used externally by other types if they write their own implementation for Expression evaluation
	 * @param variableExpression
	 * @return
	 */
	public DataType resolveVariable(String variableExpression) {
		Matcher matcher = VARIABLE_EXPRESSION_PATTERN.matcher(variableExpression);
		if(!matcher.matches()){
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Invalid variable reference ["+variableExpression+"]"));
		}
		try {
			String containerVariableName = matcher.group(1);
			//The remaining part
			String indexOrKeyExpression = variableExpression.substring(matcher.end(1));
			TestCaseScope.ScopedVariable scopeVariable = scope.getVariable(containerVariableName);
			DataType containerVariable = scopeVariable.getValue();
			return resolveVariable(containerVariable, indexOrKeyExpression);
		} catch (Exception e) {
            ErrorInfo errorInfo = ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE);
            errorInfo.setDescription("An exception occurred when resolving variable [" + variableExpression + "]");
			throw new GITBEngineInternalError(errorInfo, e);
		}
	}

    public boolean isVariableReference(String variableExpression) {
        Matcher matcher = VARIABLE_EXPRESSION_PATTERN.matcher(variableExpression);
        return matcher.matches();
    }

    /**
     * Try to convert list of other types to NodeList
     * @param list
     * @return
     */
    private List<Node> convertListOfOthersToListOfNodes(List<DataType> list) {
        List<Node> nodeList = new ArrayList<Node>();
        for(DataType object :list){
            DataType temp = DataTypeFactory.getInstance().create(DataType.OBJECT_DATA_TYPE);
            temp.deserialize(object.serialize(ObjectType.DEFAULT_COMMON_ENCODING_FORMAT));

            nodeList.add((Node)temp.getValue());
        }
        return nodeList;
    }

    /**
     * Convert List of Nodes into a NodeList
     * @param listOfNodes
     * @return
     */
    private NodeList convertListOfNodesToNodeList(List listOfNodes){
        Document temp = documentBuilder.newDocument();
        temp.appendChild(temp.createElement("Temp"));
        Element tempRoot = temp.getDocumentElement();
        for(Object node : listOfNodes) {
            tempRoot.appendChild(temp.adoptNode((Node) node));
        }
        return tempRoot.getChildNodes();
    }

    /**
     * Convert a primitive List into a NodeList with Text nodes
     * @param primitiveList
     * @return
     */
    private NodeList convertPrimitiveListToNodeList(List primitiveList){
        Document temp = documentBuilder.newDocument();
        temp.appendChild(temp.createElement("Temp"));
        Element tempRoot = temp.getDocumentElement();
        for(Object primitive : primitiveList) {
            tempRoot.appendChild(temp.createTextNode(primitive.toString()));
        }
        return tempRoot.getChildNodes();
    }

    /**
     * Resolve an index expression embedded in a variable expression
     * @param indexOrKeyExpression
     * @return
     */
    private String resolveIndexOrKeyExpression(String indexOrKeyExpression){
	    Matcher variableExpressionMatcher = VARIABLE_EXPRESSION_PATTERN.matcher(indexOrKeyExpression);
	    if(variableExpressionMatcher.matches()) {
		    DataType tempValue = resolveVariable(indexOrKeyExpression);
		    if(tempValue instanceof NumberType || tempValue instanceof StringType){
			    return tempValue.getValue().toString();
		    }else{
			    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Index of a container variable should be either String or Number, not [" + tempValue.getType() + "]!"));
		    }
	    } else {
		    return indexOrKeyExpression;
        }
    }

    /**
     * Resolve Variable value from the Container Value and indexOrKeyExpression part if exists
     * @param containerValue - Value of the container
     * @param keyOrIndexExpressions - The remaining expression (optional) for accessing the items in a container
     * @return
     */
    private DataType resolveVariable(DataType containerValue, String keyOrIndexExpressions){
        DataType tempValue = containerValue;
	    String expression = keyOrIndexExpressions;
        //If we are accessing an item in a container type
        if(expression != null){
	        do {
		        Matcher matcher = INDEX_OR_KEY_PATTERN.matcher(expression);
		        if(!matcher.matches()) {
			        break;
		        }

		        String indexOrKeyExpression = matcher.group(1);
		        String indexOrKey = resolveIndexOrKeyExpression(indexOrKeyExpression);

		        tempValue = resolveItemInContainer(tempValue, indexOrKey);
		        expression = matcher.group(3);
	        } while(expression.length() > 0);
        }
        return tempValue;
    }

    /**
     * Resolve item in a container by giving the index or key
     * @param container List or Map type object
     * @param keyOrIndex Numeric index for list type or string keys for map type
     * @return
     */
    private DataType resolveItemInContainer(DataType container, String keyOrIndex){
        if(!(container instanceof ContainerType)){
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Invalid variable reference, you can use index or key only on container types"));
        }
        if(container instanceof ListType){
            int index = Integer.parseInt(keyOrIndex);
            return ((ListType) container).getItem(index);
        }
        //MapType
        else {
            return ((MapType)container).getItem(keyOrIndex);
        }
    }

    /**
     * Checks if an expression is a valid TDL Variable Expression used in TDL expressions
     * @param expression
     * @return
     */
    public static boolean isVariableExpression(String expression){
        return VARIABLE_EXPRESSION_PATTERN.matcher(expression).matches();
    }
}
