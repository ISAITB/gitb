package com.gitb.engine.processors;

import com.gitb.core.ErrorCode;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Assign;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.types.ListType;
import com.gitb.types.MapType;
import com.gitb.utils.ErrorUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by senan on 9/5/14.
 */
public class AssignProcessor implements IProcessor {

	private static final Pattern MAP_APPEND_EXPRESSION_PATTERN = Pattern.compile("(\\$[a-zA-Z][a-zA-Z\\-_0-9]*)\\{(\\$?[a-zA-Z][a-zA-Z\\-\\._0-9]*)\\}");

    private final TestCaseScope scope;

    public AssignProcessor(TestCaseScope scope){
        this.scope = scope;
    }

    @Override
    public TestStepReportType process(Object object) throws Exception {

	    Assign assign = (Assign) object;

	    Matcher matcher = MAP_APPEND_EXPRESSION_PATTERN.matcher(assign.getTo());

	    VariableResolver variableResolver = new VariableResolver(scope);
	    ExpressionHandler exprHandler = new ExpressionHandler(scope);

	    if(matcher.matches()) { // should be assign to map key
		    String containerVariableName = matcher.group(1);
		    //The remaining part
		    String keyExpression = matcher.group(2);

		    DataType lValue = variableResolver.resolveVariable(containerVariableName);

		    if(!lValue.getType().equals(DataType.MAP_DATA_TYPE)) {
			    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "To expression should be a map type"));
		    }

            DataType result = exprHandler.processExpression(assign, assign.getType());

            if(keyExpression.startsWith("$")) { //key is also a variable reference
                DataType keyValue = variableResolver.resolveVariable(keyExpression);
                if(!keyValue.getType().equals(DataType.STRING_DATA_TYPE)) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Map type key should be a string"));
                }

                if(assign.getType() == null) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Type parameter for the assign operation is necessary"));
                }

                ((MapType)lValue).addItem((String) keyValue.getValue(), result);
            } else {
                ((MapType)lValue).addItem(keyExpression, result);
            }
	    } else { // regular assign expression
		    DataType lValue = variableResolver.resolveVariable(assign.getTo());
		    //Expected return type for the expression is normally the type of lef value
		    String expectedReturnType = lValue.getType();
		    //If the lValue type is list and append is indicated than expected return type is child type of list
		    if(lValue instanceof ListType && assign.isAppend())
			    expectedReturnType = ((ListType) lValue).getContainedType();

		    if(assign.getType() != null && !assign.getType().equals(expectedReturnType)) {
			    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Expected type does not match with the given type"));
		    }

		    DataType result = exprHandler.processExpression(assign, expectedReturnType);
		    if (result == null) {
				throw new IllegalStateException("Assigned type was null");
			} else {
				if (lValue instanceof ListType) {
					if (assign.isAppend()) {
						((ListType) lValue).append(result);
					} else {
						// The result should be a list - this is a direct assignment,
						lValue.setValue(result);
					}
				} else {
					//Normal Assignment
					lValue.setValue(result.getValue());
				}
			}
	    }
        return null;
    }
}
