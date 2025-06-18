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

package com.gitb.reports.dto;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class ConformanceStatementData {

    private Long actorId;
    private Long specificationId;
    private Long systemId;
    private String testActor;
    private String testActorInternal;
    private String testActorDescription;
    private String testActorReportMetadata;
    private String testSpecification;
    private String testSpecificationDescription;
    private String testSpecificationReportMetadata;
    private String testSpecificationGroup;
    private String testSpecificationGroupDescription;
    private String testSpecificationGroupReportMetadata;
    private String testDomain;
    private String testDomainDescription;
    private String testDomainReportMetadata;
    private String lastUpdated;
    private int completedTests = 0;
    private int failedTests = 0;
    private int undefinedTests = 0;
    private int completedTestsIgnored = 0;
    private int failedTestsIgnored = 0;
    private int undefinedTestsIgnored = 0;
    private String overallStatus;
    private List<TestSuiteOverview> testSuites;
    private Boolean hasOptionalTests = null;
    private Boolean hasRequiredTests = null;
    private Boolean hasDisabledTests = null;
    private List<TestCaseOverview.Tag> distinctTags;
    private Map<Long, LinkedHashMap<String, TestCaseGroup>> groupMap;

    public void prepareTestCaseGroups() {
        if (testSuites != null) {
            for (var testSuite: testSuites) {
                Map<String, Counters> groupResults = new HashMap<>();
                if (testSuite.getTestCases() != null) {
                    TestCaseOverview lastTestCase = null;
                    for (var testCase: testSuite.getTestCases()) {
                        if (testCase.getGroup() != null) {
                            testCase.setInGroup(true);
                            if (lastTestCase != null) {
                                if (!Objects.equals(testCase.getGroup(), lastTestCase.getGroup())) {
                                    testCase.setFirstInGroup(true);
                                    if (lastTestCase.getGroup() != null) {
                                        lastTestCase.setLastInGroup(true);
                                    }
                                }
                            } else {
                                testCase.setFirstInGroup(true);
                            }
                            // Add result
                            groupResults.computeIfAbsent(testCase.getGroup(), (key) -> new Counters()).addResult(testCase);
                        } else if (lastTestCase != null && lastTestCase.getGroup() != null) {
                            lastTestCase.setLastInGroup(true);
                        }
                        lastTestCase = testCase;
                    }
                    if (lastTestCase != null && lastTestCase.getGroup() != null) {
                        lastTestCase.setLastInGroup(true);
                    }
                }
                // Set group results
                var groupMap = getGroupMap(testSuite.getTestSuiteId());
                groupResults.forEach(((groupId, counters) -> groupMap.get(groupId).setResult(counters.toResult())));
            }
        }
    }

    public TestCaseGroup getTestCaseGroup(Long testSuiteId, String groupId) {
        var groupMap = getGroupMap(testSuiteId);
        if (groupMap != null) {
            return groupMap.get(groupId);
        }
        return null;
    }

    private LinkedHashMap<String, TestCaseGroup> getGroupMap(Long testSuiteId) {
        if (groupMap == null) {
            groupMap = new HashMap<>();
            if (testSuites != null) {
                for (var ts: testSuites) {
                    var groups = new LinkedHashMap<String, TestCaseGroup>();
                    if (ts.getTestCaseGroups() != null) {
                        for (var group: ts.getTestCaseGroups()) {
                            if (group.getId() != null) {
                                groups.put(group.getId(), group);
                            }
                        }
                    }
                    groupMap.put(ts.getTestSuiteId(), groups);
                }
            }
        }
        return groupMap.get(testSuiteId);
    }

    public String getTestActor() {
        return testActor;
    }

    public void setTestActor(String testActor) {
        this.testActor = testActor;
    }

    public String getTestSpecification() {
        return testSpecification;
    }

    public void setTestSpecification(String testSpecification) {
        this.testSpecification = testSpecification;
    }

    public String getTestSpecificationGroup() {
        return testSpecificationGroup;
    }

    public void setTestSpecificationGroup(String testSpecificationGroup) {
        this.testSpecificationGroup = testSpecificationGroup;
    }

    public String getTestDomain() {
        return testDomain;
    }

    public void setTestDomain(String testDomain) {
        this.testDomain = testDomain;
    }

    public int getCompletedTests() {
        return completedTests;
    }

    public void setCompletedTests(int completedTests) {
        this.completedTests = completedTests;
    }

    public int getFailedTests() {
        return failedTests;
    }

    public void setFailedTests(int failedTests) {
        this.failedTests = failedTests;
    }

    public int getUndefinedTests() {
        return undefinedTests;
    }

    public void setUndefinedTests(int undefinedTests) {
        this.undefinedTests = undefinedTests;
    }

    public int getCompletedTestsIgnored() {
        return completedTestsIgnored;
    }

    public void setCompletedTestsIgnored(int completedTestsIgnored) {
        this.completedTestsIgnored = completedTestsIgnored;
    }

    public int getFailedTestsIgnored() {
        return failedTestsIgnored;
    }

    public void setFailedTestsIgnored(int failedTestsIgnored) {
        this.failedTestsIgnored = failedTestsIgnored;
    }

    public int getUndefinedTestsIgnored() {
        return undefinedTestsIgnored;
    }

    public void setUndefinedTestsIgnored(int undefinedTestsIgnored) {
        this.undefinedTestsIgnored = undefinedTestsIgnored;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public List<TestSuiteOverview> getTestSuites() {
        return testSuites;
    }

    public void setTestSuites(List<TestSuiteOverview> testSuites) {
        this.testSuites = testSuites;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean hasOptionalTests() {
        if (hasOptionalTests == null) {
            hasOptionalTests = testSuites != null && testSuites.stream().anyMatch(testSuite -> testSuite.getTestCases().stream().anyMatch(TestCaseOverview::isOptional));
        }
        return hasOptionalTests;
    }

    public boolean hasRequiredTests() {
        if (hasRequiredTests == null) {
            hasRequiredTests = testSuites != null && testSuites.stream().anyMatch(testSuite -> testSuite.getTestCases().stream().anyMatch((tc) -> !tc.isDisabled() && !tc.isOptional()));
        }
        return hasRequiredTests;
    }

    public boolean hasDisabledTests() {
        if (hasDisabledTests == null) {
            hasDisabledTests = testSuites != null && testSuites.stream().anyMatch(testSuite -> testSuite.getTestCases().stream().anyMatch(TestCaseOverview::isDisabled));
        }
        return hasDisabledTests;
    }

    public String getTestStatus() {
        var resultText = new StringBuilder();
        var totalTests = completedTests + failedTests + undefinedTests;
        resultText.append(completedTests).append(" of ").append(totalTests).append(" required tests passed");
        if (totalTests > completedTests) {
            resultText.append(" (");
            if (failedTests > 0) {
                resultText.append(failedTests).append(" failed");
                if (undefinedTests > 0) {
                    resultText.append(", ");
                }
            }
            if (undefinedTests > 0) {
                resultText.append(undefinedTests).append(" incomplete");
            }
            resultText.append(")");
        }
        return resultText.toString();
    }

    public boolean hasTags() {
        return !getDistinctTags().isEmpty();
    }

    public List<TestCaseOverview.Tag> getDistinctTags() {
        if (distinctTags == null) {
            // Collect all distinct tags (by name and colours) that have a description.
            Map<String, TestCaseOverview.Tag> uniqueTags = new HashMap<>();
            testSuites.stream().map(TestSuiteOverview::getTestCases)
                    .flatMap(List::stream)
                    .filter((tc) -> tc.getTags() != null && !tc.getTags().isEmpty())
                    .map(TestCaseOverview::getTags)
                    .flatMap(List::stream)
                    .filter(tag -> StringUtils.isNotBlank(tag.description()))
                    .forEach(tag -> uniqueTags.putIfAbsent(tag.name()+"|"+tag.background()+"|"+tag.foreground(), tag));
            // Sort by name.
            if (uniqueTags.isEmpty()) {
                distinctTags = Collections.emptyList();
            } else {
                distinctTags = uniqueTags.values().stream().sorted(Comparator.comparing(TestCaseOverview.Tag::name)).toList();
            }
        }
        return distinctTags;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public Long getSpecificationId() {
        return specificationId;
    }

    public void setSpecificationId(Long specificationId) {
        this.specificationId = specificationId;
    }

    public Long getSystemId() {
        return systemId;
    }

    public void setSystemId(Long systemId) {
        this.systemId = systemId;
    }

    public String getTestActorInternal() {
        return testActorInternal;
    }

    public void setTestActorInternal(String testActorInternal) {
        this.testActorInternal = testActorInternal;
    }

    public String getTestActorDescription() {
        return testActorDescription;
    }

    public void setTestActorDescription(String testActorDescription) {
        this.testActorDescription = testActorDescription;
    }

    public String getTestSpecificationDescription() {
        return testSpecificationDescription;
    }

    public void setTestSpecificationDescription(String testSpecificationDescription) {
        this.testSpecificationDescription = testSpecificationDescription;
    }

    public String getTestSpecificationGroupDescription() {
        return testSpecificationGroupDescription;
    }

    public void setTestSpecificationGroupDescription(String testSpecificationGroupDescription) {
        this.testSpecificationGroupDescription = testSpecificationGroupDescription;
    }

    public String getTestDomainDescription() {
        return testDomainDescription;
    }

    public void setTestDomainDescription(String testDomainDescription) {
        this.testDomainDescription = testDomainDescription;
    }

    public String getTestActorReportMetadata() {
        return testActorReportMetadata;
    }

    public void setTestActorReportMetadata(String testActorReportMetadata) {
        this.testActorReportMetadata = testActorReportMetadata;
    }

    public String getTestSpecificationReportMetadata() {
        return testSpecificationReportMetadata;
    }

    public void setTestSpecificationReportMetadata(String testSpecificationReportMetadata) {
        this.testSpecificationReportMetadata = testSpecificationReportMetadata;
    }

    public String getTestSpecificationGroupReportMetadata() {
        return testSpecificationGroupReportMetadata;
    }

    public void setTestSpecificationGroupReportMetadata(String testSpecificationGroupReportMetadata) {
        this.testSpecificationGroupReportMetadata = testSpecificationGroupReportMetadata;
    }

    public String getTestDomainReportMetadata() {
        return testDomainReportMetadata;
    }

    public void setTestDomainReportMetadata(String testDomainReportMetadata) {
        this.testDomainReportMetadata = testDomainReportMetadata;
    }

    private static class Counters {
        private int successes = 0;
        private int failures = 0;
        private int incomplete = 0;

        private void addResult(TestCaseOverview testCase) {
            if (!testCase.isDisabled() && !testCase.isOptional()) {
                switch (testCase.getReportResult()) {
                    case "FAILURE" -> failures += 1;
                    case "UNDEFINED" -> incomplete += 1;
                    default -> successes += 1;
                }
            }
        }

        private String toResult() {
            if (successes > 0) {
                return "SUCCESS";
            } else if (failures > 0) {
                return "FAILURE";
            } else if (incomplete > 0) {
                return "UNDEFINED";
            } else {
                return null;
            }
        }
    }
}
