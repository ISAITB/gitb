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

import com.gitb.core.Actor;
import com.gitb.core.TestRole;
import com.gitb.core.TestRoleEnumeration;
import com.gitb.tdl.TestCase;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;

import java.util.HashSet;
import java.util.Set;

public class CheckTestCaseActors extends AbstractTestCaseObserver {

    private Set<String> unreferencedTestSuiteActors;
    private Set<String> referencedActorIds;
    private Actor defaultActor;
    private boolean defaultActorDefinedAsSUT;
    private boolean sutDefined;

    @Override
    public void initialise(Context context, ValidationReport report) {
        super.initialise(context, report);
        unreferencedTestSuiteActors = new HashSet<>(context.getTestSuiteActors().keySet());
        defaultActor = context.getDefaultActor();
    }

    @Override
    public void initialiseTestCase(TestCase currentTestCase) {
        super.initialiseTestCase(currentTestCase);
        sutDefined = false;
        referencedActorIds = new HashSet<>();
    }

    @Override
    public void handleActor(TestRole testRole) {
        if (testRole.getRole() == TestRoleEnumeration.SUT) {
            if (sutDefined) {
                addReportItem(ErrorCode.MULTIPLE_SUT_ACTORS_DEFINED_IN_TEST_CASE, currentTestCase.getId());
            } else {
                sutDefined = true;
                if (defaultActor != null && testRole.getId().equals(defaultActor.getId())) {
                    defaultActorDefinedAsSUT = true;
                }
            }
        }
        if (!context.getTestSuiteActors().containsKey(testRole.getId())) {
            addReportItem(ErrorCode.INVALID_ACTOR_ID_REFERENCED_IN_TEST_CASE, currentTestCase.getId(), testRole.getId());
        }
        if (referencedActorIds.contains(testRole.getId())) {
            addReportItem(ErrorCode.TEST_CASE_REFERENCES_ACTOR_MULTIPLE_TIMES, currentTestCase.getId(), testRole.getId());
        } else {
            referencedActorIds.add(testRole.getId());
        }
        unreferencedTestSuiteActors.remove(testRole.getId());
    }

    @Override
    public void finaliseTestCase() {
        if (!testCaseIsWrappedScriptlet && !sutDefined) {
            addReportItem(ErrorCode.NO_SUT_DEFINED_IN_TEST_CASE, currentTestCase.getId());
        }
        super.finaliseTestCase();
    }

    @Override
    public void finalise() {
        if (!context.getTestCases().isEmpty()) {
            for (String actorId: unreferencedTestSuiteActors) {
                addReportItem(ErrorCode.ACTOR_NOT_REFERENCED_IN_TEST_CASES, actorId);
            }
            if (defaultActor != null && !defaultActorDefinedAsSUT) {
                addReportItem(ErrorCode.DEFAULT_ACTOR_NOT_REFERENCED_IN_TEST_CASES_AS_SUT, defaultActor.getId());
            }
        }
        super.finalise();
    }

}
