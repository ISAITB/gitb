package com.gitb.engine.processors;

import com.gitb.core.ErrorCode;
import com.gitb.core.TestModule;
import com.gitb.core.TypedParameter;
import com.gitb.core.UsageEnumeration;
import com.gitb.ModuleManager;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Binding;
import com.gitb.tdl.Verify;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;
import com.gitb.validation.IValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by senan on 9/12/14.
 */
public class VerifyProcessor implements IProcessor {
	private static Logger logger = LoggerFactory.getLogger(VerifyProcessor.class);

	private final TestCaseScope scope;

	public VerifyProcessor(TestCaseScope scope) {
		this.scope = scope;
	}

	@Override
	public TestStepReportType process(Object object) throws Exception {
		Verify verify = (Verify) object;
		//Get the Validator Module from its name
		IValidationHandler validator = ModuleManager.getInstance().getValidationHandler(verify.getHandler());
		ExpressionHandler exprHandler = new ExpressionHandler(scope);
		TestModule validatorDefinition = validator.getModuleDefinition();

		//Construct the inputs
		Map<String, DataType> inputs = new HashMap<String, DataType>();
		List<Binding> inputExpressions = verify.getInput();

		//If binding is expected in order
		if (BindingUtils.isNameBinding(inputExpressions)) {
			//Find expected inputs
			HashMap<String, TypedParameter> expectedParamsMap = constructExpectedParameterMap(validatorDefinition);
			//Evaluate each expression to supply the inputs
			for (Binding inputExpression : inputExpressions) {
				TypedParameter expectedParam = expectedParamsMap.get(inputExpression.getName());
				if (expectedParam == null) {
					throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Invalid parameter for the handler [" + verify.getHandler() + "]"));
				}
				//Evaluate Expression
				DataType result = exprHandler.processExpression(inputExpression, expectedParam.getType());
				//Add result to the input map
				inputs.put(inputExpression.getName(), result);
			}
		} else {
			List<TypedParameter> expectedParams = validatorDefinition.getInputs().getParam();
			for (int i = 0; i < expectedParams.size() && i < inputExpressions.size(); i++) {
				Binding inputExpression = inputExpressions.get(i);
				TypedParameter expectedParam = expectedParams.get(i);
				//Evaluate Expression
				DataType result = exprHandler.processExpression(inputExpression, expectedParam.getType());
				//Add result to the input map
				inputs.put(expectedParam.getName(), result);
			}
		}

		if (!checkIfExpectedParamsAreSupplied(inputs, validatorDefinition.getInputs().getParam())) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Some required parameters are not supplied for verification!"));
		}

		// Validate content with given configurations and inputs; and return the report
		TestStepReportType report = validator.validate(verify.getConfig(), inputs);
        if(verify.getId() != null && verify.getId().length() > 0) {
            boolean result = report.getResult().equals(TestResultType.SUCCESS);

            if(scope.getVariable(verify.getId()).isDefined()) {
                scope.getVariable(verify.getId()).setValue(new BooleanType(result));
            } else {
                scope.createVariable(verify.getId()).setValue(new BooleanType(result));
            }
        }
		return report;
	}

	private HashMap<String, TypedParameter> constructExpectedParameterMap(TestModule moduleDefinition) {
		HashMap<String, TypedParameter> expectedParamsMap = new HashMap<String, TypedParameter>();
		List<TypedParameter> expectedParams = moduleDefinition.getInputs().getParam();
		for (TypedParameter expectedParam : expectedParams) {
			expectedParamsMap.put(expectedParam.getName(), expectedParam);
		}
		return expectedParamsMap;
	}

	private boolean checkIfExpectedParamsAreSupplied(Map<String, DataType> inputs, List<TypedParameter> expectedParams) {
		for (TypedParameter expectedParam : expectedParams) {
			if (expectedParam.getUse().equals(UsageEnumeration.R) && !inputs.containsKey(expectedParam.getName()))
				return false;
		}
		return true;
	}
}
