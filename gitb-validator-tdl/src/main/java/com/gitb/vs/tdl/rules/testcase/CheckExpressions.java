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
import net.sf.saxon.expr.instruct.DummyNamespaceResolver;
import net.sf.saxon.pull.NamespaceContextImpl;
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
        if (step instanceof TestConstruct && ((TestConstruct)step).getId() != null) {
            recordVariable(((TestConstruct)step).getId(), true);
        }
        if (step instanceof TestStep) {
            checkConstantReferenceInScriptlet(((TestStep) step).getDesc(), ATTRIBUTE_DESC);
            checkConstantReferenceInScriptlet(((TestStep) step).getHidden(), ATTRIBUTE_HIDDEN);
        }
        if (step instanceof BeginTransaction) {
            checkConstantReferenceInScriptlet(((BeginTransaction) step).getFrom(), ATTRIBUTE_FROM);
            checkConstantReferenceInScriptlet(((BeginTransaction) step).getTo(), ATTRIBUTE_TO);
            checkToken(((BeginTransaction) step).getHandler(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            checkConfigurations(((BeginTransaction) step).getProperty());
            checkConfigurations(((BeginTransaction) step).getConfig());
        } else if (step instanceof MessagingStep) {
            checkConstantReferenceInScriptlet(((MessagingStep) step).getFrom(), ATTRIBUTE_FROM);
            checkConstantReferenceInScriptlet(((MessagingStep) step).getTo(), ATTRIBUTE_TO);
            checkConstantReferenceInScriptlet(((MessagingStep) step).getReply(), ATTRIBUTE_REPLY);
            checkConfigurations(((MessagingStep) step).getConfig());
            checkBindings(((MessagingStep) step).getInput());
            if (step instanceof Receive) {
                checkToken(((Receive) step).getTimeout(), TokenType.STRING_OR_VARIABLE_REFERENCE);
                checkToken(((Receive) step).getTimeoutFlag(), TokenType.STRING_OR_VARIABLE_REFERENCE);
                checkToken(((Receive) step).getTimeoutIsError(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            }
        } else if (step instanceof BeginProcessingTransaction) {
            checkToken(((BeginProcessingTransaction) step).getHandler(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            checkConfigurations(((BeginProcessingTransaction) step).getProperty());
            checkConfigurations(((BeginProcessingTransaction) step).getConfig());
        } else if (step instanceof Process) {
            checkBindings(((Process) step).getInput());
            checkToken(((Process) step).getInputAttribute(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            if (((Process)step).getOutput() != null) {
                recordVariable(((Process)step).getOutput(), true);
            }
        } else if (step instanceof IfStep) {
            checkConstantReferenceInScriptlet(((IfStep) step).getTitle(), ATTRIBUTE_TITLE);
            checkConstantReferenceInScriptlet(((IfStep) step).getThen().getHidden(), ATTRIBUTE_HIDDEN);
            if (((IfStep) step).getElse() != null) {
                checkConstantReferenceInScriptlet(((IfStep) step).getElse().getHidden(), ATTRIBUTE_HIDDEN);
            }
            checkExpression(((IfStep) step).getCond());
        } else if (step instanceof WhileStep) {
            checkConstantReferenceInScriptlet(((WhileStep) step).getTitle(), ATTRIBUTE_TITLE);
            checkExpression(((WhileStep) step).getCond());
        } else if (step instanceof RepeatUntilStep) {
            checkConstantReferenceInScriptlet(((RepeatUntilStep) step).getTitle(), ATTRIBUTE_TITLE);
            checkExpression(((RepeatUntilStep) step).getCond());
        } else if (step instanceof ForEachStep) {
            checkConstantReferenceInScriptlet(((ForEachStep) step).getTitle(), ATTRIBUTE_TITLE);
            checkToken(((ForEachStep) step).getStart(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            checkToken(((ForEachStep) step).getEnd(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            if (((ForEachStep) step).getCounter() != null) {
                recordVariable(((ForEachStep) step).getCounter(), false);
            } else {
                recordVariable("i", false);
            }
        } else if (step instanceof ExitStep) {
            checkToken(((ExitStep)step).getSuccess(), TokenType.STRING_OR_VARIABLE_REFERENCE);
        } else if (step instanceof Assign) {
            String toToken = ((Assign) step).getTo();
            checkToken(toToken, TokenType.STRING_OR_VARIABLE_REFERENCE);
            if (toToken != null) {
                if (!toToken.startsWith("$")) {
                    // A new variable is defined.
                    Matcher mapMatcher = MAP_APPEND_EXPRESSION_PATTERN.matcher(toToken);
                    if (mapMatcher.matches()) {
                        String mapName = mapMatcher.group(1);
                        recordVariable(mapName, true);
                    } else {
                        // Always add this as a container as this is more flexible (we can't know at validation time if it is simple or not.
                        recordVariable(toToken, true);
                    }
                }
            }
            checkToken(((Assign) step).getSource(), TokenType.VARIABLE_REFERENCE);
            checkToken(((Assign) step).getValue(), TokenType.EXPRESSION);
        } else if (step instanceof Log) {
            checkToken(((Log) step).getSource(), TokenType.VARIABLE_REFERENCE);
            checkToken(((Log) step).getValue(), TokenType.EXPRESSION);
            checkToken(((Log)step).getLevel(), TokenType.LOG_LEVEL_OR_VARIABLE_REFERENCE);
        } else if (step instanceof Verify) {
            checkToken(((Verify)step).getHandler(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            checkToken(((Verify)step).getLevel(), TokenType.ERROR_LEVEL_OR_VARIABLE_REFERENCE);
            checkConfigurations(((Verify) step).getProperty());
            checkConfigurations(((Verify) step).getConfig());
            checkBindings(((Verify) step).getInput());
            if (((Verify)step).getOutput() != null) {
                recordVariable(((Verify)step).getOutput(), true);
            }
        } else if (step instanceof CallStep) {
            checkConstantReferenceInScriptlet(((CallStep) step).getHidden(), ATTRIBUTE_HIDDEN);
            checkBindings(((CallStep) step).getInput());
            checkToken(((CallStep) step).getInputAttribute(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            if (((CallStep)step).getOutputAttribute() != null) {
                recordVariable(((CallStep)step).getOutputAttribute(), true);
            }
        } else if (step instanceof UserInteraction) {
            checkConstantReferenceInScriptlet(((UserInteraction) step).getTitle(), ATTRIBUTE_TITLE);
            checkConstantReferenceInScriptlet(((UserInteraction) step).getDesc(), ATTRIBUTE_DESC);
            checkConstantReferenceInScriptlet(((UserInteraction) step).getWith(), ATTRIBUTE_WITH);
            checkToken(((UserInteraction) step).getInputTitle(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            if (((UserInteraction)step).getInstructOrRequest() != null) {
                for (InstructionOrRequest ir: ((UserInteraction)step).getInstructOrRequest()) {
                    checkConstantReferenceInScriptlet(ir.getDesc(), ATTRIBUTE_DESC);
                    checkConstantReferenceInScriptlet(ir.getWith(), ATTRIBUTE_WITH);
                    if (ir instanceof UserRequest) {
                        checkToken(ir.getValue(), TokenType.VARIABLE_REFERENCE);
                        checkToken(((UserRequest) ir).getOptions(), TokenType.STRING_OR_VARIABLE_REFERENCE);
                        checkToken(((UserRequest) ir).getOptionLabels(), TokenType.STRING_OR_VARIABLE_REFERENCE);
                        checkToken(((UserRequest) ir).getMultiple(), TokenType.STRING_OR_VARIABLE_REFERENCE);
                    } else {
                        checkExpression(ir);
                    }
                }
            }
        } else if (step instanceof Group) {
            checkConstantReferenceInScriptlet(((Group) step).getDesc(), ATTRIBUTE_DESC);
            checkConstantReferenceInScriptlet(((Group) step).getTitle(), ATTRIBUTE_TITLE);
        } else if (step instanceof FlowStep) {
            checkConstantReferenceInScriptlet(((FlowStep) step).getDesc(), ATTRIBUTE_DESC);
            checkConstantReferenceInScriptlet(((FlowStep) step).getTitle(), ATTRIBUTE_TITLE);
            for (var thread: ((FlowStep) step).getThread()) {
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
                            expression.evaluate(Utils.getSecureDocumentBuilderFactory().newDocumentBuilder().newDocument());
                        } catch (XPathExpressionException e) {
                            addReportItem(ErrorCode.INVALID_EXPRESSION, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), token);
                        } catch (ParserConfigurationException e) {
                            throw new IllegalStateException(e);
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
        if (propertyNames.size() > 0) {
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
