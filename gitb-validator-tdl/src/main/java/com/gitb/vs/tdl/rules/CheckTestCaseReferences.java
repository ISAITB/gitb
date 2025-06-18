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
