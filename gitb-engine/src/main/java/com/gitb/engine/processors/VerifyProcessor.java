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

package com.gitb.engine.processors;

import com.gitb.common.AliasManager;
import com.gitb.core.*;
import com.gitb.engine.ModuleManager;
import com.gitb.engine.SessionManager;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.HandlerUtils;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.engine.validation.handlers.common.AbstractValidator;
import com.gitb.engine.validation.handlers.xmlunit.XmlMatchValidator;
import com.gitb.engine.validation.handlers.xpath.XPathValidator;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.remote.ClientConfiguration;
import com.gitb.remote.HandlerTimeoutException;
import com.gitb.remote.validation.RemoteValidationModuleClient;
import com.gitb.tdl.Binding;
import com.gitb.tdl.ErrorLevel;
import com.gitb.tdl.Verify;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.*;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;
import com.gitb.validation.IValidationHandler;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static com.gitb.engine.utils.TestCaseUtils.postProcessReport;
import static com.gitb.engine.utils.TestCaseUtils.resolveReportErrorLevel;

/**
 * Created by senan on 9/12/14.
 */
public class VerifyProcessor implements IProcessor {

	private final TestCaseScope scope;

	public VerifyProcessor(TestCaseScope scope) {
		this.scope = scope;
	}

	@Override
	public TestStepReportType process(Object object) {
		Verify verify = (Verify) object;
		//Get the Validator Module from its name

		IValidationHandler validator;

		String handlerIdentifier = verify.getHandler();
		VariableResolver resolver = new VariableResolver(scope);
		if (VariableResolver.isVariableReference(handlerIdentifier)) {
			handlerIdentifier = resolver.resolveVariableAsString(handlerIdentifier).toString();
		}
		if (verify.getConfig() != null) {
			for (Configuration config: verify.getConfig()) {
				if (VariableResolver.isVariableReference(config.getValue())) {
					config.setValue(resolver.resolveVariableAsString(config.getValue()).toString());
				}
			}
		}

		if (isURL(handlerIdentifier)) {
			validator = getRemoteValidator(handlerIdentifier,
					TestCaseUtils.getStepProperties(verify.getProperty(), resolver),
					scope.getContext().getSessionId(),
					HandlerUtils.getHandlerTimeout(verify.getHandlerTimeout(), resolver)
			);
		} else {
			// This is a local validator.
			handlerIdentifier = AliasManager.getInstance().resolveValidationHandler(handlerIdentifier);
			validator = ModuleManager.getInstance().getValidationHandler(handlerIdentifier);
		}
		if (validator == null) {
			throw new IllegalStateException("Validation handler for ["+handlerIdentifier+"] could not be resolved");
		}
		try {
			ExpressionHandler exprHandler = new ExpressionHandler(scope);
			TestModule validatorDefinition = validator.getModuleDefinition();

			//Construct the inputs
			Map<String, DataType> inputs = new HashMap<>();
			List<Binding> inputExpressions = verify.getInput();

			//If binding is expected in order
			if (BindingUtils.isNameBinding(inputExpressions)) {
				//Find expected inputs
				Map<String, TypedParameter> expectedParamsMap = constructExpectedParameterMap(validatorDefinition);
				//Evaluate each expression to supply the inputs
				for (Binding inputExpression : inputExpressions) {
					String inputName = AliasManager.getInstance().resolveValidationHandlerInput(handlerIdentifier, inputExpression.getName());
					TypedParameter expectedParam = expectedParamsMap.get(inputName);
					DataType result;
					if (expectedParam == null) {
						result = exprHandler.processExpression(inputExpression);
					} else {
						result = exprHandler.processExpression(inputExpression, expectedParam.getType());
					}
					//Add result to the input map
					inputs.put(inputName, result);
				}
			} else {
				List<TypedParameter> expectedParams = new ArrayList<>();
				if (validatorDefinition != null && validatorDefinition.getInputs() != null) {
					expectedParams.addAll(validatorDefinition.getInputs().getParam());
				}
				Iterator<TypedParameter> expectedParamsIterator = expectedParams.iterator();
				Iterator<Binding> inputExpressionsIterator = inputExpressions.iterator();
				while (expectedParamsIterator.hasNext() && inputExpressionsIterator.hasNext()) {
					TypedParameter expectedParam = expectedParamsIterator.next();
					Binding inputExpression = inputExpressionsIterator.next();
					DataType result = exprHandler.processExpression(inputExpression, expectedParam.getType());
					inputs.put(expectedParam.getName(), result);
				}
			}
			if (validatorDefinition != null && validatorDefinition.getInputs() != null) {
				failIfMissingRequiredParameter(inputs, validatorDefinition.getInputs().getParam());
			}

			// Add validator-specific inputs
			if (validator instanceof AbstractValidator) {
				inputs.put(AbstractValidator.TEST_CASE_ID_INPUT, new StringType(scope.getContext().getTestCase().getId()));
				inputs.put(HandlerUtils.SESSION_INPUT, new StringType(scope.getContext().getSessionId()));
				if (validator instanceof XPathValidator || validator instanceof XmlMatchValidator) {
					inputs.put(HandlerUtils.NAMESPACE_MAP_INPUT, MapType.fromMap(scope.getNamespaceDefinitions()));
				}
			}

			// Validate content with given configurations and inputs; and return the report
			TestStepReportType report = validator.validate(verify.getConfig(), inputs, verify.getId());
			ErrorLevel errorLevel = resolveReportErrorLevel(verify.getLevel(), scope.getContext().getSessionId(), resolver);

			// Post process the step's report (invert logic, error to warning switch, counter creation)
			postProcessReport(verify.isInvert(), errorLevel, report);

			// Record the step's result as a Boolean flag bound to its ID
			if(verify.getId() != null && !verify.getId().isEmpty()) {
				boolean result = report.getResult().equals(TestResultType.SUCCESS) || report.getResult().equals(TestResultType.WARNING);

				if(scope.getVariable(verify.getId()).isDefined()) {
					scope.getVariable(verify.getId()).setValue(new BooleanType(result));
				} else {
					scope.createVariable(verify.getId()).setValue(new BooleanType(result));
				}
			}
			if ((report instanceof TAR) && ((TAR)report).getContext() != null) {
				// Record the report's context if specified to do so.
				if (verify.getOutput() != null && !verify.getOutput().isBlank()) {
					String outputVariable = verify.getOutput().trim();
					var contextData = DataTypeFactory.getInstance().create(((TAR)report).getContext(), AnyContent::isForContext);
					if (contextData != null) {
						scope.createVariable(outputVariable).setValue(contextData);
					}
				}
				// Remove from the report the context items not meant for display (do this last as this changes the context itself).
				((TAR) report).setContext(DataTypeFactory.getInstance().applyFilter(((TAR) report).getContext(), AnyContent::isForDisplay));
			}
			HandlerUtils.recordHandlerTimeout(verify.getHandlerTimeoutFlag(), resolver, scope, false);
			return report;
		} catch (HandlerTimeoutException e) {
			HandlerUtils.recordHandlerTimeout(verify.getHandlerTimeoutFlag(), resolver, scope, true);
			throw e;
		}
	}


