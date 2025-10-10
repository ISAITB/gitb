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

import com.gitb.core.Configuration;
import com.gitb.tdl.Process;
import com.gitb.tdl.*;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.rules.TestCaseSection;
import com.gitb.vs.tdl.util.Utils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CheckUniqueSetsOfValues extends AbstractTestCaseObserver {

    private final Set<String> imports = new HashSet<>();
    private final Set<String> importDuplicates = new TreeSet<>();
    private final Set<String> scriptletImports = new HashSet<>();
    private final Set<String> scriptletImportDuplicates = new TreeSet<>();
    private final Set<String> variables = new HashSet<>();
    private final Set<String> variableDuplicates = new TreeSet<>();
    private final Set<String> scriptletOutputs = new HashSet<>();
    private final Set<String> scriptletOutputDuplicates = new TreeSet<>();
    private final Set<String> scriptletVariables = new HashSet<>();
    private final Set<String> scriptletVariableDuplicates = new TreeSet<>();
    private final Set<String> scriptletParameters = new HashSet<>();
    private final Set<String> scriptletParameterDuplicates = new TreeSet<>();
    private boolean inInternalScriptlet = false;

    @Override
    public void initialiseTestCase(TestCase currentTestCase) {
        super.initialiseTestCase(currentTestCase);
        imports.clear();
        importDuplicates.clear();
        scriptletOutputs.clear();
        scriptletOutputDuplicates.clear();
        variables.clear();
        variableDuplicates.clear();
        scriptletParameters.clear();
        scriptletParameterDuplicates.clear();
    }

    @Override
    public void initialiseScriptlet(Scriptlet scriptlet) {
        super.initialiseScriptlet(scriptlet);
        scriptletImports.clear();
        scriptletImportDuplicates.clear();
        scriptletOutputs.clear();
        scriptletOutputDuplicates.clear();
        scriptletVariables.clear();
        scriptletVariableDuplicates.clear();
        scriptletParameters.clear();
        scriptletParameterDuplicates.clear();
        inInternalScriptlet = true;
    }

    @Override
    public void handleImport(Object artifactObj) {
        super.handleImport(artifactObj);
        if (artifactObj instanceof TestArtifact) {
            if (inInternalScriptlet) {
                recordAndCheckForDuplicate(((TestArtifact) artifactObj).getName(), scriptletImports, scriptletImportDuplicates);
            } else {
                recordAndCheckForDuplicate(((TestArtifact) artifactObj).getName(), imports, importDuplicates);
            }
        }
    }

    @Override
    public void handleInputParameter(InputParameter param) {
        handleVariable(param);
    }

    @Override
    public void handleVariable(Variable var) {
        super.handleVariable(var);
        if (section == TestCaseSection.SCRIPTLET_PARAMETERS) {
            // Scriptlet parameters.
            recordAndCheckForDuplicate(var.getName(), scriptletParameters, scriptletParameterDuplicates);
        } else {
            // Variables.
            if (inInternalScriptlet) {
                recordAndCheckForDuplicate(var.getName(), scriptletVariables, scriptletVariableDuplicates);
            } else {
                recordAndCheckForDuplicate(var.getName(), variables, variableDuplicates);
            }
        }
    }

    @Override
    public void handleOutput(Binding binding) {
        super.handleOutput(binding);
        if (section == TestCaseSection.SCRIPTLET_OUTPUT) {
            recordAndCheckForDuplicate(binding.getName(), scriptletOutputs, scriptletOutputDuplicates);
        }
    }

    private void recordAndCheckForDuplicate(String value, Set<String> valueSet, Set<String> duplicateSet) {
        if (value != null && !value.isBlank()) {
            if (valueSet.contains(value)) {
                duplicateSet.add(value);
            } else {
                valueSet.add(value);
            }
        }
    }

    @Override
    public void handleStep(Object stepObj) {
        super.handleStep(stepObj);
        if (stepObj instanceof BeginTransaction) {
            validateForUniqueValues(fromConfigs(((BeginTransaction) stepObj).getConfig()), ErrorCode.DUPLICATE_CONFIG);
        } else if (stepObj instanceof MessagingStep) {
            validateForUniqueValues(fromConfigs(((MessagingStep) stepObj).getConfig()), ErrorCode.DUPLICATE_CONFIG);
            validateForUniqueValues(fromBindings(((MessagingStep) stepObj).getInput()), ErrorCode.DUPLICATE_INPUT);
        } else if (stepObj instanceof BeginProcessingTransaction) {
            validateForUniqueValues(fromConfigs(((BeginProcessingTransaction) stepObj).getConfig()), ErrorCode.DUPLICATE_CONFIG);
        } else if (stepObj instanceof Process) {
            validateForUniqueValues(fromBindings(((Process) stepObj).getInput()), ErrorCode.DUPLICATE_INPUT);
        } else if (stepObj instanceof Verify) {
            validateForUniqueValues(fromConfigs(((Verify) stepObj).getConfig()), ErrorCode.DUPLICATE_CONFIG);
            validateForUniqueValues(fromBindings(((Verify) stepObj).getInput()), ErrorCode.DUPLICATE_INPUT);
        } else if (stepObj instanceof CallStep) {
            validateForUniqueValues(fromBindings(((CallStep) stepObj).getInput()), ErrorCode.DUPLICATE_INPUT);
            validateForUniqueValues(fromBindings(((CallStep) stepObj).getOutput()), ErrorCode.DUPLICATE_OUTPUT);
        }
    }

    private Collection<String> fromConfigs(Collection<Configuration> configs) {
        return configs.stream().map(Configuration::getName).collect(Collectors.toList());
    }

    private Collection<String> fromBindings(Collection<Binding> bindings) {
        return bindings.stream().map(Binding::getName).collect(Collectors.toList());
    }

    public void validateForUniqueValues(Collection<String> values, ErrorCode errorCode) {
        if (values != null) {
            var uniqueValues = new HashSet<String>();
            var duplicates = new LinkedHashSet<String>();
            for (var value: values) {
                if (value != null) {
                    if (uniqueValues.contains(value)) {
                        duplicates.add(value);
                    } else {
                        uniqueValues.add(value);
                    }
                }
            }
            for (var duplicate: duplicates) {
                addReportItem(errorCode, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), duplicate);
            }
        }
    }

    private void reportScriptletDuplicates(Collection<String> duplicates, ErrorCode errorCode, Function<String, String[]> argumentFunction) {
        for (var duplicate: duplicates) {
            addReportItem(errorCode, argumentFunction.apply(duplicate));
        }
    }

    @Override
    public void finaliseScriptlet() {
        inInternalScriptlet = false;
        reportScriptletDuplicates(scriptletImportDuplicates, ErrorCode.DUPLICATE_INTERNAL_SCRIPTLET_PROPERTY_NAME, (duplicate) -> new String[] {currentTestCase.getId(), currentScriptlet.getId(), "import", duplicate});
        reportScriptletDuplicates(scriptletVariableDuplicates, ErrorCode.DUPLICATE_INTERNAL_SCRIPTLET_PROPERTY_NAME, (duplicate) -> new String[] {currentTestCase.getId(), currentScriptlet.getId(), "variable", duplicate});
        reportScriptletDuplicates(scriptletParameterDuplicates, ErrorCode.DUPLICATE_INTERNAL_SCRIPTLET_PROPERTY_NAME, (duplicate) -> new String[] {currentTestCase.getId(), currentScriptlet.getId(), "parameter", duplicate});
        reportScriptletDuplicates(scriptletOutputDuplicates, ErrorCode.DUPLICATE_INTERNAL_SCRIPTLET_PROPERTY_NAME, (duplicate) -> new String[] {currentTestCase.getId(), currentScriptlet.getId(), "output", duplicate});
        // Check now for uniqueness across imports, variables and parameters.
        checkForCrossDuplicates(ErrorCode.DUPLICATE_INTERNAL_SCRIPTLET_DEFINITION_NAME,
                List.of(
                        new ImmutablePair<>("parameter", scriptletParameters),
                        new ImmutablePair<>("variable", scriptletVariables),
                        new ImmutablePair<>("import", scriptletImports)
                ),
                (info) -> new String[] {currentTestCase.getId(), currentScriptlet.getId(), info.getLeft(), info.getRight()}
        );
        super.finaliseScriptlet();
    }

    @Override
    public void finaliseTestCase() {
        reportScriptletDuplicates(importDuplicates, ErrorCode.DUPLICATE_PROPERTY_NAME, (duplicate) -> new String[] {currentTestCase.getId(), "import", duplicate});
        reportScriptletDuplicates(variableDuplicates, ErrorCode.DUPLICATE_PROPERTY_NAME, (duplicate) -> new String[] {currentTestCase.getId(), "variable", duplicate});
        if (testCaseIsWrappedScriptlet) {
            reportScriptletDuplicates(scriptletParameterDuplicates, ErrorCode.DUPLICATE_PROPERTY_NAME, (duplicate) -> new String[] {currentTestCase.getId(), "parameter", duplicate});
            reportScriptletDuplicates(scriptletOutputDuplicates, ErrorCode.DUPLICATE_PROPERTY_NAME, (duplicate) -> new String[] {currentTestCase.getId(), "output", duplicate});
            checkForCrossDuplicates(ErrorCode.DUPLICATE_DEFINITION_NAME,
                    List.of(
                            new ImmutablePair<>("parameter", scriptletParameters),
                            new ImmutablePair<>("variable", variables),
                            new ImmutablePair<>("import", imports)
                    ),
                    (info) -> new String[] {currentTestCase.getId(), info.getLeft(), info.getRight()}
            );
        } else {
            checkForCrossDuplicates(ErrorCode.DUPLICATE_DEFINITION_NAME,
                    List.of(
                            new ImmutablePair<>("variable", variables),
                            new ImmutablePair<>("import", imports)
                    ),
                    (info) -> new String[] {currentTestCase.getId(), info.getLeft(), info.getRight()}
            );
        }
        super.finaliseTestCase();
    }

    private void checkForCrossDuplicates(ErrorCode errorCode, List<Pair<String, Set<String>>> declarations, Function<Pair<String, String>, String[]> argumentFunction) {
        var allNames = new HashSet<String>();
        for (var declarationSet: declarations) {
            allNames.addAll(declarationSet.getRight());
        }
        boolean[] inSetChecks = new boolean[declarations.size()];
        for (var name: allNames) {
            int i = 0;
            for (var declarationSet: declarations) {
                inSetChecks[i++] = declarationSet.getRight().contains(name);
            }
            // Check to see if we have multiple matches.
            if (multipleMatches(inSetChecks)) {
                var parts = new ArrayList<String>(declarations.size());
                i = 0;
                for (var declarationSet: declarations) {
                    if (inSetChecks[i++]) {
                        parts.add(declarationSet.getLeft());
                    }
                }
                addReportItem(errorCode, argumentFunction.apply(new ImmutablePair<>(name, buildDescription(parts))));
            }
        }
    }

    private boolean multipleMatches(boolean[] matchStatus) {
        boolean hasMatch = false;
        for (var match: matchStatus) {
            if (match) {
                if (hasMatch) {
                    return true;
                } else {
                    hasMatch = true;
                }
            }
        }
        return false;
    }

    private String buildDescription(Collection<String> declarationTypes) {
        Iterator<String> it = declarationTypes.iterator();
        var msgBuilder = new StringBuilder();
        while (it.hasNext()) {
            var part = it.next();
            if (!msgBuilder.isEmpty()) {
                if (it.hasNext()) {
                    msgBuilder.append(", ");
                } else {
                    msgBuilder.append(" and ");
                }
            }
            msgBuilder.append(part);
        }
        return msgBuilder.toString();
    }

}
