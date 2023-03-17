package com.gitb.engine.expr.resolvers;

import com.gitb.core.ErrorCode;
import com.gitb.engine.testcase.StepStatusMapType;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.types.*;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.XMLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathVariableResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by senan on 9/8/14.
 */
public class VariableResolver implements XPathVariableResolver{

    private static final Logger logger = LoggerFactory.getLogger(VariableResolver.class);
    //Regular expression for Variable Expressions and L-Values (Assignment to variables, Expression.source) in TDL
    // Valid Examples ('x' is the variable name):
    //      $x for any type of variable,
    //      $x{2} for accessing the element of a list  with index,
    //      $x{item} for accessing the element of a map with key 'item',
    //      $x{$i} for accessing the element of a map or list with index or key coming from the value of variable 'i' (either String or Number type)

	private static final String VARIABLE = "[a-zA-Z][a-zA-Z\\-_0-9]*";
	private static final String LITERAL_OR_VARIABLE = "[a-zA-Z\\-\\._0-9]*";
	private static final String NUMBERS = "[0-9]+";

    public static final String VARIABLE_EXPRESSION__NO_DOLLAR = "([a-zA-Z][a-zA-Z\\-_0-9]*)(?:\\{(?:[\\$\\{\\}a-zA-Z\\-\\._0-9]*)\\})*";
    public static final String VARIABLE_EXPRESSION = "\\$"+VARIABLE_EXPRESSION__NO_DOLLAR;
	private static final String INDEX_OR_KEY = "(?:(?:\\{((?:"+ LITERAL_OR_VARIABLE +")" +
		"|(?:"+ NUMBERS +")" +
		"|(?:"+ VARIABLE_EXPRESSION +"))\\})(.*))";

    public static final Pattern VARIABLE_EXPRESSION_PATTERN = Pattern.compile(VARIABLE_EXPRESSION);
	public static final Pattern INDEX_OR_KEY_PATTERN = Pattern.compile(INDEX_OR_KEY);
    public static final Pattern VARIABLE_PATTERN = Pattern.compile(VARIABLE);

    private static final Pattern BRACKET_DETECTION_PATTERN = Pattern.compile("(?:'[^']*'|(\\$(?:[a-zA-Z][a-zA-Z\\-_0-9]*)(?:\\{(?:[\\$\\{\\}a-zA-Z\\-\\._0-9]*)\\})*))|(?:\"[^\"]*\"|(\\$(?:[a-zA-Z][a-zA-Z\\-_0-9]*)(?:\\{(?:[\\$\\{\\}a-zA-Z\\-\\._0-9]*)\\})*))");
    private static final String CURLY_BRACKET_OPEN_REPLACEMENT = "_com.gitb.OPEN_";
    private static final String CURLY_BRACKET_CLOSE_REPLACEMENT = "_com.gitb.CLOSE_";
    private static final String DOLLAR_REPLACEMENT = "_com.gitb.DOLLAR_";

    private final TestCaseScope scope;

    private DocumentBuilder documentBuilder;

