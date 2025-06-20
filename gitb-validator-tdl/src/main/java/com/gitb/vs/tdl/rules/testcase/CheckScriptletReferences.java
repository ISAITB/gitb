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

import com.gitb.tdl.Binding;
import com.gitb.tdl.CallStep;
import com.gitb.tdl.Scriptlet;
import com.gitb.tdl.TestCase;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class CheckScriptletReferences extends AbstractTestCaseObserver {

    private Map<String, Scriptlet> definedScriptlets;
    private Set<String> calledScriptlets;
    private Set<String> invalidTestCaseScriptletReferences;
    private Set<String> invalidTestSuiteScriptletReferences;
    private Set<Pair<String, String>> externalScriptlets;

    @Override
    public void initialise(Context context, ValidationReport report) {
        super.initialise(context, report);
        externalScriptlets = new LinkedHashSet<>();
    }

    @Override
    public void initialiseTestCase(TestCase currentTestCase) {
        super.initialiseTestCase(currentTestCase);
        definedScriptlets = new HashMap<>();
        calledScriptlets = new HashSet<>();
        invalidTestCaseScriptletReferences = new HashSet<>();
        invalidTestSuiteScriptletReferences = new HashSet<>();
        if (currentTestCase.getScriptlets() != null) {
            for (var scriptlet: currentTestCase.getScriptlets().getScriptlet()) {
                if (definedScriptlets.containsKey(scriptlet.getId())) {
                    addReportItem(ErrorCode.DUPLICATE_SCRIPTLET_ID, currentTestCase.getId(), scriptlet.getId());
                } else {
                    definedScriptlets.put(scriptlet.getId(), scriptlet);
                }
            }
        }
    }

    @Override
    public void handleStep(Object step) {
        super.handleStep(step);
        if (step instanceof CallStep) {
            var from = ((CallStep) step).getFrom();
            var path = ((CallStep) step).getPath();
            if (StringUtils.isBlank(from) || from.equals(context.getTestSuite().getId())) {
                // Scriptlet from same test suite. Look first in local scriptlets.
                if (definedScriptlets.containsKey(path)) {
                    calledScriptlets.add(path);
                } else {
                    // Look in external scriptlets in the same test suite.
                    var resolvedPath = context.resolveTestSuiteResourceIfValid(path);
                    if (resolvedPath == null) {
                        // No file was found using the path.
                        invalidTestCaseScriptletReferences.add(path);
                    } else {
                        var resolvedScriptlet = context.getScriptletPaths().get(resolvedPath);
                        if (resolvedScriptlet == null) {
                            // A file was found but was not a scriptlet.
                            invalidTestSuiteScriptletReferences.add(path);
                        } else {
                            // Scriptlet found - check inputs and outputs.
                            // Check inputs.
                            var definedInputs = toSetOfNames(((CallStep) step).getInput());
                            String inputAttribute = null;
                            if (definedInputs.isEmpty()) {
                                inputAttribute = ((CallStep) step).getInputAttribute();
                            } else if (((CallStep) step).getInputAttribute() != null) {
                                addReportItem(ErrorCode.DOUBLE_CALL_INPUTS, currentTestCase.getId(), path);
                            }
                            var validInputs = new HashSet<String>();
                            var requiredInputs = new HashSet<String>();
                            if (resolvedScriptlet.getParams() != null) {
                                for (var variable: resolvedScriptlet.getParams().getVar()) {
                                    validInputs.add(variable.getName());
                                    if (!variable.isOptional()) {
                                        requiredInputs.add(variable.getName());
                                    }
                                }
                            }
                            if (validInputs.size() == 1 && inputAttribute != null) {
                                requiredInputs.clear();
                            } else {
                                for (var definedInput: definedInputs) {
                                    if (!validInputs.contains(definedInput)) {
                                        addReportItem(ErrorCode.UNEXPECTED_SCRIPTLET_INPUT, currentTestCase.getId(), path, definedInput);
                                    } else {
                                        requiredInputs.remove(definedInput);
                                    }
                                }
                            }
                            // Remove from the expected inputs the ones for which we have default values defined by the scriptlet.
                            if (resolvedScriptlet.getParams() != null) {
                                resolvedScriptlet.getParams().getVar().stream().filter(variable -> !variable.getValue().isEmpty() || variable.isDefaultEmpty()).forEach(variableWithDefaultValue -> requiredInputs.remove(variableWithDefaultValue.getName()));
                            }
                            // Now for everything that remains report missing values.
                            for (var missingInput: requiredInputs) {
                                addReportItem(ErrorCode.MISSING_SCRIPTLET_INPUT, currentTestCase.getId(), path, missingInput);
                            }
                            // Check outputs.
                            var requestedOutputs = toSetOfNames(((CallStep) step).getOutput());
                            var supportedOutputs = toSetOfNames(resolvedScriptlet.getOutput());
                            for (var output: requestedOutputs) {
                                if (!supportedOutputs.contains(output)) {
                                    addReportItem(ErrorCode.UNEXPECTED_SCRIPTLET_OUTPUT, currentTestCase.getId(), path, output);
                                }
                            }
                        }
                    }
                }
            } else {
                // Reference to scriptlet from another test suite.
                externalScriptlets.add(new ImmutablePair<>(from, path));
            }
        }
    }

    private Set<String> toSetOfNames(List<Binding> bindings) {
        var names = new HashSet<String>();
        for (var binding: bindings) {
            names.add(binding.getName());
        }
        return names;
    }

    @Override
    public void finaliseTestCase() {
        for (String id: definedScriptlets.keySet()) {
            if (!calledScriptlets.contains(id)) {
                addReportItem(ErrorCode.UNUSED_SCRIPTLET, currentTestCase.getId(), id);
            }
        }
        for (var path: invalidTestCaseScriptletReferences) {
            addReportItem(ErrorCode.INVALID_SCRIPTLET_REFERENCE, currentTestCase.getId(), path);
        }
        for (var path: invalidTestSuiteScriptletReferences) {
            addReportItem(ErrorCode.SCRIPTLET_REFERENCE_DID_NOT_MATCH_SCRIPTLET, currentTestCase.getId(), path);
        }
        super.finaliseTestCase();
    }

    @Override
    public void finalise() {
        super.finalise();
        for (var ref: externalScriptlets) {
            addReportItem(ErrorCode.EXTERNAL_SCRIPTLET_USED, ref.getRight(), ref.getLeft());
        }
    }
}
