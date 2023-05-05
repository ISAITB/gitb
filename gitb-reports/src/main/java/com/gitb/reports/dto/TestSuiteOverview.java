package com.gitb.reports.dto;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class TestSuiteOverview {

    private String testSuiteName;
    private String testSuiteDescription;
    private String overallStatus;
    private List<TestCaseOverview> testCases;

    public List<TestCaseOverview> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseOverview> testCases) {
        this.testCases = testCases;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public String getTestSuiteName() {
        return testSuiteName;
    }

    public void setTestSuiteName(String testSuiteName) {
        this.testSuiteName = testSuiteName;
    }

    public String getTestSuiteDescription() {
        return testSuiteDescription;
    }

    public void setTestSuiteDescription(String testSuiteDescription) {
        this.testSuiteDescription = StringUtils.normalizeSpace(testSuiteDescription);
    }
}
