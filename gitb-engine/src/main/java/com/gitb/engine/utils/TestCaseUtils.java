package com.gitb.engine.utils;

import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.core.ErrorCode;
import com.gitb.core.StepStatus;
import com.gitb.engine.ModuleManager;
import com.gitb.engine.PropertyConstants;
import com.gitb.engine.expr.StaticExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.remote.RemoteCallContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tdl.Process;
import com.gitb.tdl.*;
import com.gitb.tr.*;
import com.gitb.tr.ObjectFactory;
import com.gitb.types.*;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.XMLDateTimeUtils;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.*;
import java.util.function.Supplier;

/**
 * Created by senan on 10/13/14.
 */
public class TestCaseUtils {

    public static final String TEST_ENGINE_VERSION;
    private static final ObjectFactory OBJECT_FACTORY_TR = new ObjectFactory();
    private static final Logger LOG = LoggerFactory.getLogger(TestCaseUtils.class);

    static {
        TEST_ENGINE_VERSION = getTestEngineVersion();
    }

	// TODO add the test case construct classes to report their statuses (COMPLETED, ERROR, etc.)
	private static final Class<?>[] TEST_CONSTRUCTS_TO_REPORT = {
        com.gitb.tdl.MessagingStep.class, Verify.class, IfStep.class, RepeatUntilStep.class,
		ForEachStep.class, WhileStep.class, com.gitb.tdl.FlowStep.class, Process.class,
		CallStep.class, com.gitb.tdl.ExitStep.class, Group.class, UserInteraction.class
	};

    public static Properties getStepProperties(List<Configuration> properties, VariableResolver resolver) {
        Properties result = new Properties();
        if (properties != null && !properties.isEmpty()) {
            for (Configuration config: properties) {
                String value = config.getValue();
                if (VariableResolver.isVariableReference(value)) {
                    value = resolver.resolveVariableAsString(value).toString();
                }
                result.setProperty(config.getName(), value);
            }
        }
        return result;
    }

