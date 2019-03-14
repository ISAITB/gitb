package com.gitb.vs.tdl.rules;

import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class CheckTestSuiteCount extends AbstractCheck {

    @Override
    public void doCheck(Context context, ValidationReport report) {
        int testSuiteCount = context.getTestSuiteCount();
        if (testSuiteCount == 0) {
            report.addItem(ErrorCode.NO_TEST_SUITE_FOUND, "");
        } else if (testSuiteCount > 1) {
            Set<Path> paths = new HashSet<>(testSuiteCount);
            context.getTestSuitePaths().values().forEach(paths::addAll);
            report.addItem(ErrorCode.MULTIPLE_TEST_SUITES_FOUND, getResourceLocation(context, paths.toArray(new Path[] {})), String.valueOf(testSuiteCount));
        }
    }

}
