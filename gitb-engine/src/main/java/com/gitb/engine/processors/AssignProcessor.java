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
		boolean respectScopeIsolation = false;
		if (!toExpression.startsWith("$")) {
			toExpression = "$" + toExpression;
			// When defined without a "$" prefix this may lead to a new variable. In this case we must respect scope isolation (in case of scriptlets).
			respectScopeIsolation = true;
		}
	    Matcher matcher = MAP_APPEND_EXPRESSION_PATTERN.matcher(toExpression);
	    VariableResolver variableResolver = new VariableResolver(scope);
	    ExpressionHandler exprHandler = new ExpressionHandler(scope);

	    if (matcher.matches()) { // Assignment to a map key
		    String containerVariableExpression = matcher.group(1);
		    //The remaining part
		    String keyExpression = matcher.group(2);
			DataType lValue = variableResolver.resolveVariable(containerVariableExpression, true, respectScopeIsolation).orElse(null);
			if (lValue == null) {
				// New variable
				lValue = createIntermediateMaps(variableResolver, containerVariableExpression);
				if (lValue == null) {
					throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Cannot create a new variable based on the provided name ["+stripExpressionStart(containerVariableExpression)+"]"));
				}
			} else if (!lValue.getType().equals(DataType.MAP_DATA_TYPE)) {
				// Matched an existing variable that is not a map that will be replaced.
				lValue = new MapType();
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
			var existingValue = ((MapType)lValue).getItems().get(mapKey);
			if (existingValue == null) {
				if (assign.isAppend()) {
					// This is the first item of a list being assigned to a key in the map.
					var list = new ListType(result.getType());
					addToList(assign, list, result);
					((MapType)lValue).addItem(mapKey, list);
				} else {
					// Map the result to the map.
					addToMap(assign, (MapType) lValue, mapKey, result);
				}
			} else {
				if (assign.isAppend()) {
					if (DataType.isListType(existingValue.getType())) {
						// Appending to an existing list.
						String containedType = ((ListType)existingValue).getContainedType();
						if (containedType != null) {
							addToList(assign, (ListType) existingValue, result.convertTo(containedType));
						} else {
							addToList(assign, (ListType) existingValue, result);
						}
					} else {
						// Append to an existing non-list value - convert existing value to list.
						var list = new ListType(existingValue.getType());
						list.append(existingValue);
						addToList(assign, list, result.convertTo(existingValue.getType()));
					}
				} else {
					// Replace the key mapping of the map.
					addToMap(assign, (MapType) lValue, mapKey, result);
				}
			}
	    } else {
	    	// Regular assignment.
			String expectedReturnType = assign.getType();
			DataType lValue = variableResolver.resolveVariable(toExpression, true, respectScopeIsolation).orElse(null);
			if (lValue != null) {
				// Existing variable
				if (lValue instanceof ListType lValueList && assign.isAppend()) {
					expectedReturnType = lValueList.getContainedType();
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
						lValue = scope.createVariable(variableName).setValue(lValue);
					} else {
						throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Cannot create a new variable based on the provided name ["+variableName+"]"));
					}
				}
			}
			if (lValue != null) {
				if (lValue instanceof ListType lValueList) {
					if (assign.isAppend()) {
						String containedType = lValueList.getContainedType();
						if (result != null) {
							if (containedType != null) {
								addToList(assign, lValueList, result.convertTo(containedType));
							} else {
								addToList(assign, lValueList, result);
							}
						} else {
							// We are told to append so we will add an item to the list.
							if (containedType == null) {
								throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Unable to append an item to list for expression [" + toExpression + "] because it was not possible to determine its contained type"));
							} else {
								lValueList.append(DataTypeFactory.getInstance().create(containedType));
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

	private void addToList(Assign assign, ListType targetList, DataType value) {
		if (assign.isByValue()) {
			DataType newValue = DataTypeFactory.getInstance().create(value.getType());
			newValue.copyFrom(value);
			targetList.append(newValue);
		} else {
			targetList.append(value);
		}
	}

	private void addToMap(Assign assign, MapType targetMap, String key, DataType value) {
		if (assign.isByValue()) {
			DataType newValue = DataTypeFactory.getInstance().create(value.getType());
			newValue.copyFrom(value);
			targetMap.addItem(key, newValue);
		} else {
			targetMap.addItem(key, value);
		}
	}

	private DataType createIntermediateMaps(VariableResolver variableResolver, String expression) {
		DataType resultingMap = null;
		expression = addExpressionStart(expression);
		if (VariableResolver.isVariableReference(expression)) {
			resultingMap = variableResolver.resolveVariable(expression, true).orElse(null);
		}
		if (resultingMap == null) {
			expression = stripExpressionStart(expression);
			if (VariableResolver.VARIABLE_PATTERN.matcher(expression).matches()) {
				resultingMap = scope.createVariable(expression).setValue(new MapType());
			} else {
				Matcher matcher = MAP_APPEND_EXPRESSION_PATTERN.matcher(expression);
				if (matcher.matches()) {
					String containerVariableExpression = matcher.group(1);
					String keyExpression = matcher.group(2);
					resultingMap = createIntermediateMaps(variableResolver, containerVariableExpression);
					if (resultingMap instanceof MapType resultingMapType) {
						DataType childMap = new MapType();
						resultingMapType.addItem(keyExpression, childMap);
						resultingMap = childMap;
					} else {
						throw new IllegalStateException("Variable ["+containerVariableExpression+"] was expected to be of type [map] but was of type ["+(resultingMap == null?"null":resultingMap.getType())+"]");
					}
				}
			}
		}
		return resultingMap;
	}

	private String stripExpressionStart(String expression) {
		String resultingExpression = expression;
		if (expression.startsWith("$")) {
			resultingExpression = resultingExpression.substring(1);
		}
		return resultingExpression;
	}

	private String addExpressionStart(String expression) {
		String resultingExpression = expression;
		if (!expression.startsWith("$")) {
			resultingExpression = "$"+resultingExpression;
		}
		return resultingExpression;
	}

}
