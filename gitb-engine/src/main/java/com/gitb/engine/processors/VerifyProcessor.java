package com.gitb.engine.processors;

import com.gitb.engine.ModuleManager;
import com.gitb.core.*;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.remote.validation.RemoteValidationModuleClient;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Binding;
import com.gitb.tdl.ErrorLevel;
import com.gitb.tdl.Verify;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.*;
import com.gitb.types.*;
import com.gitb.utils.BindingUtils;
import com.gitb.utils.ErrorUtils;
import com.gitb.validation.IValidationHandler;
import com.gitb.engine.validation.handlers.common.AbstractValidator;
import com.gitb.engine.validation.handlers.xpath.XPathValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by senan on 9/12/14.
 */
public class VerifyProcessor implements IProcessor {

	private final static Logger logger = LoggerFactory.getLogger(VerifyProcessor.class);
	private final TestCaseScope scope;
	protected ObjectFactory objectFactory = new ObjectFactory();

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
			validator = getRemoteValidator(handlerIdentifier, TestCaseUtils.getStepProperties(verify.getProperty(), resolver), scope.getContext().getSessionId());
		} else {
			// This is a local validator.
			validator = ModuleManager.getInstance().getValidationHandler(handlerIdentifier);
		}
		if (validator == null) {
			throw new IllegalStateException("Validation handler for ["+handlerIdentifier+"] could not be resolved");
		}
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
				TypedParameter expectedParam = expectedParamsMap.get(inputExpression.getName());
				DataType result;
				if (expectedParam == null) {
					result = exprHandler.processExpression(inputExpression);
				} else {
					result = exprHandler.processExpression(inputExpression, expectedParam.getType());
				}
				//Add result to the input map
				inputs.put(inputExpression.getName(), result);
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
			if (validator instanceof XPathValidator) {
				inputs.put(XPathValidator.NAMESPACE_MAP_INPUT, MapType.fromMap(scope.getNamespaceDefinitions()));
			}
		}

		// Validate content with given configurations and inputs; and return the report
		TestStepReportType report = validator.validate(verify.getConfig(), inputs, verify.getId());

		var errorLevel = ErrorLevel.ERROR;
		if (VariableResolver.isVariableReference(verify.getLevel())) {
			var resolvedErrorLevel = resolver.resolveVariableAsString(verify.getLevel());
			try {
				errorLevel = ErrorLevel.valueOf((String) resolvedErrorLevel.getValue());
			} catch (NullPointerException e) {
				logger.warn(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), String.format("Severity level for verify step could not be determined using expression [%s]. Using %s level instead.", verify.getLevel(), ErrorLevel.ERROR));
			} catch (IllegalArgumentException e) {
				logger.warn(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), String.format("Invalid severity level [%s] for verify step determined using expression [%s]. Using %s level instead.", errorLevel, verify.getLevel(), ErrorLevel.ERROR));
			}
		} else {
			errorLevel = ErrorLevel.valueOf(verify.getLevel());
		}

		// Processing if step is at warning level
		if (errorLevel == ErrorLevel.WARNING && report.getResult().equals(TestResultType.FAILURE)) {
			// Failed report but with step at warning level - mark as success and convert reported error items to warnings
			convertErrorItemsToWarnings(report);
		}
		if (report instanceof TAR) {
			completeReportCounters((TAR)report);
		}
		// Record the step's result as a Boolean flag bound to its ID
        if(verify.getId() != null && verify.getId().length() > 0) {
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
		return report;
	}

	private void completeReportCounters(TAR report) {
		int errorCount = 0;
		int warningCount = 0;
		int infoCount = 0;
		TestAssertionGroupReportsType reportsType = report.getReports();
		if (reportsType != null) {
			for (JAXBElement<TestAssertionReportType> item : reportsType.getInfoOrWarningOrError()) {
				if (item.getValue() instanceof BAR) {
					if (item.getName().getLocalPart().equals("error")) {
						errorCount += 1;
					} else if (item.getName().getLocalPart().equals("warning")) {
						warningCount += 1;
					} else {
						infoCount += 1;
					}
				}
			}
		}
		if (report.getCounters() == null) {
			report.setCounters(new ValidationCounters());
		}
		if (report.getCounters().getNrOfErrors() == null) {
			report.getCounters().setNrOfErrors(BigInteger.valueOf(errorCount));
		}
		if (report.getCounters().getNrOfWarnings() == null) {
			report.getCounters().setNrOfWarnings(BigInteger.valueOf(warningCount));
		}
		if (report.getCounters().getNrOfAssertions() == null) {
			report.getCounters().setNrOfAssertions(BigInteger.valueOf(infoCount));
		}
	}

	private void convertErrorItemsToWarnings(TestStepReportType report) {
		report.setResult(TestResultType.WARNING);
		if (report instanceof TAR) {
			// Set errors to warnings in counters.
			ValidationCounters counters = ((TAR)report).getCounters();
			if (counters != null) {
				int errorCount = 0;
				if (counters.getNrOfErrors() != null) {
					errorCount = counters.getNrOfErrors().intValue();
				}
				int warningCount = 0;
				if (counters.getNrOfWarnings() != null) {
					warningCount = counters.getNrOfWarnings().intValue();
				}
				counters.setNrOfErrors(BigInteger.ZERO);
				counters.setNrOfWarnings(BigInteger.valueOf(errorCount + warningCount));
			}
			// Set errors to warnings in report items.
			TestAssertionGroupReportsType reportsType = ((TAR)report).getReports();
			if (reportsType != null) {
				List<JAXBElement<TestAssertionReportType>> newReports = new ArrayList<>(reportsType.getInfoOrWarningOrError().size());
				for (JAXBElement<TestAssertionReportType> item: reportsType.getInfoOrWarningOrError()) {
					if (item.getValue() instanceof BAR) {
						if (item.getName().getLocalPart().equals("error")) {
							newReports.add(objectFactory.createTestAssertionGroupReportsTypeWarning(item.getValue()));
						} else {
							newReports.add(item);
						}
					}
				}
				reportsType.getInfoOrWarningOrError().clear();
				reportsType.getInfoOrWarningOrError().addAll(newReports);
			}
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

	private IValidationHandler getRemoteValidator(String handler, Properties connectionProperties, String sessionId) {
		try {
			return new RemoteValidationModuleClient(new URI(handler).toURL(), connectionProperties, sessionId);
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
