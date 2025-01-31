package com.gitb.vs.tdl.rules;

import com.gitb.core.TestRole;
import com.gitb.core.TestRoleEnumeration;
import com.gitb.tdl.TestCase;
import com.gitb.tdl.TestCaseGroup;
import com.gitb.tdl.TestSuite;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;

import java.util.*;
import java.util.stream.Collectors;

public class CheckTestCaseGroups extends AbstractCheck {

    @Override
    public void doCheck(Context context, ValidationReport report) {
        TestSuite testSuite = context.getTestSuite();
        if (testSuite != null && testSuite.getGroups() != null && !testSuite.getGroups().getGroup().isEmpty()) {
            var groupIds = new HashSet<>(testSuite.getGroups().getGroup().stream().map(TestCaseGroup::getId).toList());
            var groupIdIndexMap = new HashMap<String, Integer>();
            var groupTestCaseIdentifierMap = new HashMap<String, Set<String>>();
            var unreferencedGroupIds = new HashSet<>(groupIds);
            var groupsWithNonConsecutiveTestCases = new LinkedHashSet<String>();
            var groupCounts = new LinkedHashMap<String, Integer>();
            int testCaseIndex = 0;
            for (var testCase: testSuite.getTestcase()) {
                if (testCase.getGroup() != null && !testCase.getGroup().isBlank()) {
                    if (groupIdIndexMap.containsKey(testCase.getGroup())) {
                        if (groupIdIndexMap.get(testCase.getGroup()) != (testCaseIndex - 1)) {
                            groupsWithNonConsecutiveTestCases.add(testCase.getGroup());
                        }
                    }
                    if (groupCounts.containsKey(testCase.getGroup())) {
                        groupCounts.put(testCase.getGroup(), groupCounts.get(testCase.getGroup()) + 1);
                    } else {
                        groupCounts.put(testCase.getGroup(), 1);
                    }
                    groupIdIndexMap.put(testCase.getGroup(), testCaseIndex);
                    unreferencedGroupIds.remove(testCase.getGroup());
                    groupTestCaseIdentifierMap.computeIfAbsent(testCase.getGroup(), (key) -> new HashSet<>()).add(testCase.getId());
                }
                testCaseIndex += 1;
            }
            // Report the groups referring to different SUT actors
            Map<String, TestCase> testCaseMap = context.getTestCases();
            groupTestCaseIdentifierMap.entrySet().stream()
                    .filter((entry) -> {
                        var groupActors = entry.getValue().stream()
                                .map(testCaseMap::get) // Map test case identifier to the full test case
                                .map((testCase) -> {
                                    if (testCase.getActors() == null) {
                                        return null; // Test case with no actors - ignore
                                    } else {
                                        return testCase.getActors().getActor().stream()
                                                .filter((role) -> role.getRole() == TestRoleEnumeration.SUT) // Get the test case SUT actor
                                                .findFirst() // Get the first one
                                                .orElse(null); // Ignore if no SUT is defined
                                    }
                                })
                                .filter(Objects::nonNull) // Filter out the ignored cases
                                .map(TestRole::getId) // Get the SUT actor IDs
                                .collect(Collectors.toSet()); // Add then to a set to maintain the unique actor IDs
                        return groupActors.size() > 1; // Keep the groups that refer to more than one SUT actors
                    })
                    .map(Map.Entry::getKey) // Get the group identifier to report
                    .forEach(groupId -> report.addItem(ErrorCode.TEST_CASE_GROUP_WITH_TEST_CASES_FOR_DIFFERENT_SUTS, getTestSuiteLocation(context), groupId)); // Report each offending group
            if (!groupCounts.isEmpty()) {
                groupCounts.entrySet().stream()
                        .filter(entry -> entry.getValue() == 1)
                        .forEach(entry -> report.addItem(ErrorCode.TEST_CASE_GROUP_WITH_SINGLE_TEST_CASE, getTestSuiteLocation(context), entry.getKey()));
            }
            if (!groupsWithNonConsecutiveTestCases.isEmpty()) {
                groupsWithNonConsecutiveTestCases.forEach(groupId -> report.addItem(ErrorCode.NON_CONSECUTIVE_TEST_CASES_IN_GROUP, getTestSuiteLocation(context), groupId));
            }
            if (!unreferencedGroupIds.isEmpty()) {
                unreferencedGroupIds.forEach(groupId -> report.addItem(ErrorCode.TEST_CASE_GROUP_NOT_USED, getTestSuiteLocation(context), groupId));
            }
        }
    }

}
