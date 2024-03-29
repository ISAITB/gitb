package com.gitb.vs.tdl.rules.testcase;

import com.gitb.core.TestRole;
import com.gitb.core.TestRoleEnumeration;
import com.gitb.tdl.BeginTransaction;
import com.gitb.tdl.InstructionOrRequest;
import com.gitb.tdl.MessagingStep;
import com.gitb.tdl.UserInteraction;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;
import com.gitb.vs.tdl.util.Utils;

import java.util.Set;
import java.util.TreeSet;

public class CheckTestCaseActorsInSteps extends AbstractTestCaseObserver {

    private Set<String> actorsInScriptlets;
    private Set<String> actorsReferencesInScriptlets;

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
        if (currentStep instanceof UserInteraction) {
            UserInteraction interaction = (UserInteraction)currentStep;
            validateActorReference(interaction.getWith(), TestRoleEnumeration.SUT, currentStep);
            if (interaction.getInstructOrRequest() != null) {
                for (InstructionOrRequest ir: interaction.getInstructOrRequest()) {
                    validateActorReference(ir.getWith(), TestRoleEnumeration.SUT, currentStep);
                }
            }
        } else if (currentStep instanceof BeginTransaction) {
            validateActorReference(((BeginTransaction)currentStep).getFrom(), null, currentStep);
            validateActorReference(((BeginTransaction)currentStep).getTo(), null, currentStep);
        } else if (currentStep instanceof MessagingStep) {
            validateActorReference(((MessagingStep)currentStep).getFrom(), null, currentStep);
            validateActorReference(((MessagingStep)currentStep).getTo(), null, currentStep);
        }
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
        }
    }
}
