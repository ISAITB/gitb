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
    private String overallStatus;
    private List<TestSuiteOverview> testSuites;
    private Boolean hasOptionalTests = null;
    private Boolean hasRequiredTests = null;
    private Boolean hasDisabledTests = null;
    private List<TestCaseOverview.Tag> distinctTags;

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
}
