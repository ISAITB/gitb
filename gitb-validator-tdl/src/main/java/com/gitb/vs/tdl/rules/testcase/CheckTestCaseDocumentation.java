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
                    (String path, String value) -> addReportItem(ErrorCode.TEST_STEP_DOCUMENTATION_BOTH_AS_VALUE_AND_IMPORT, currentTestCase.getId(), Utils.getStepName(currentStep)),
                    (String path) -> addReportItem(ErrorCode.TEST_STEP_DOCUMENTATION_REFERENCE_INVALID, currentTestCase.getId(), Utils.getStepName(currentStep), path)
            );
        }
    }
}
