package com.gitb.vs.tdl.rules.testcase;

import com.gitb.core.Configuration;
import com.gitb.core.TestRole;
import com.gitb.tdl.Process;
import com.gitb.tdl.*;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.rules.TestCaseSection;
import com.gitb.vs.tdl.rules.testcase.expression.VariableResolver;
import com.gitb.vs.tdl.rules.testcase.expression.VariableResolverProvider;
import com.gitb.vs.tdl.util.Utils;
import net.sf.saxon.expr.ArithmeticExpression;
import net.sf.saxon.expr.instruct.DummyNamespaceResolver;
import net.sf.saxon.pull.NamespaceContextImpl;
import net.sf.saxon.xpath.XPathExpressionImpl;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.apache.commons.lang3.StringUtils;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckExpressions extends AbstractTestCaseObserver implements VariableResolverProvider {

    private static final Pattern MAP_APPEND_EXPRESSION_PATTERN = Pattern.compile("(\\$?[a-zA-Z][a-zA-Z\\-_0-9]*(?:\\{(?:[\\$\\{\\}a-zA-Z\\-\\._0-9]*)\\})*)\\{(\\$?[a-zA-Z][a-zA-Z\\-\\._0-9]*)\\}");
    private static final String ATTRIBUTE_DESC = "desc";
    private static final String ATTRIBUTE_HIDDEN = "hidden";
    private static final String ATTRIBUTE_TITLE = "title";
    private static final String ATTRIBUTE_WITH = "with";
    private static final String ATTRIBUTE_FROM = "from";
    private static final String ATTRIBUTE_TO = "to";
    private static final String ATTRIBUTE_REPLY = "reply";

    private final Map<String, Boolean> testCaseScope = new HashMap<>();
    private final Map<String, Boolean> internalScriptletScope = new HashMap<>();
    private VariableResolver variableResolver;
    private final List<String> importVariableExpressionsToCheck = new ArrayList<>();
    private boolean inInternalScriptlet = false;

    @Override
    public void initialiseTestCase(TestCase currentTestCase) {
        super.initialiseTestCase(currentTestCase);
        testCaseScope.clear();
        testCaseScope.put(Utils.DOMAIN_MAP, true);
        testCaseScope.put(Utils.ORGANISATION_MAP, true);
        testCaseScope.put(Utils.SYSTEM_MAP, true);
        testCaseScope.put(Utils.SESSION_MAP, true);
        testCaseScope.put(Utils.STEP_SUCCESS, true);
        testCaseScope.put(Utils.STEP_STATUS, true);
        testCaseScope.put(Utils.TEST_SUCCESS, true);
        variableResolver = new VariableResolver(this);
    }

    @Override
    public void finaliseTestCase() {
        variableResolver.scopeFinished();
        super.finaliseTestCase();
    }

    @Override
    public void initialiseScriptlet(Scriptlet scriptlet) {
        super.initialiseScriptlet(scriptlet);
        inInternalScriptlet = true;
        internalScriptletScope.clear();
        internalScriptletScope.putAll(testCaseScope);
    }

    @Override
    public void finaliseScriptlet() {
        inInternalScriptlet = false;
        internalScriptletScope.clear();
        super.finaliseScriptlet();
    }

    @Override
    public void sectionChanged(TestCaseSection section) {
        super.sectionChanged(section);
        if (section == TestCaseSection.STEPS) {
            if (currentTestCase.getSteps() != null) {
                checkToken(currentTestCase.getSteps().getLogLevel(), TokenType.LOG_LEVEL_OR_VARIABLE_REFERENCE);
            }
        } else if (section == TestCaseSection.OUTPUT || section == TestCaseSection.SCRIPTLET_OUTPUT) {
            // In this section we have processed variables, parameters, actors and imports. We can now check imports for variable references.
            var currentStepCopy = currentStep;
            // Ensure the step is reported correctly.
            currentStep = new TestArtifact();
            for (String importExpression: importVariableExpressionsToCheck) {
                checkToken(importExpression, TokenType.VARIABLE_REFERENCE);
            }
            currentStep = currentStepCopy;
            /*
             We clear the checked values because this check will happen in two phases:
             - For the imports of a test case (or a standalone scriptlet).
             - For the imports of each test case's internal scriptlet.
             */
            importVariableExpressionsToCheck.clear();
        }
    }

    @Override
    public void handleImport(Object artifactObj) {
        super.handleImport(artifactObj);
        if (artifactObj instanceof TestArtifact) {
            if (Utils.isVariableExpression(((TestArtifact)artifactObj).getValue())) {
                // Park this because we need to make sure we have already processed actors and variable definitions.
                importVariableExpressionsToCheck.add(((TestArtifact)artifactObj).getValue());
            }
            recordVariable(((TestArtifact)artifactObj).getName(), ((TestArtifact)artifactObj).getType());
        }
    }

    @Override
    public void handleActor(TestRole testRole) {
        recordVariable(testRole.getId(), true);
    }

    @Override
    public void handleInputParameter(InputParameter param) {
        handleVariable(param);
    }

    @Override
    public void handleVariable(Variable var) {
        recordVariable(var.getName(), var.getType());
    }

    @Override
    public void handleOutput(Binding binding) {
        super.handleOutput(binding);
        if (section == TestCaseSection.SCRIPTLET_OUTPUT && binding != null) {
            // If a scriptlet output does not include a variable reference it's name must match a variable in the scriptlet's scope.
            if (binding.getValue() != null && !binding.getValue().isEmpty()) {
                var previousStep = currentStep;
                // Set current step to ensure correct reporting.
                currentStep = new Utils.ScriptletOutputsMarker();
                checkExpression(binding);
                currentStep = previousStep;
            } else {
                if (!getScope().containsKey(binding.getName())) {
                    if (inInternalScriptlet) {
                        addReportItem(ErrorCode.INTERNAL_SCRIPTLET_OUTPUT_NOT_FOUND_AS_VARIABLE, currentTestCase.getId(), currentScriptlet.getId(), binding.getName());
                    } else {
                        addReportItem(ErrorCode.EXTERNAL_SCRIPTLET_OUTPUT_NOT_FOUND_AS_VARIABLE, currentTestCase.getId(), binding.getName());
                    }
                }
            }
        }
    }

    @Override
    public void handleTestOutput(Output output) {
        super.handleTestOutput(output);
        if (output != null) {
            if (output.getSuccess() != null) {
                for (OutputCase outputCase: output.getSuccess().getCase()) {
                    checkExpression(outputCase.getCond());
                    checkExpression(outputCase.getMessage());
                }
                checkExpression(output.getSuccess().getDefault());
            }
            if (output.getFailure() != null) {
                for (OutputCase outputCase: output.getFailure().getCase()) {
                    checkExpression(outputCase.getCond());
                    checkExpression(outputCase.getMessage());
                }
                checkExpression(output.getFailure().getDefault());
            }
        }
    }

    @Override
    public void handleStep(Object step) {
        super.handleStep(step);
        if (step instanceof TestConstruct testConstructStep && testConstructStep.getId() != null) {
            recordVariable(testConstructStep.getId(), true);
        }
        if (step instanceof TestStep testStep) {
            checkConstantReferenceInScriptlet(testStep.getDesc(), ATTRIBUTE_DESC);
            checkConstantReferenceInScriptlet(testStep.getHidden(), ATTRIBUTE_HIDDEN);
        }
        if (step instanceof BeginTransaction beginTransactionStep) {
            checkConstantReferenceInScriptlet(beginTransactionStep.getFrom(), ATTRIBUTE_FROM);
            checkConstantReferenceInScriptlet(beginTransactionStep.getTo(), ATTRIBUTE_TO);
            checkToken(beginTransactionStep.getHandler(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            checkConfigurations(beginTransactionStep.getProperty());
            checkConfigurations(beginTransactionStep.getConfig());
        } else if (step instanceof MessagingStep messagingStep) {
            checkConstantReferenceInScriptlet(messagingStep.getFrom(), ATTRIBUTE_FROM);
            checkConstantReferenceInScriptlet(messagingStep.getTo(), ATTRIBUTE_TO);
            checkConstantReferenceInScriptlet(messagingStep.getReply(), ATTRIBUTE_REPLY);
            checkConfigurations(messagingStep.getConfig());
            checkBindings(messagingStep.getInput());
            if (step instanceof Receive receiveStep) {
                checkToken(receiveStep.getTimeout(), TokenType.STRING_OR_VARIABLE_REFERENCE);
                checkToken(receiveStep.getTimeoutFlag(), TokenType.STRING_OR_VARIABLE_REFERENCE);
                checkToken(receiveStep.getTimeoutIsError(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            }
        } else if (step instanceof BeginProcessingTransaction beginProcessingTransactionStep) {
            checkToken(beginProcessingTransactionStep.getHandler(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            checkConfigurations(beginProcessingTransactionStep.getProperty());
            checkConfigurations(beginProcessingTransactionStep.getConfig());
        } else if (step instanceof Process processStep) {
            checkBindings(processStep.getInput());
            checkToken(processStep.getInputAttribute(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            if (processStep.getOutput() != null) {
                recordVariable(processStep.getOutput(), true);
            }
        } else if (step instanceof IfStep ifStep) {
            checkConstantReferenceInScriptlet(ifStep.getTitle(), ATTRIBUTE_TITLE);
            checkConstantReferenceInScriptlet(ifStep.getThen().getHidden(), ATTRIBUTE_HIDDEN);
            if (ifStep.getElse() != null) {
                checkConstantReferenceInScriptlet(ifStep.getElse().getHidden(), ATTRIBUTE_HIDDEN);
            }
            checkExpression(ifStep.getCond());
        } else if (step instanceof WhileStep whileStep) {
            checkConstantReferenceInScriptlet(whileStep.getTitle(), ATTRIBUTE_TITLE);
            checkExpression(whileStep.getCond());
        } else if (step instanceof RepeatUntilStep repeatUntilStep) {
            checkConstantReferenceInScriptlet(repeatUntilStep.getTitle(), ATTRIBUTE_TITLE);
            checkExpression(repeatUntilStep.getCond());
        } else if (step instanceof ForEachStep forEachStep) {
            checkConstantReferenceInScriptlet(forEachStep.getTitle(), ATTRIBUTE_TITLE);
            checkToken(forEachStep.getStart(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            checkToken(forEachStep.getEnd(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            if (forEachStep.getCounter() != null) {
                recordVariable(forEachStep.getCounter(), false);
            } else {
                recordVariable("i", false);
            }
        } else if (step instanceof ExitStep exitStep) {
            checkToken(exitStep.getSuccess(), TokenType.STRING_OR_VARIABLE_REFERENCE);
        } else if (step instanceof Assign assignStep) {
            String toToken = assignStep.getTo();
            checkToken(toToken, TokenType.STRING_OR_VARIABLE_REFERENCE);
            if (toToken != null) {
                if (!toToken.startsWith("$")) {
                    // A new variable is defined.
                    Matcher mapMatcher = MAP_APPEND_EXPRESSION_PATTERN.matcher(toToken);
                    if (mapMatcher.matches()) {
                        // Loop as we might have nested maps.
                        boolean proceed;
                        do {
                            String mapName = mapMatcher.group(1);
                            recordVariable(mapName, true);
                            // Ensure we don't endlessly loop for a complete match.
                            proceed = !mapMatcher.group().equals(mapName);
                            if (proceed) {
                                // Check the already matched map to see if itself was a nested map.
                                mapMatcher = MAP_APPEND_EXPRESSION_PATTERN.matcher(mapName);
                                proceed = mapMatcher.matches();
                            }
                        } while (proceed);
                    } else {
                        // Always add this as a container as this is more flexible (we can't know at validation time if it is simple or not.
                        recordVariable(toToken, true);
                    }
                }
            }
            checkToken(assignStep.getSource(), TokenType.VARIABLE_REFERENCE);
            checkToken(assignStep.getValue(), TokenType.EXPRESSION);
        } else if (step instanceof Log logStep) {
            checkToken(logStep.getSource(), TokenType.VARIABLE_REFERENCE);
            checkToken(logStep.getValue(), TokenType.EXPRESSION);
            checkToken(logStep.getLevel(), TokenType.LOG_LEVEL_OR_VARIABLE_REFERENCE);
        } else if (step instanceof Verify verifyStep) {
            checkToken(verifyStep.getHandler(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            checkToken(verifyStep.getLevel(), TokenType.ERROR_LEVEL_OR_VARIABLE_REFERENCE);
            checkConfigurations(verifyStep.getProperty());
            checkConfigurations(verifyStep.getConfig());
            checkBindings(verifyStep.getInput());
            if (verifyStep.getOutput() != null) {
                recordVariable(verifyStep.getOutput(), true);
            }
        } else if (step instanceof CallStep callStep) {
            checkConstantReferenceInScriptlet(callStep.getHidden(), ATTRIBUTE_HIDDEN);
            checkBindings(callStep.getInput());
            checkToken(callStep.getInputAttribute(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            if (callStep.getOutputAttribute() != null) {
                recordVariable(callStep.getOutputAttribute(), true);
            }
        } else if (step instanceof UserInteraction userInteractionStep) {
            checkConstantReferenceInScriptlet(userInteractionStep.getTitle(), ATTRIBUTE_TITLE);
            checkConstantReferenceInScriptlet(userInteractionStep.getDesc(), ATTRIBUTE_DESC);
            checkConstantReferenceInScriptlet(userInteractionStep.getWith(), ATTRIBUTE_WITH);
            checkToken(userInteractionStep.getInputTitle(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            if (userInteractionStep.getInstructOrRequest() != null) {
                for (InstructionOrRequest ir: userInteractionStep.getInstructOrRequest()) {
                    checkConstantReferenceInScriptlet(ir.getDesc(), ATTRIBUTE_DESC);
                    checkConstantReferenceInScriptlet(ir.getWith(), ATTRIBUTE_WITH);
                    if (ir instanceof UserRequest userRequest) {
                        checkToken(ir.getValue(), TokenType.VARIABLE_REFERENCE);
                        checkToken(userRequest.getOptions(), TokenType.STRING_OR_VARIABLE_REFERENCE);
                        checkToken(userRequest.getOptionLabels(), TokenType.STRING_OR_VARIABLE_REFERENCE);
                        checkToken(userRequest.getMultiple(), TokenType.STRING_OR_VARIABLE_REFERENCE);
                        checkToken(userRequest.getFileName(), TokenType.STRING_OR_VARIABLE_REFERENCE);
                    } else {
                        checkExpression(ir);
                    }
                }
            }
        } else if (step instanceof Group groupStep) {
            checkConstantReferenceInScriptlet(groupStep.getDesc(), ATTRIBUTE_DESC);
            checkConstantReferenceInScriptlet(groupStep.getTitle(), ATTRIBUTE_TITLE);
        } else if (step instanceof FlowStep flowStep) {
            checkConstantReferenceInScriptlet(flowStep.getDesc(), ATTRIBUTE_DESC);
            checkConstantReferenceInScriptlet(flowStep.getTitle(), ATTRIBUTE_TITLE);
            for (var thread: flowStep.getThread()) {
                checkConstantReferenceInScriptlet(thread.getHidden(), ATTRIBUTE_HIDDEN);
            }
        }
    }

    private void checkExpression(Expression expression) {
        if (expression != null) {
            checkToken(expression.getSource(), TokenType.VARIABLE_REFERENCE);
            checkToken(expression.getValue(), TokenType.EXPRESSION);
        }
    }

    private void checkBindings(List<Binding> bindings) {
        if (bindings != null) {
            for (Binding binding: bindings) {
                checkToken(binding.getSource(), TokenType.VARIABLE_REFERENCE);
                checkToken(binding.getValue(), TokenType.EXPRESSION);
            }
        }
    }

    private void checkConfigurations(List<Configuration> configs) {
        if (configs != null) {
            for (Configuration config: configs) {
                checkToken(config.getValue(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            }
        }
    }

    private void checkConstantReferenceInScriptlet(String value, String attribute) {
        if (!testCaseIsWrappedScriptlet && Utils.isVariableExpression(value)) {
            addReportItem(ErrorCode.CONSTANT_REFERENCE_OUTSIDE_SCRIPTLET, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), value, attribute, attribute);
        }
    }

    private void checkToken(String token, TokenType expectedType) {
        if (StringUtils.isNotBlank(token)) {
            boolean isVariableExpression = Utils.isVariableExpression(token);
            if (isVariableExpression && StringUtils.countMatches(token, '{') != StringUtils.countMatches(token, '}')) {
                addReportItem(ErrorCode.INVALID_VARIABLE_REFERENCE, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), token);
            } else {
                if (expectedType == TokenType.VARIABLE_REFERENCE) {
                    if (isVariableExpression) {
                        variableResolver.checkVariablesInToken(token);
                    } else {
                        addReportItem(ErrorCode.INVALID_VARIABLE_REFERENCE, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), token);
                    }
                } else if (expectedType == TokenType.STRING_OR_VARIABLE_REFERENCE) {
                    if (isVariableExpression) {
                        variableResolver.checkVariablesInToken(token);
                    }
                } else if (expectedType == TokenType.ERROR_LEVEL_OR_VARIABLE_REFERENCE) {
                    if (isVariableExpression) {
                        variableResolver.checkVariablesInToken(token);
                    }
                } else if (expectedType == TokenType.LOG_LEVEL_OR_VARIABLE_REFERENCE) {
                    if (isVariableExpression) {
                        variableResolver.checkVariablesInToken(token);
                    }
                } else if (expectedType == TokenType.EXPRESSION) {
                    if (isVariableExpression) {
                        variableResolver.checkVariablesInToken(token);
                    } else {
                        try {
                            XPathExpression expression = createXPathExpression(token);
                            if (expression instanceof XPathExpressionImpl saxonExpression && saxonExpression.getInternalExpression() instanceof ArithmeticExpression) {
                                // This is an arithmetic expression. We need to ensure variables are evaluated as numbers.
                                variableResolver.setResolveVariablesAsNumber(true);
                            }
                            expression.evaluate(Utils.getSecureDocumentBuilderFactory().newDocumentBuilder().newDocument());
                        } catch (XPathExpressionException e) {
                            addReportItem(ErrorCode.INVALID_EXPRESSION, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), token);
                        } catch (ParserConfigurationException e) {
                            throw new IllegalStateException(e);
                        } finally {
                            variableResolver.setResolveVariablesAsNumber(false);
                        }
                    }
                }
            }
        }
    }

    private XPathExpression createXPathExpression(String expression) throws XPathExpressionException {
        XPathFactoryImpl factory = new XPathFactoryImpl();
        factory.setXPathVariableResolver(variableResolver);
        XPath xPath = factory.newXPath();
        xPath.setNamespaceContext(new NamespaceContextImpl(DummyNamespaceResolver.getInstance()));
        return xPath.compile(VariableResolver.toLegalXPath(expression));
    }

    private void recordVariable(String name, Boolean isContainer) {
        getScope().put(name, isContainer);
    }

    private void recordVariable(String name, String type) {
        recordVariable(name, Utils.isContainerType(type, context.getExternalConfiguration().getContainerDataTypes(), context.getExternalConfiguration().getContainedDataTypes()));
    }

    @Override
    public Map<String, Boolean> getScope() {
        if (inInternalScriptlet) {
            return internalScriptletScope;
        } else {
            return testCaseScope;
        }
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public TestCase getCurrentTestCase() {
        return currentTestCase;
    }

    @Override
    public Scriptlet getCurrentScriptlet() {
        return currentScriptlet;
    }

    @Override
    public Object getCurrentStep() {
        return currentStep;
    }

    @Override
    public boolean isInternalScriptlet() {
        return currentScriptlet != null;
    }

    @Override
    public boolean isStandaloneScriptlet() {
        return testCaseIsWrappedScriptlet;
    }

    @Override
    public void finalise() {
        super.finalise();
        reportCustomPropertyUsage(ErrorCode.INVALID_EXTERNAL_PARAMETER_REFERENCE, context.getCustomDomainParametersUsed());
        reportCustomPropertyUsage(ErrorCode.POTENTIALLY_INVALID_ORGANISATION_VARIABLE, context.getCustomOrganisationPropertiesUsed());
        reportCustomPropertyUsage(ErrorCode.POTENTIALLY_INVALID_SYSTEM_VARIABLE, context.getCustomSystemPropertiesUsed());
    }

    private void reportCustomPropertyUsage(ErrorCode code, Collection<String> propertyNames) {
        if (!propertyNames.isEmpty()) {
            addReportItem(code, String.format("[%s]", String.join(", ", propertyNames)));
        }
    }

    enum TokenType {
        STRING,
        STRING_OR_VARIABLE_REFERENCE,
        ERROR_LEVEL_OR_VARIABLE_REFERENCE,
        LOG_LEVEL_OR_VARIABLE_REFERENCE,
        VARIABLE_REFERENCE,
        EXPRESSION
    }
}