    public VariableResolver(TestCaseScope scope) {
        this.scope = scope;
        try {
            documentBuilder = XMLUtils.getSecureDocumentBuilderFactory().newDocumentBuilder();
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
        String variableExpression = "$"+toTDLExpression(name.getLocalPart());
        DataType value = resolveVariable(variableExpression);
        if(value instanceof PrimitiveType){
            if(value instanceof BinaryType){
                return value.convertTo(DataType.STRING_DATA_TYPE).getValue();
            }
            return value.getValue();
        }else if(value instanceof ObjectType){
            return value.getValue();
        }else if(value instanceof ListType){
            ListType list = (ListType)value;
            var itemValues = new ArrayList<>();
            for(int i=0;i<list.getSize();i++) {
                itemValues.add(list.getItem(i).getValue());
            }
            NodeList result;
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

    public static Pair<String, String> extractVariableNameFromExpression(String variableExpression) {
        Matcher matcher = VARIABLE_EXPRESSION_PATTERN.matcher(variableExpression);
        if(!matcher.matches()){
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Invalid variable reference ["+variableExpression+"]"));
        }
        return Pair.of(matcher.group(1), variableExpression.substring(matcher.end(1)));
    }

    public DataType resolveVariable(String variableExpression, boolean tolerateMissing) {
        DataType result = null;
        var variableName = extractVariableNameFromExpression(variableExpression);
        try {
            String containerVariableName = variableName.getLeft();
            //The remaining part
            String indexOrKeyExpression = variableName.getRight();
            TestCaseScope.ScopedVariable scopeVariable = scope.getVariable(containerVariableName);
            if (scopeVariable == null || !scopeVariable.isDefined()) {
                // No variable could be matched.
                if (!tolerateMissing && scope.getContext().getCurrentState() != TestCaseContext.TestCaseStateEnum.OUTPUT) {
                    logger.warn(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), "No variable could be located in the session context for expression [" + variableExpression + "]");
                }
            } else {
                DataType containerVariable = scopeVariable.getValue();
                result = resolveVariable(containerVariable, indexOrKeyExpression);
            }
        } catch (Exception e) {
            logger.warn(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), "An exception occurred when resolving variable [" + variableExpression + "]", e);
        }
        if (result == null && !tolerateMissing) {
            result = new StringType();
        }
        return result;
    }

	/**
	 * Resolve variable value from the given variable expression ex: $x{2}, $y
	 * This can be used externally by other types if they write their own implementation for Expression evaluation
	 * @param variableExpression
	 * @return
	 */
	public DataType resolveVariable(String variableExpression) {
	    return resolveVariable(variableExpression, false);
	}

    public StringType resolveVariableAsString(String variableExpression) {
        return (StringType)resolveVariable(variableExpression).convertTo(DataType.STRING_DATA_TYPE);
    }

    public BooleanType resolveVariableAsBoolean(String variableExpression) {
        return (BooleanType)resolveVariable(variableExpression).convertTo(DataType.BOOLEAN_DATA_TYPE);
    }

    public NumberType resolveVariableAsNumber(String variableExpression) {
        return (NumberType)resolveVariable(variableExpression).convertTo(DataType.NUMBER_DATA_TYPE);
    }

    public static boolean isVariableReference(String variableExpression) {
	    if (!StringUtils.isBlank(variableExpression)) {
            return VARIABLE_EXPRESSION_PATTERN.matcher(variableExpression).matches();
        }
        return false;
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
            temp.deserialize(object.serialize(ObjectType.DEFAULT_ENCODING));

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
        if (container instanceof ListType) {
            try {
                int index = Double.valueOf(keyOrIndex).intValue();
                return ((ListType) container).getItem(index);
            } catch (NumberFormatException e) {
                throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Value [" + StringUtils.defaultString(keyOrIndex) + "] must be numeric for it to be used as an index of its containing list."), e);
            }
        } else { //MapType
            DataType returnValue;
            if (container instanceof StepStatusMapType) {
                returnValue = ((StepStatusMapType)container).getScopedItem(keyOrIndex, scope);
            } else {
                returnValue = ((MapType)container).getItem(keyOrIndex);
            }
            if (returnValue == null) {
                // Check to see in case of numeric index.
                try {
                    returnValue = ((MapType)container).getItem(String.valueOf(Double.valueOf(keyOrIndex).intValue()));
                } catch (Exception e) {
                    // Ignore.
                }
            }
            return returnValue;
        }
    }

    private static String processMatch(MatchResult match, int group, String expression) {
        var matchedText = match.group(group);
        if (matchedText != null) {
            // Replace all curly brackets and all dollar signs except the first one (which is always there for matches).
            var variableExpression = matchedText.substring(1)      // Remove initial dollar.
                    .replace("{", CURLY_BRACKET_OPEN_REPLACEMENT)  // Replace curly brace open.
                    .replace("}", CURLY_BRACKET_CLOSE_REPLACEMENT) // Replace curly brace close.
                    .replace("$", DOLLAR_REPLACEMENT);             // Replace dollars.
            variableExpression = "$"+variableExpression;           // Add initial dollar.
            return new StringBuilder(expression)
                        .replace(match.start(group), match.end(group), variableExpression)
                        .toString();
        } else {
            return expression;
        }
    }

    public static String toLegalXPath(String expression) {
        // GITB TDL expressions contain curly braces for container types which are reserved characters in XPath 2.0+
        var matcher = BRACKET_DETECTION_PATTERN.matcher(expression);
        List<MatchResult> matches = matcher.results().collect(Collectors.toList());
        // Reverse so that the sections to replace don't overlap with the replacements.
        Collections.reverse(matches);
        for (var match: matches) {
            for (int i=1; i <= matcher.groupCount(); i++) {
                expression = processMatch(match, i, expression);
            }
        }
        return expression;
    }

    public static String toTDLExpression(String expression) {
        return expression.replace(CURLY_BRACKET_OPEN_REPLACEMENT, "{")
                .replace(CURLY_BRACKET_CLOSE_REPLACEMENT, "}")
                .replace(DOLLAR_REPLACEMENT, "$");
    }
}
