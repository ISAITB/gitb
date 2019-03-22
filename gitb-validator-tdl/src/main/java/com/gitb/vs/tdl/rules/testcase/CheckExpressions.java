package com.gitb.vs.tdl.rules.testcase;

import com.gitb.core.Configuration;
import com.gitb.core.TestRole;
import com.gitb.tdl.Process;
import com.gitb.tdl.*;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.rules.testcase.expression.VariableResolver;
import com.gitb.vs.tdl.rules.testcase.expression.VariableResolverProvider;
import com.gitb.vs.tdl.util.Utils;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.apache.commons.lang3.StringUtils;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckExpressions extends AbstractTestCaseObserver implements VariableResolverProvider {

    private Map<String, Boolean> scope;
    private VariableResolver variableResolver;

    @Override
    public void initialiseTestCase(TestCase currentTestCase) {
        super.initialiseTestCase(currentTestCase);
        scope = new HashMap<>();
        scope.put(Utils.DOMAIN_MAP, true);
        variableResolver = new VariableResolver(this);
    }

    @Override
    public void handleImport(Object artifactObj) {
        if (artifactObj instanceof TestArtifact) {
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
    public void handleStep(Object step) {
        super.handleStep(step);
        if (step instanceof TestConstruct && ((TestConstruct)step).getId() != null) {
            recordVariable(((TestConstruct)step).getId(), true);
        }
        if (step instanceof BeginTransaction) {
            checkToken(((BeginTransaction) step).getHandler(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            checkConfigurations(((BeginTransaction) step).getProperty());
            checkConfigurations(((BeginTransaction) step).getConfig());
        } else if (step instanceof MessagingStep) {
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
        } else if (step instanceof IfStep) {
            checkExpression(((IfStep) step).getCond());
        } else if (step instanceof WhileStep) {
            checkExpression(((WhileStep) step).getCond());
        } else if (step instanceof RepeatUntilStep) {
            checkExpression(((RepeatUntilStep) step).getCond());
        } else if (step instanceof ForEachStep) {
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
            checkToken(((Assign)step).getTo(), TokenType.VARIABLE_REFERENCE);
            checkToken(((Assign)step).getSource(), TokenType.VARIABLE_REFERENCE);
            checkToken(((Assign)step).getValue(), TokenType.EXPRESSION);
        } else if (step instanceof Verify) {
            checkToken(((Verify)step).getHandler(), TokenType.STRING_OR_VARIABLE_REFERENCE);
            checkConfigurations(((Verify) step).getProperty());
            checkConfigurations(((Verify) step).getConfig());
            checkBindings(((Verify) step).getInput());
        } else if (step instanceof CallStep) {
            checkBindings(((CallStep) step).getInput());
        } else if (step instanceof UserInteraction) {
            if (((UserInteraction)step).getInstructOrRequest() != null) {
                for (InstructionOrRequest ir: ((UserInteraction)step).getInstructOrRequest()) {
                    if (ir instanceof UserRequest) {
                        checkToken(ir.getValue(), TokenType.VARIABLE_REFERENCE);
                    } else {
                        checkExpression(ir);
                    }
                }
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

    private void checkToken(String token, TokenType expectedType) {
        if (StringUtils.isNotBlank(token)) {
            boolean isVariableExpression = Utils.isVariableExpression(token);
            if (expectedType == TokenType.VARIABLE_REFERENCE) {
                if (isVariableExpression) {
                    variableResolver.checkVariablesInToken(token);
                } else {
                    addReportItem(ErrorCode.INVALID_VARIABLE_REFERENCE, currentTestCase.getId(), Utils.getStepName(currentStep), token);
                }
            } else if (expectedType == TokenType.STRING_OR_VARIABLE_REFERENCE) {
                if (isVariableExpression) {
                    variableResolver.checkVariablesInToken(token);
                }
            } else if (expectedType == TokenType.EXPRESSION) {
                if (isVariableExpression) {
                    variableResolver.checkVariablesInToken(token);
                } else {
                    try {
                        XPathExpression expression = createXPathExpression(token);
                        expression.evaluate(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
                    } catch (XPathExpressionException e) {
                        addReportItem(ErrorCode.INVALID_EXPRESSION, currentTestCase.getId(), Utils.getStepName(currentStep), token);
                    } catch (ParserConfigurationException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
    }

    private XPathExpression createXPathExpression(String expression) throws XPathExpressionException {
        XPathFactory factory = new XPathFactoryImpl();
        factory.setXPathVariableResolver(variableResolver);
        XPath xPath = factory.newXPath();
        return xPath.compile(expression);
    }

    private void recordVariable(String name, Boolean isContainer) {
        scope.put(name, isContainer);
    }

    private void recordVariable(String name, String type) {
        recordVariable(name, Utils.isContainerType(type, context.getExternalConfiguration().getContainerDataTypes(), context.getExternalConfiguration().getContainedDataTypes()));
    }

    @Override
    public Map<String, Boolean> getScope() {
        return scope;
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
    public Object getCurrentStep() {
        return currentStep;
    }

    enum TokenType {
        STRING,
        STRING_OR_VARIABLE_REFERENCE,
        VARIABLE_REFERENCE,
        EXPRESSION
    }
}
