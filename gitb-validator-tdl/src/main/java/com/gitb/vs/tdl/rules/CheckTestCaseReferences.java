package com.gitb.vs.tdl.rules;

import com.gitb.tdl.TestCaseEntry;
import com.gitb.tdl.TestSuite;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CheckTestCaseReferences extends AbstractCheck {

    @Override
    public void doCheck(Context context, ValidationReport report) {
        TestSuite testSuite = context.getTestSuite();
        if (testSuite != null) {
            Set<String> referencedTestCases = new HashSet<>();
            Set<String> duplicateTestCases = new HashSet<>();
            Set<String> unreferencedTestCases = new HashSet<>(context.getTestCasePaths().keySet());
            List<TestCaseEntry> testCaseEntries = testSuite.getTestcase();
            if (testCaseEntries != null) {
                for (TestCaseEntry entry: testCaseEntries) {
                    // Check the test case ID.
                    if (!context.getTestCasePaths().containsKey(entry.getId())) {
                        report.addItem(ErrorCode.INVALID_TEST_CASE_REFERENCE, getTestSuiteLocation(context), entry.getId());
                    }
                    // Make sure we haven't already referred to this test case.
                    if (referencedTestCases.contains(entry.getId())) {
                        duplicateTestCases.add(entry.getId());
                    } else {
                        referencedTestCases.add(entry.getId());
                    }
                    // Check the prerequisites defined for each test case.
                    if (entry.getPrequisite() != null) {
                        for (String prerequisite: entry.getPrequisite()) {
                            if (!context.getTestCaseIdsReferencedByTestSuite().contains(prerequisite)) {
                                report.addItem(ErrorCode.INVALID_TEST_CASE_PREREQUISITE, getTestSuiteLocation(context), prerequisite, entry.getId());
                            }
                        }
                    }
                    unreferencedTestCases.remove(entry.getId());
                }
            }
            for (String testCaseId: duplicateTestCases) {
                report.addItem(ErrorCode.DUPLICATE_TEST_CASE_REFERENCE, getTestSuiteLocation(context), testCaseId);
            }
            for (String testCaseId: unreferencedTestCases) {
                report.addItem(ErrorCode.TEST_CASE_NOT_REFERENCED, getTestCaseLocation(testCaseId, context), testCaseId);
            }
        }
    }

}
