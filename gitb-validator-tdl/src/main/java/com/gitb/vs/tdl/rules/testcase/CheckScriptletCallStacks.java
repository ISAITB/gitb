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

package com.gitb.vs.tdl.rules.testcase;

import com.gitb.tdl.*;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class CheckScriptletCallStacks extends AbstractTestCaseObserver {

    private LinkedList<Pair<CallStep, Scriptlet>> callStack;
    private Set<String> recursiveScriptlets;
    private Set<Triple<String, String, String>> invalidErrors;
    private Set<Triple<String, String, String>> referenceNotInTestCaseErrors;
    private Set<Triple<String, String, String>> noInputErrors;
    private Set<Triple<String, String, String>> notInTestCaseErrors;

    @Override
    public void initialiseTestCase(TestCase currentTestCase) {
        super.initialiseTestCase(currentTestCase);
        callStack = new LinkedList<>();
        recursiveScriptlets = new TreeSet<>();
        invalidErrors = new TreeSet<>();
        referenceNotInTestCaseErrors = new TreeSet<>();
        noInputErrors = new TreeSet<>();
        notInTestCaseErrors = new TreeSet<>();
    }

    @Override
    public void handleStep(Object step) {
        super.handleStep(step);
        // We only do this check starting from test cases (to ensure we have correct initial constants).
        if (!testCaseIsWrappedScriptlet) {
            if (step instanceof CallStep) {
                var scriptlet = findScriptlet((CallStep) step);
                if (scriptlet != null) {
                    callStack.addLast(Pair.of((CallStep) step, scriptlet));
                    validateCallStack(scriptlet.getSteps());
                    callStack.removeLast();
                }
            }
        }
    }

    private boolean scriptletAlreadyInCallStack(Scriptlet scriptlet) {
        return callStack.stream().anyMatch(existing -> Objects.equals(existing.getRight().getId(), scriptlet.getId()));
    }

    private Scriptlet findScriptlet(CallStep step) {
        // We don't report missing or invalid scriptlets here - only return ones that are found.
        Scriptlet locatedScriptlet = null;
        var from = step.getFrom();
        var path = step.getPath();
        if (StringUtils.isBlank(from) || from.equals(context.getTestSuite().getId())) {
            // Scriptlet from same test suite
            if (currentTestCase.getScriptlets() != null) {
                var testCaseScriptlet = currentTestCase.getScriptlets().getScriptlet().stream().filter(tcScriptlet -> Objects.equals(tcScriptlet.getId(), path)).findFirst();
                if (testCaseScriptlet.isPresent()) {
                    locatedScriptlet = testCaseScriptlet.get();
                }
            }
            if (locatedScriptlet == null) {
                // Look in external scriptlets in the same test suite.
                var resolvedPath = context.resolveTestSuiteResourceIfValid(path);
                if (resolvedPath != null) {
                    locatedScriptlet = context.getScriptletPaths().get(resolvedPath);
                }
            }
        }
        return locatedScriptlet;
    }

    private void validateCallStack(Sequence step) {
        if (step != null) {
            step.getSteps().forEach(this::validateCallStack);
        }
    }

    private void validateCallStack(Object step) {
        if (step instanceof BeginTransaction) {
            checkActorId(((BeginTransaction) step).getFrom(), step);
            checkActorId(((BeginTransaction) step).getTo(), step);
        } else if (step instanceof MessagingStep) {
            checkActorId(((MessagingStep) step).getFrom(), step);
            checkActorId(((MessagingStep) step).getTo(), step);
        } else if (step instanceof UserInteraction) {
            checkActorId(((UserInteraction) step).getWith(), step);
            ((UserInteraction) step).getInstructOrRequest().forEach(action -> checkActorId(action.getWith(), step));
        } else if (step instanceof Group) {
            validateCallStack((Group) step);
        } else if (step instanceof IfStep) {
            validateCallStack(((IfStep) step).getThen());
            validateCallStack(((IfStep) step).getElse());
        } else if (step instanceof WhileStep) {
            validateCallStack(((WhileStep) step).getDo());
        } else if (step instanceof RepeatUntilStep) {
            validateCallStack(((RepeatUntilStep) step).getDo());
        } else if (step instanceof ForEachStep) {
            validateCallStack(((ForEachStep) step).getDo());
        } else if (step instanceof FlowStep) {
            ((FlowStep) step).getThread().forEach(this::validateCallStack);
        } else if (step instanceof CallStep) {
            var scriptlet = findScriptlet((CallStep) step);
            if (scriptlet != null) {
                if (scriptletAlreadyInCallStack(scriptlet)) {
                    recursiveScriptlets.add(scriptlet.getId());
                } else {
                    callStack.addLast(Pair.of((CallStep) step, scriptlet));
                    validateCallStack(scriptlet.getSteps());
                    callStack.removeLast();
                }
            }
        }
    }

    private void checkActorId(String actorReference, Object step) {
        if (actorReference != null) {
            var callData = callStack.getLast();
            if (Utils.isVariableExpression(actorReference)) {
                boolean inputFound = false;
                var inputName = actorReference.substring(1); // Ignore first '$'
                var iterator = callStack.descendingIterator();
                var originalInputName = inputName;
                while (iterator.hasNext()) {
                    var currentCallData = iterator.next();
                    var inputToLookFor = inputName;
                    var matchedInput = currentCallData.getLeft().getInput().stream().filter(input -> inputToLookFor.equals(input.getName())).findFirst();
                    if (matchedInput.isPresent()) {
                        var inputValue = matchedInput.get().getValue();
                        if (Utils.isVariableExpression(inputValue)) {
                            // The input's value is itself a variable reference.
                            inputName = inputValue.substring(1); // Ignore first '$'
                            continue;
                        }
                        inputFound = true;
                        // The input has been matched but also needs to resolve to a fixed string.
                        String resolvedActorId = null;
                        try {
                            resolvedActorId = XPathFactory.newInstance().newXPath().compile(inputValue).evaluate(Utils.getSecureDocumentBuilderFactory().newDocumentBuilder().newDocument());
                        } catch (XPathExpressionException e) {
                            invalidErrors.add(Triple.of(callData.getRight().getId(), actorReference, Utils.stepNameWithScriptlet(step, null)));
                        } catch (ParserConfigurationException e) {
                            throw new IllegalStateException(e);
                        }
                        if (resolvedActorId != null) {
                            if (!actorDefinedInTestCase(resolvedActorId)) {
                                referenceNotInTestCaseErrors.add(Triple.of(callData.getRight().getId(), actorReference, Utils.stepNameWithScriptlet(step, null)));
                            }
                        }
                    }
                    break;
                }
                if (!inputFound) {
                    // No input found. Look also at variable default values.
                    var scriptlet = callData.getRight();
                    if (scriptlet.getParams() != null) {
                        var matchedVariableValue = scriptlet.getParams().getVar().stream().filter(variable -> originalInputName.equals(variable.getName()) && !variable.getValue().isEmpty()).findFirst();
                        if (matchedVariableValue.isPresent()) {
                            // The parameter defines a default value.
                            inputFound = true;
                            var resolvedActorId = matchedVariableValue.get().getValue().get(0).getValue();
                            if (!actorDefinedInTestCase(resolvedActorId)) {
                                referenceNotInTestCaseErrors.add(Triple.of(callData.getRight().getId(), actorReference, Utils.stepNameWithScriptlet(step, null)));
                            }
                        }
                    }
                }
                if (!inputFound) {
                    noInputErrors.add(Triple.of(callData.getRight().getId(), actorReference, Utils.stepNameWithScriptlet(step, null)));
                }
            } else {
                if (!actorDefinedInTestCase(actorReference)) {
                    notInTestCaseErrors.add(Triple.of(callData.getRight().getId(), actorReference, Utils.stepNameWithScriptlet(step, null)));
                }
            }
        }
    }

    private boolean actorDefinedInTestCase(String actorId) {
        return actorId != null && currentTestCase.getActors() != null && currentTestCase.getActors().getActor().stream().anyMatch(actor -> Objects.equals(actor.getId(), actorId));
    }

    @Override
    public void finaliseTestCase() {
        recursiveScriptlets.forEach(scriptlet -> addReportItem(ErrorCode.SCRIPTLET_CALLED_RECURSIVELY, currentTestCase.getId(), scriptlet));
        invalidErrors.forEach(data -> addReportItem(ErrorCode.SCRIPTLET_ACTOR_REFERENCE_INVALID, currentTestCase.getId(), data.getLeft(), data.getMiddle(), data.getRight()));
        referenceNotInTestCaseErrors.forEach(data -> addReportItem(ErrorCode.SCRIPTLET_ACTOR_REFERENCED_NOT_IN_TEST_CASE, currentTestCase.getId(), data.getLeft(), data.getMiddle(), data.getRight()));
        noInputErrors.forEach(data -> addReportItem(ErrorCode.SCRIPTLET_ACTOR_REFERENCE_WITHOUT_INPUT, currentTestCase.getId(), data.getLeft(), data.getMiddle(), data.getRight()));
        notInTestCaseErrors.forEach(data -> addReportItem(ErrorCode.SCRIPTLET_ACTOR_NOT_DEFINED_IN_TEST_CASE, currentTestCase.getId(), data.getLeft(), data.getMiddle(), data.getRight()));
        super.finaliseTestCase();
    }

}
