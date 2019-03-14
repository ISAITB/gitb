package com.gitb.vs.tdl.rules;

import com.gitb.core.Actor;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;

public class CheckExternalActorReferences extends AbstractCheck {

    @Override
    public void doCheck(Context context, ValidationReport report) {
        for (Actor actor: context.getTestSuiteActors().values()) {
            if (actor.getId() != null && actor.getName() == null) {
                // This is a reference to an external actor.
                if (!context.getExternalConfiguration().getExternalActorIds().contains(actor.getId())) {
                    report.addItem(ErrorCode.INVALID_EXTERNAL_ACTOR_REFERENCE, getTestSuiteLocation(context), actor.getId());
                }
            }
        }
    }
}
