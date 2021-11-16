package com.gitb.engine.processors;

import com.gitb.core.ErrorCode;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Assign;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.*;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by senan on 9/5/14.
 */
public class AssignProcessor implements IProcessor {

	private static final Pattern MAP_APPEND_EXPRESSION_PATTERN = Pattern.compile("(\\$?[a-zA-Z][a-zA-Z\\-_0-9]*(?:\\{(?:[\\$\\{\\}a-zA-Z\\-\\._0-9]*)\\})*)\\{(\\$?[a-zA-Z][a-zA-Z\\-\\._0-9]*)\\}");

    private final TestCaseScope scope;

    public AssignProcessor(TestCaseScope scope){
        this.scope = scope;
    }

    @Override
    public TestStepReportType process(Object object) throws Exception {
	    Assign assign = (Assign) object;
		String toExpression = assign.getTo();
		if (!toExpression.startsWith("$")) {
			toExpression = "$" + toExpression;
		}
	    Matcher matcher = MAP_APPEND_EXPRESSION_PATTERN.matcher(toExpression);
	    VariableResolver variableResolver = new VariableResolver(scope);
	    ExpressionHandler exprHandler = new ExpressionHandler(scope);

	    if (matcher.matches()) { // Assignment to a map key
		    String containerVariableExpression = matcher.group(1);
		    //The remaining part
		    String keyExpression = matcher.group(2);
			DataType lValue = variableResolver.resolveVariable(containerVariableExpression, true);
			if (lValue == null) {
				// New variable
				String variableName = stripExpressionStart(containerVariableExpression);
				if (VariableResolver.VARIABLE_PATTERN.matcher(variableName).matches()) {
					lValue = new MapType();
					scope.createVariable(variableName).setValue(lValue);
				} else {
					throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Cannot create a new variable based on the provided name ["+variableName+"]"));
				}
			} else if (!lValue.getType().equals(DataType.MAP_DATA_TYPE)) {
				throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Expression ["+containerVariableExpression+"] was expected to evaluate a map but evaluated a "+lValue.getType()));
			}
            DataType result = exprHandler.processExpression(assign, assign.getType());
		    String mapKey = keyExpression;
            if (mapKey.startsWith("$")) { //key is also a variable reference
                DataType keyValue = variableResolver.resolveVariable(mapKey);
                if (keyValue == null) {
					throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Map key expression ["+mapKey+"] could not resolve an existing variable"));
				}
				mapKey = (String)(keyValue.convertTo(StringType.STRING_DATA_TYPE).getValue());
            }
			((MapType)lValue).addItem(mapKey, result);
	    } else {
	    	// Regular assignment.
			String expectedReturnType = assign.getType();
			DataType lValue = variableResolver.resolveVariable(toExpression, true);
			if (lValue != null) {
				// Existing variable
				if (lValue instanceof ListType && assign.isAppend()) {
					expectedReturnType = ((ListType) lValue).getContainedType();
				} else {
					expectedReturnType = lValue.getType();
				}
				if (assign.getType() != null && !assign.getType().equals(expectedReturnType)) {
					throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Expected type does not match with the given type"));
				}
			}
			DataType result = null;
			if (StringUtils.isNotBlank(assign.getSource()) || StringUtils.isNotBlank(assign.getValue())) {
				result = exprHandler.processExpression(assign, expectedReturnType);
				if (result == null) {
					throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "The provided assign expression (source ["+StringUtils.defaultString(assign.getSource())+"] expression ["+StringUtils.defaultString(assign.getValue())+"]) could not evaluate a result"));
				}
				expectedReturnType = result.getType();
			}
			if (lValue == null) {
				// New variable
				if (assign.isAppend()) {
					// This can only be an array.
					lValue = new ListType(expectedReturnType);
				} else {
					// Match the type of the result.
					if (expectedReturnType != null) {
						lValue = DataTypeFactory.getInstance().create(expectedReturnType);
					}
				}
				if (lValue != null) {
					String variableName = stripExpressionStart(toExpression);
					if (VariableResolver.VARIABLE_PATTERN.matcher(variableName).matches()) {
						scope.createVariable(variableName).setValue(lValue);
					} else {
						throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Cannot create a new variable based on the provided name ["+variableName+"]"));
					}
				}
			}
			if (lValue != null) {
				if (lValue instanceof ListType) {
					if (assign.isAppend()) {
						String containedType = ((ListType) lValue).getContainedType();
						if (result != null) {
							if (containedType != null) {
								((ListType) lValue).append(result.convertTo(containedType));
							} else {
								((ListType) lValue).append(result);
							}
						} else {
							// We are told to append so we will add an item to the list.
							if (containedType == null) {
								throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Unable to append an item to list for expression [" + toExpression + "] becuase it was not possible to determine its contained type"));
							} else {
								((ListType) lValue).append(DataTypeFactory.getInstance().create(containedType));
							}
						}
					} else {
						if (result != null) {
							// The result should be a list - this is a direct assignment,
							lValue.copyFrom(result, DataType.LIST_DATA_TYPE);
						}
					}
				} else {
					// Normal assignment - convert to make sure the types match and then set the value (lValue is updated via reference).
					if (result != null) {
						lValue.copyFrom(result);
					}
				}
			}
	    }
        return null;
    }

	private String stripExpressionStart(String expression) {
		String resultingExpression = expression;
		if (expression.startsWith("$")) {
			resultingExpression = resultingExpression.substring(1);
		}
		return resultingExpression;
	}
}
