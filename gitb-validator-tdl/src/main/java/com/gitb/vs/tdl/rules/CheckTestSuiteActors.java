package com.gitb.vs.tdl.rules;

import com.gitb.core.Actor;
import com.gitb.tdl.TestSuite;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;

import java.util.HashSet;
import java.util.Set;

public class CheckTestSuiteActors extends AbstractCheck {

    @Override
    public void doCheck(Context context, ValidationReport report) {
        TestSuite testSuite = context.getTestSuite();
        Set<String> uniqueActorIds = new HashSet<>();
        if (testSuite != null && testSuite.getActors() != null && testSuite.getActors().getActor() != null) {
            for (Actor actor: testSuite.getActors().getActor()) {
                if (uniqueActorIds.contains(actor.getId())) {
                    report.addItem(ErrorCode.TEST_SUITE_DEFINES_ACTOR_MULTIPLE_TIMES, getTestSuiteLocation(context), actor.getId());
                } else {
                    uniqueActorIds.add(actor.getId());
                }
            }
        }
    }

}
