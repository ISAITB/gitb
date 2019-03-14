package com.gitb.vs.tdl.rules.testcase;

import com.gitb.core.TestRole;
import com.gitb.core.TestRoleEnumeration;
import com.gitb.tdl.InstructionOrRequest;
import com.gitb.tdl.MessagingStep;
import com.gitb.tdl.UserInteraction;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.util.Utils;

public class CheckTestCaseActorsInSteps extends AbstractTestCaseObserver {

    @Override
    public void handleStep(Object step) {
        super.handleStep(step);
        if (currentStep instanceof UserInteraction) {
            UserInteraction interaction = (UserInteraction)currentStep;
            validateActorReference(interaction.getWith(), TestRoleEnumeration.SUT, currentStep);
            if (interaction.getInstructOrRequest() != null) {
                for (InstructionOrRequest ir: interaction.getInstructOrRequest()) {
                    validateActorReference(ir.getWith(), TestRoleEnumeration.SUT, currentStep);
                }
            }
        } else if (currentStep instanceof MessagingStep) {
            validateActorReference(((MessagingStep)currentStep).getFrom(), null, currentStep);
            validateActorReference(((MessagingStep)currentStep).getTo(), null, currentStep);
        }
    }

    private void validateActorReference(String actorId, TestRoleEnumeration expectedRole, Object currentStep) {
        if (actorId != null) {
            TestRole role = context.getTestCaseActors().get(currentTestCase.getId()).get(actorId);
            if (role == null) {
                addReportItem(ErrorCode.INVALID_ACTOR_REFERENCE_IN_STEP, currentTestCase.getId(), Utils.getStepName(currentStep), actorId);
            } else if (expectedRole != null && expectedRole != role.getRole()) {
                addReportItem(ErrorCode.REFERENCED_ACTOR_IN_STEP_HAS_UNEXPECTED_ROLE, currentTestCase.getId(), Utils.getStepName(currentStep), actorId, role.getRole().value(), expectedRole.value());
            }
        }
    }

}
