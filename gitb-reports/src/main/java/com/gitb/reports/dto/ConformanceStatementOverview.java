package com.gitb.reports.dto;

import java.util.List;

public class ConformanceStatementOverview {

    private String title;
    private String testActor;
    private String testSpecification;
    private String testDomain;
    private String organisation;
    private String system;
    private int completedTests = 0;
    private int failedTests = 0;
    private int undefinedTests = 0;
    private String overallStatus;
    private String reportDate;
    private Boolean includeTestCases;
    private Boolean includeDetails = Boolean.TRUE;
    private Boolean includeMessage = Boolean.FALSE;
    private Boolean includeTestStatus = Boolean.TRUE;
    private Boolean includePageNumbers = Boolean.TRUE;
    private String message;
    private String labelDomain;
    private String labelSpecification;
    private String labelActor;
    private String labelOrganisation;
    private String labelSystem;
    private List<TestSuiteOverview> testSuites;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getTestDomain() {
        return testDomain;
    }

    public void setTestDomain(String testDomain) {
        this.testDomain = testDomain;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
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

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public Boolean getIncludeTestCases() {
        return includeTestCases;
    }

    public void setIncludeTestCases(Boolean includeTestCases) {
        this.includeTestCases = includeTestCases;
    }

    public List<TestSuiteOverview> getTestSuites() {
        return testSuites;
    }

    public void setTestSuites(List<TestSuiteOverview> testSuites) {
        this.testSuites = testSuites;
    }

    public Boolean getIncludeDetails() {
        return includeDetails;
    }

    public void setIncludeDetails(Boolean includeDetails) {
        this.includeDetails = includeDetails;
    }

    public Boolean getIncludeMessage() {
        return includeMessage;
    }

    public void setIncludeMessage(Boolean includeMessage) {
        this.includeMessage = includeMessage;
    }

    public Boolean getIncludeTestStatus() {
        return includeTestStatus;
    }

    public void setIncludeTestStatus(Boolean includeTestStatus) {
        this.includeTestStatus = includeTestStatus;
    }

    public Boolean getIncludePageNumbers() {
        return includePageNumbers;
    }

    public void setIncludePageNumbers(Boolean includePageNumbers) {
        this.includePageNumbers = includePageNumbers;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLabelDomain() {
        return labelDomain;
    }

    public void setLabelDomain(String labelDomain) {
        this.labelDomain = labelDomain;
    }

    public String getLabelSpecification() {
        return labelSpecification;
    }

    public void setLabelSpecification(String labelSpecification) {
        this.labelSpecification = labelSpecification;
    }

    public String getLabelActor() {
        return labelActor;
    }

    public void setLabelActor(String labelActor) {
        this.labelActor = labelActor;
    }

    public String getLabelOrganisation() {
        return labelOrganisation;
    }

    public void setLabelOrganisation(String labelOrganisation) {
        this.labelOrganisation = labelOrganisation;
    }

    public String getLabelSystem() {
        return labelSystem;
    }

    public void setLabelSystem(String labelSystem) {
        this.labelSystem = labelSystem;
    }
}