    public static void prepareRemoteServiceLookup(Properties stepProperties) {
        if (stepProperties != null && !StringUtils.isBlank(stepProperties.getProperty(PropertyConstants.AUTH_BASIC_USERNAME))) {
            /*
            The configuration specifies that we have basic authentication. To allow this to go through even if
            the WSDL is protected we use a thread-safe (via ThreadLocal) authenticator. This is because the
            new MessagingServiceClient(getServiceURL()) call results in a call to the WSDL (that needs authentication).
             */
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    Properties callProperties = RemoteCallContext.getCallProperties();
                    String username = callProperties.getProperty(PropertyConstants.AUTH_BASIC_USERNAME);
                    String password = callProperties.getProperty(PropertyConstants.AUTH_BASIC_PASSWORD);
                    return new PasswordAuthentication(
                            username,
                            password.toCharArray());
                }
            });
        }
    }

	public static boolean shouldBeReported(Class<?> stepClass) {
		for(Class<?> c : TEST_CONSTRUCTS_TO_REPORT) {
            Class<?> current = stepClass;
            while(current != null) {
                if(current.equals(c)) {
                    return true;
                }
                current = current.getSuperclass();
			}
		}

		return false;
	}

	private static Scriptlet lookupExternalScriptlet(String from, String testCaseId, String scriptletPath) {
        ITestCaseRepository repository = ModuleManager.getInstance().getTestCaseRepository();
        return repository.getScriptlet(from, testCaseId, scriptletPath);
    }

    public static Scriptlet lookupScriptlet(String from, String path, com.gitb.tdl.TestCase testCase, boolean required) {
        Scriptlet foundScriptlet = null;
        if (StringUtils.isBlank(path)) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "No scriptlet path was provided."));
        }
        String scriptletPath = path.trim();
        if (StringUtils.isNotBlank(from)) {
            // Lookup from a specific test suite.
            foundScriptlet = lookupExternalScriptlet(from.trim(), testCase.getId(), path);
        } else {
            // Find scriptlet in the test case (if it is inline).
            if (testCase.getScriptlets() != null) {
                for (Scriptlet scriptlet: testCase.getScriptlets().getScriptlet()) {
                    if (scriptlet.getId().equals(scriptletPath)) {
                        foundScriptlet = scriptlet;
                        break;
                    }
                }
            }
            if (foundScriptlet == null) {
                // Look also in the current test suite.
                foundScriptlet = lookupExternalScriptlet(null, testCase.getId(), path);
            }
        }
        if (foundScriptlet == null) {
            if (required) {
                if (from == null) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Scriptlet definition ["+ scriptletPath+"] not found."));
                } else {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Scriptlet definition from ["+StringUtils.defaultString(from, "")+"] path ["+ scriptletPath+"] not found."));
                }
            }
        }
        return foundScriptlet;
    }

    public static void applyStopOnErrorSemantics(TestConstruct step, Boolean parentStopOnErrorSetting) {
        if (step != null) {
            boolean parentStopOnErrorSettingToUse = parentStopOnErrorSetting != null && parentStopOnErrorSetting;
            if (step.isStopOnError() == null) {
                // Inherit parent setting.
                step.setStopOnError(parentStopOnErrorSettingToUse);
            }
            if (step instanceof Sequence) {
                for (Object childStep: ((Sequence)step).getSteps()) {
                    if (childStep instanceof TestConstruct) {
                        applyStopOnErrorSemantics((TestConstruct)childStep, step.isStopOnError());
                    }
                }
            } else {
                // Cover also the steps that have internal sequences.
                if (step instanceof IfStep) {
                    applyStopOnErrorSemantics(((IfStep) step).getThen(), step.isStopOnError());
                    applyStopOnErrorSemantics(((IfStep) step).getElse(), step.isStopOnError());
                } else if (step instanceof WhileStep) {
                    applyStopOnErrorSemantics(((WhileStep) step).getDo(), step.isStopOnError());
                } else if (step instanceof ForEachStep) {
                    applyStopOnErrorSemantics(((ForEachStep) step).getDo(), step.isStopOnError());
                } else if (step instanceof RepeatUntilStep) {
                    applyStopOnErrorSemantics(((RepeatUntilStep) step).getDo(), step.isStopOnError());
                } else if (step instanceof FlowStep) {
                    if (((FlowStep) step).getThread() != null) {
                        for (Sequence thread: ((FlowStep) step).getThread()) {
                            applyStopOnErrorSemantics(thread, step.isStopOnError());
                        }
                    }
                }
            }
        }
    }

    private static String qualifiedStepIdForStatusMaps(String stepId, TestCaseScope scope) {
        Objects.requireNonNull(stepId);
        String id = stepId;
        if (scope != null && StringUtils.isNotBlank(scope.getQualifiedScopeId())) {
            id = scope.getQualifiedScopeId()+"_"+stepId;
        }
        return id;
    }

    public static void updateStepStatusMaps(MapType stepSuccessMap, MapType stepStatusMap, TestConstruct step, TestCaseScope scope, StepStatus status) {
        if (step != null && StringUtils.isNotBlank(step.getId())) {
            // We only record status for a step with an identifier.
            var stepId = step.getId();
            var qualifiedStepId = qualifiedStepIdForStatusMaps(stepId, scope);
            var successValue = new BooleanType(status == StepStatus.COMPLETED || status == StepStatus.WARNING);
            var statusValue = new StringType((status == null)?"":status.toString());
            if (StringUtils.isNotBlank(qualifiedStepId)) {
                // Record the status using the qualified step ID.
                stepSuccessMap.addItem(qualifiedStepId, successValue);
                stepStatusMap.addItem(qualifiedStepId, statusValue);
            }
            if (!Objects.equals(step.getId(), qualifiedStepId)) {
                // Also record using the basic ID (if already existing one value - the latest - is maintained).
                stepSuccessMap.addItem(stepId, successValue);
                stepStatusMap.addItem(stepId, statusValue);
            }
        }
    }

    public static void initialiseStepStatusMaps(MapType stepSuccessMap, MapType stepStatusMap, TestConstruct step, TestCaseScope scope) {
        if (step != null) {
            // Initialise for the step itself.
            updateStepStatusMaps(stepSuccessMap, stepStatusMap, step, scope, null);
            if (step instanceof Sequence) {
                // Initialise for children.
                for (Object childStep: ((Sequence)step).getSteps()) {
                    if (childStep instanceof TestConstruct) {
                        initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, (TestConstruct)childStep, scope);
                    }
                }
            } else {
                // Initialise for other steps with internal sequences.
                if (step instanceof IfStep) {
                    initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, ((IfStep) step).getThen(), scope);
                    initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, ((IfStep) step).getElse(), scope);
                } else if (step instanceof WhileStep) {
                    initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, ((WhileStep) step).getDo(), scope);
                } else if (step instanceof ForEachStep) {
                    initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, ((ForEachStep) step).getDo(), scope);
                } else if (step instanceof RepeatUntilStep) {
                    initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, ((RepeatUntilStep) step).getDo(), scope);
                } else if (step instanceof FlowStep) {
                    if (((FlowStep) step).getThread() != null) {
                        for (Sequence thread: ((FlowStep) step).getThread()) {
                            initialiseStepStatusMaps(stepSuccessMap, stepStatusMap, thread, scope);
                        }
                    }
                }
            }
        }
    }

    private static String extractStepName(Object step) {
        String name = step.getClass().getSimpleName();
        if (name.endsWith("Step")) {
            return name.substring(0, name.indexOf("Step"));
        } else {
            return name;
        }
    }

    public static String extractStepDescription(Object step, TestCaseScope scope) {
        String description = null;
        if (step instanceof TestStep) {
            description = ((TestStep) step).getDesc();
            if (description != null && description.isBlank()) {
                description = null;
            }
        }
        if (description == null) {
            description = extractStepName(step);
        } else {
            if (VariableResolver.isVariableReference(description)) {
                description = (String) new VariableResolver(scope).resolveVariable(description).convertTo(DataType.STRING_DATA_TYPE).getValue();
            }
        }
        return description;
    }

    public static <T> T fixedOrVariableValue(String originalValue, Class<T> variableClass, LinkedList<Pair<CallStep, Scriptlet>> scriptletStepStack) {
        if (originalValue != null) {
            String dataType;
            Supplier<T> nonVariableValueFn;
            if (String.class.equals(variableClass)) {
                dataType = DataType.STRING_DATA_TYPE;
                nonVariableValueFn = () -> variableClass.cast(originalValue);
            } else if (Boolean.class.equals(variableClass)) {
                dataType = DataType.BOOLEAN_DATA_TYPE;
                nonVariableValueFn = () -> variableClass.cast(Boolean.valueOf(originalValue));
            } else {
                throw new IllegalArgumentException("Unsupported variable class ["+variableClass+"]");
            }
            if (!scriptletStepStack.isEmpty() && VariableResolver.isVariableReference(originalValue)) {
                // The description may be set dynamically from the call inputs.
                return TestCaseUtils.getConstantCallInput(
                        VariableResolver.extractVariableNameFromExpression(originalValue).getLeft(),
                        variableClass,
                        dataType, scriptletStepStack
                ).orElseGet(nonVariableValueFn);
            }
            return nonVariableValueFn.get();
        } else {
            return null;
        }
    }

    private static <T> Optional<T> getConstantCallInput(String inputName, Class<T> constantClass, String constantDataType, LinkedList<Pair<CallStep, Scriptlet>> scriptletStepStack) {
        var originalInputName = inputName;
        DataType dataToUse = null;
        var iterator = scriptletStepStack.descendingIterator();
        while (iterator.hasNext()) {
            var callData = iterator.next();
            var inputToLookFor = inputName;
            var matchedInput = callData.getLeft().getInput().stream().filter(input -> inputToLookFor.equals(input.getName())).findFirst();
            if (matchedInput.isPresent()) {
                // We found a matching input.
                var inputValueExpression = matchedInput.get().getValue();
                if (VariableResolver.isVariableReference(inputValueExpression)) {
                    // The input's value is itself a variable reference.
                    inputName = VariableResolver.extractVariableNameFromExpression(inputValueExpression).getLeft();
                    continue;
                }
                dataToUse = new StaticExpressionHandler().processExpression(matchedInput.get(), constantDataType);
            }
            break;
        }
        if (dataToUse == null && !scriptletStepStack.isEmpty()) {
            // No input found. Look also at variable default values.
            var scriptlet = scriptletStepStack.getLast().getRight();
            if (scriptlet.getParams() != null) {
                var matchedVariableValue = scriptlet.getParams().getVar().stream().filter(variable -> originalInputName.equals(variable.getName()) && !variable.getValue().isEmpty()).findFirst();
                if (matchedVariableValue.isPresent()) {
                    // The parameter defines a default value.
                    dataToUse = DataTypeFactory.getInstance().create(matchedVariableValue.get());
                }
            }
        }
        if (dataToUse != null) {
            var valueToUse = dataToUse.convertTo(constantDataType).getValue();
            if (valueToUse != null && constantClass.equals(valueToUse.getClass())) {
                return Optional.of(constantClass.cast(valueToUse));
            }
        }
        return Optional.empty();
    }

    public static TAR mergeReports(List<TAR> reports) {
        return mergeReports(reports.toArray(new TAR[0]));
    }

    public static TAR mergeReports(TAR[] reports) {
        TAR mergedReport = reports[0];
        if (reports.length > 1) {
            for(int i = 1; i < reports.length; ++i) {
                TAR report = reports[i];
                if (report != null) {
                    if (report.getCounters() != null) {
                        if (mergedReport.getCounters() == null) {
                            mergedReport.setCounters(new ValidationCounters());
                            mergedReport.getCounters().setNrOfAssertions(BigInteger.ZERO);
                            mergedReport.getCounters().setNrOfWarnings(BigInteger.ZERO);
                            mergedReport.getCounters().setNrOfErrors(BigInteger.ZERO);
                        }

                        if (report.getCounters().getNrOfAssertions() != null) {
                            mergedReport.getCounters().setNrOfAssertions(mergedReport.getCounters().getNrOfAssertions().add(report.getCounters().getNrOfAssertions()));
                        }

                        if (report.getCounters().getNrOfWarnings() != null) {
                            mergedReport.getCounters().setNrOfWarnings(mergedReport.getCounters().getNrOfWarnings().add(report.getCounters().getNrOfWarnings()));
                        }

                        if (report.getCounters().getNrOfErrors() != null) {
                            mergedReport.getCounters().setNrOfErrors(mergedReport.getCounters().getNrOfErrors().add(report.getCounters().getNrOfErrors()));
                        }
                    }

                    if (report.getReports() != null) {
                        if (mergedReport.getReports() == null) {
                            mergedReport.setReports(new TestAssertionGroupReportsType());
                        }

                        mergedReport.getReports().getInfoOrWarningOrError().addAll(report.getReports().getInfoOrWarningOrError());
                    }

                    if (mergedReport.getResult() == null) {
                        mergedReport.setResult(TestResultType.UNDEFINED);
                    }

                    if (report.getResult() != null && report.getResult() != TestResultType.UNDEFINED && (mergedReport.getResult() == TestResultType.UNDEFINED || mergedReport.getResult() == TestResultType.SUCCESS && report.getResult() != TestResultType.SUCCESS || mergedReport.getResult() == TestResultType.WARNING && report.getResult() == TestResultType.FAILURE)) {
                        mergedReport.setResult(report.getResult());
                    }

                    if (report.getContext() != null) {
                        if (mergedReport.getContext() == null) {
                            mergedReport.setContext(report.getContext());
                        } else if (report.getContext().getItem() != null) {
                            for (AnyContent item : report.getContext().getItem()) {
                                if (item.getName() != null) {
                                    List<AnyContent> matchedInputs = getInputFor(mergedReport.getContext().getItem(), item.getName());
                                    if (matchedInputs.isEmpty()) {
                                        mergedReport.getContext().getItem().add(item);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return mergedReport;
    }

    public static List<AnyContent> getInputFor(List<AnyContent> inputsToConsider, String name) {
        List<AnyContent> inputs = new ArrayList<>();
        if (inputsToConsider != null) {
            for (AnyContent anInput : inputsToConsider) {
                if (name.equals(anInput.getName())) {
                    inputs.add(anInput);
                }
            }
        }
        return inputs;
    }

    public static TAR createEmptyReport() {
        var report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        try {
            report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Exception while creating XMLGregorianCalendar", e);
        }
        return report;
    }

    public static ErrorLevel resolveReportErrorLevel(String stepLevel, String sessionId, VariableResolver resolver) {
        var errorLevel = ErrorLevel.ERROR;
        if (VariableResolver.isVariableReference(stepLevel)) {
            var resolvedErrorLevel = resolver.resolveVariableAsString(stepLevel);
            try {
                errorLevel = ErrorLevel.valueOf((String) resolvedErrorLevel.getValue());
            } catch (NullPointerException e) {
                LOG.warn(MarkerFactory.getDetachedMarker(sessionId), String.format("Severity level for step could not be determined using expression [%s]. Using %s level instead.", stepLevel, ErrorLevel.ERROR));
            } catch (IllegalArgumentException e) {
                LOG.warn(MarkerFactory.getDetachedMarker(sessionId), String.format("Invalid severity level [%s] for step determined using expression [%s]. Using %s level instead.", errorLevel, stepLevel, ErrorLevel.ERROR));
            }
        } else {
            errorLevel = ErrorLevel.valueOf(stepLevel);
        }
        return errorLevel;
    }

    public static void postProcessReport(boolean invert, ErrorLevel errorLevel, TestStepReportType report) {
        if (report != null) {
            // Invert the result if required to do so.
            if (invert) {
                if (report.getResult().equals(TestResultType.FAILURE)) {
                    report.setResult(TestResultType.SUCCESS);
                } else if (report.getResult().equals(TestResultType.SUCCESS)) {
                    report.setResult(TestResultType.FAILURE);
                }
            }
            // Transform errors to warnings if the step is at warning level.
            if (errorLevel == ErrorLevel.WARNING && report.getResult().equals(TestResultType.FAILURE)) {
                // Failed report but with step at warning level - mark as success and convert reported error items to warnings
                convertErrorItemsToWarnings(report);
            }
            // Complete the report's counters.
            if (report instanceof TAR) {
                completeReportCounters((TAR)report);
            }
        }
    }

    public static void convertErrorItemsToWarnings(TestStepReportType report) {
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
                            newReports.add(OBJECT_FACTORY_TR.createTestAssertionGroupReportsTypeWarning(item.getValue()));
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

    public static void completeReportCounters(TAR report) {
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

    public static void applyContentTypes(DataType contentType, AnyContent reportItem) {
        if (contentType != null && reportItem != null) {
            if (contentType instanceof MapType) {
                for (var childItem: ((MapType) contentType).getItems().entrySet()) {
                    applyContentTypes(childItem.getValue(), reportItem.getItem().stream().filter(item -> Objects.equals(childItem.getKey(), item.getName())).findFirst().orElse(null));
                }
            } else if (contentType instanceof ListType) {
                var childContentItemIterator = ((ListType) contentType).iterator();
                var childReportItemIterator = reportItem.getItem().iterator();
                while (childContentItemIterator.hasNext() && childReportItemIterator.hasNext()) {
                    var childContentItem = childContentItemIterator.next();
                    var childReportItem = childReportItemIterator.next();
                    applyContentTypes(childContentItem, childReportItem);
                }
            } else if (contentType instanceof StringType) {
                // Apply the defined content type.
                reportItem.setMimeType((String) contentType.getValue());
            }
        }
    }

    private static String getTestEngineVersion() {
        try (var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("core-module.properties")) {
            var props = new Properties();
            props.load(stream);
            var version = props.getProperty("gitb.version");
            if (version.toLowerCase(Locale.getDefault()).endsWith("snapshot")) {
                version += " ("+props.getProperty("gitb.buildTimestamp")+")";
            }
            return version;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read core properties", e);
        }
    }

}

