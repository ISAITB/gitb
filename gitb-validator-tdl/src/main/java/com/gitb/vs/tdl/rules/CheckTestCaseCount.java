package com.gitb.vs.tdl.rules;

import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;

public class CheckTestCaseCount extends AbstractCheck {

    @Override
    public void doCheck(Context context, ValidationReport report) {
        int testCaseCount = context.getTestCaseCount();
        if (testCaseCount == 0) {
            report.addItem(ErrorCode.NO_TEST_CASE_FOUND, "");
        }
    }

}
