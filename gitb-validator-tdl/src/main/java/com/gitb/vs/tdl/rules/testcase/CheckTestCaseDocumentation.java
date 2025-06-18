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

import com.gitb.core.Documentation;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.rules.TestCaseSection;
import com.gitb.vs.tdl.util.Utils;

public class CheckTestCaseDocumentation extends AbstractTestCaseObserver {

    @Override
    public void handleDocumentation(Documentation documentation) {
        super.handleDocumentation(documentation);
        if (section == TestCaseSection.METADATA) {
            // Documentation of the test case itself.
            context.validateDocumentation(documentation,
                    (String path, String value) -> addReportItem(ErrorCode.TEST_CASE_DOCUMENTATION_BOTH_AS_VALUE_AND_IMPORT, currentTestCase.getId()),
                    (String path) -> addReportItem(ErrorCode.TEST_CASE_DOCUMENTATION_REFERENCE_INVALID, currentTestCase.getId(), path)
            );
        } else {
            // Documentation in a test step.
            context.validateDocumentation(documentation,
                    (String path, String value) -> addReportItem(ErrorCode.TEST_STEP_DOCUMENTATION_BOTH_AS_VALUE_AND_IMPORT, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet)),
                    (String path) -> addReportItem(ErrorCode.TEST_STEP_DOCUMENTATION_REFERENCE_INVALID, currentTestCase.getId(), Utils.stepNameWithScriptlet(currentStep, currentScriptlet), path)
            );
        }
    }
}
