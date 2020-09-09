package com.gitb.vs.tdl.rules;

import com.gitb.tdl.TestSuite;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;

public class CheckTestSuiteDocumentation extends AbstractCheck {

    @Override
    public void doCheck(Context context, ValidationReport report) {
        TestSuite testSuite = context.getTestSuite();
        if (testSuite != null && testSuite.getMetadata() != null && testSuite.getMetadata().getDocumentation() != null) {
            context.validateDocumentation(testSuite.getMetadata().getDocumentation(),
                    (String path, String value) -> report.addItem(ErrorCode.TEST_SUITE_DOCUMENTATION_BOTH_AS_VALUE_AND_IMPORT, getTestSuiteLocation(context)),
                    (String path) -> report.addItem(ErrorCode.TEST_SUITE_DOCUMENTATION_REFERENCE_INVALID, getTestSuiteLocation(context), path)
            );
        }
    }

}
