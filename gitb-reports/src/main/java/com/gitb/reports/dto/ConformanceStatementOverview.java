package com.gitb.reports.dto;

import java.util.List;

public class ConformanceStatementOverview {

    private String title;
    private String testActor;
    private String testSpecification;
    private String testDomain;
    private String organisation;
    private String system;
    private String testStatus;
    private String overallStatus;
    private String reportDate;
    private Boolean includeTestCases;
    private Boolean includeDetails = Boolean.TRUE;
    private Boolean includeMessage = Boolean.FALSE;
    private Boolean includeTestStatus = Boolean.TRUE;
    private String message;
    private String labelDomain;
    private String labelSpecification;
    private String labelActor;
    private String labelOrganisation;
    private String labelSystem;
    private List<TestCaseOverview> testCases;

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
        return testStatus;
    }

    public void setTestStatus(String testStatus) {
        this.testStatus = testStatus;
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

    public List<TestCaseOverview> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseOverview> testCases) {
        this.testCases = testCases;
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
