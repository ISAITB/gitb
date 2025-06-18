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

import com.gitb.core.TestRole;
import com.gitb.core.TestRoleEnumeration;
import com.gitb.tdl.*;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;
import com.gitb.vs.tdl.util.Utils;

import java.util.Set;
import java.util.TreeSet;

public class CheckTestCaseActorsInSteps extends AbstractTestCaseObserver {

    private Set<String> actorsInScriptlets;
    private Set<String> actorsReferencesInScriptlets;
    private boolean scriptletsWithMissingActorReferences = false;
    private Long simulatorActorCount = null;

    @Override
    public void initialise(Context context, ValidationReport report) {
        super.initialise(context, report);
        actorsInScriptlets = new TreeSet<>();
        actorsReferencesInScriptlets = new TreeSet<>();
    }

    @Override
    public void handleStep(Object step) {
        super.handleStep(step);
        // Only make these tests for test cases, not standalone scriptlets.
        if (currentStep instanceof UserInteraction interaction) {
            validateActorReference(interaction.getWith(), TestRoleEnumeration.SUT, currentStep);
            if (interaction.getInstructOrRequest() != null) {
                for (InstructionOrRequest ir: interaction.getInstructOrRequest()) {
                    validateActorReference(ir.getWith(), TestRoleEnumeration.SUT, currentStep);
                }
            }
        } else if (currentStep instanceof BeginTransaction) {
            validateActorReference(((BeginTransaction)currentStep).getFrom(), null, currentStep);
            validateActorReference(((BeginTransaction)currentStep).getTo(), null, currentStep);
        } else if (currentStep instanceof MessagingStep messagingStep) {
            String fromValue = messagingStep.getFrom();
            String toValue = messagingStep.getTo();
            validateActorReference(fromValue, null, currentStep);
            validateActorReference(toValue, null, currentStep);
            if (toValue == null && messagingStep instanceof ReceiveOrListen) {
                if (testCaseIsWrappedScriptlet) {
                    scriptletsWithMissingActorReferences = true;
                } else if (countSimulatedActors() != 1) {
                    // We have a missing actor reference that should be to a simulated actor.
                    addReportItem(ErrorCode.MISSING_ACTOR_REFERENCE, currentTestCase.getId(), Utils.getStepName(currentStep), "to");
                }
            } else if (fromValue == null && messagingStep instanceof Send) {
                if (testCaseIsWrappedScriptlet) {
                    scriptletsWithMissingActorReferences = true;
                } else if (countSimulatedActors() != 1) {
                    // We have a missing actor reference that should be to a simulated actor.
                    addReportItem(ErrorCode.MISSING_ACTOR_REFERENCE, currentTestCase.getId(), Utils.getStepName(currentStep), "from");
                }
            }
        }
    }

    private long countSimulatedActors() {
        if (simulatorActorCount == null) {
            simulatorActorCount = currentTestCase.getActors().getActor().stream()
                    .filter(role -> role.getRole() == null || role.getRole() == TestRoleEnumeration.SIMULATED)
                    .count();
        }
        return simulatorActorCount;
    }

    private void validateActorReference(String actorId, TestRoleEnumeration expectedRole, Object currentStep) {
        if (actorId != null) {
            if (testCaseIsWrappedScriptlet) {
                // Scriptlet
                if (Utils.isVariableExpression(actorId)) {
                    actorsReferencesInScriptlets.add(actorId);
                } else {
                    actorsInScriptlets.add(actorId);
                }
            } else {
                // Test case
                if (!Utils.isVariableExpression(actorId)) {
                    // Actors defined as expressions are covered in [TDL-108]. We only test fixed values here.
                    TestRole role = context.getTestCaseActors().get(currentTestCase.getId()).get(actorId);
                    if (role == null) {
                        addReportItem(ErrorCode.INVALID_ACTOR_REFERENCE_IN_STEP, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), actorId);
                    } else if (expectedRole != null && expectedRole != role.getRole()) {
                        addReportItem(ErrorCode.REFERENCED_ACTOR_IN_STEP_HAS_UNEXPECTED_ROLE, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), actorId, role.getRole().value(), expectedRole.value());
                    }
                }
            }
        }
    }

    @Override
    public void finalise() {
        super.finalise();
        if (testCaseIsWrappedScriptlet) {
            if (!actorsInScriptlets.isEmpty() && !actorsReferencesInScriptlets.isEmpty()) {
                addReportItem(ErrorCode.ACTOR_REFERENCES_IN_SCRIPTLET_VALUES_AND_REFS, "["+String.join(", ", actorsInScriptlets)+"]", "["+String.join(", ", actorsReferencesInScriptlets)+"]");
            } else if (!actorsInScriptlets.isEmpty()) {
                addReportItem(ErrorCode.ACTOR_REFERENCES_IN_SCRIPTLET_VALUES, "["+String.join(", ", actorsInScriptlets)+"]");
            } else if (!actorsReferencesInScriptlets.isEmpty()) {
                addReportItem(ErrorCode.ACTOR_REFERENCES_IN_SCRIPTLET_REFS, "["+String.join(", ", actorsReferencesInScriptlets)+"]");
            }
            if (scriptletsWithMissingActorReferences) {
                addReportItem(ErrorCode.ACTOR_REFERENCES_IN_SCRIPTLET_MISSING);
            }
        }
    }
}
