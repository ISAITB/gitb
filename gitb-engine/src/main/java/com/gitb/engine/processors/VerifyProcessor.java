package com.gitb.engine.processors;

import com.gitb.ModuleManager;
import com.gitb.core.ErrorCode;
import com.gitb.core.TestModule;
import com.gitb.core.TypedParameter;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.engine.remote.validation.RemoteValidationModuleClient;
import com.gitb.tdl.Binding;
import com.gitb.tdl.Verify;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;
import com.gitb.validation.IValidationHandler;
import com.gitb.validation.common.AbstractValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

		IValidationHandler validator;

		String handlerIdentifier = verify.getHandler();
		VariableResolver resolver = new VariableResolver(scope);
		if (resolver.isVariableReference(handlerIdentifier)) {
			handlerIdentifier = resolver.resolveVariableAsString(handlerIdentifier).toString();
		}

		if (isURL(handlerIdentifier)) {
			validator = getRemoteValidator(handlerIdentifier, TestCaseUtils.getStepProperties(verify.getProperty(), resolver));
		} else {
			validator = ModuleManager.getInstance().getValidationHandler(handlerIdentifier);
			// This is a local validator.
			if (validator instanceof AbstractValidator) {
				((AbstractValidator)validator).setTestCaseId(scope.getContext().getTestCase().getId());
			}
		}
		if (validator == null) {
			throw new IllegalStateException("Validation handler for ["+handlerIdentifier+"] could not be resolved");
		}
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
					throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Invalid parameter for the handler [" + handlerIdentifier + "]"));
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

	private boolean isURL(String handler) {
		try {
			new URI(handler).toURL();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private IValidationHandler getRemoteValidator(String handler, Properties connectionProperties) {
		try {
			return new RemoteValidationModuleClient(new URI(handler).toURL(), connectionProperties);
		} catch (MalformedURLException e) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote validation module found with an malformed URL ["+handler+"]"), e);
		} catch (URISyntaxException e) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote validation module found with an invalid URI syntax ["+handler+"]"), e);
		}
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