	private boolean isURL(String handler) {
		try {
			new URI(handler).toURL();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private IValidationHandler getRemoteValidator(String handler, Properties connectionProperties, String sessionId, Long handlerTimeout) {
		try {
			return new RemoteValidationModuleClient(
                    new URI(handler).toURL(),
                    connectionProperties,
                    sessionId,
                    SessionManager.getInstance().getContext(sessionId).getTestCaseIdentifier(),
                    new ClientConfiguration(handlerTimeout)
            );
		} catch (MalformedURLException e) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote validation module found with an malformed URL ["+handler+"]"), e);
		} catch (URISyntaxException e) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote validation module found with an invalid URI syntax ["+handler+"]"), e);
		}
	}

	private HashMap<String, TypedParameter> constructExpectedParameterMap(TestModule moduleDefinition) {
		HashMap<String, TypedParameter> expectedParamsMap = new HashMap<>();
		if (moduleDefinition != null && moduleDefinition.getInputs() != null) {
			for (TypedParameter expectedParam : moduleDefinition.getInputs().getParam()) {
				expectedParamsMap.put(expectedParam.getName(), expectedParam);
			}
		}
		return expectedParamsMap;
	}

	private void failIfMissingRequiredParameter(Map<String, DataType> inputs, List<TypedParameter> expectedParams) {
		for (TypedParameter expectedParam : expectedParams) {
			if (expectedParam.getUse().equals(UsageEnumeration.R) && !inputs.containsKey(expectedParam.getName())) {
				throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Missing input parameter [" + expectedParam.getName() + "]"));
			}
		}
	}
}
