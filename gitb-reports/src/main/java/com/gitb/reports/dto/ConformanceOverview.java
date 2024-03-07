package com.gitb.reports.dto;

import java.util.Collection;
import java.util.List;

public class ConformanceOverview {

    private String title;
    private String organisation;
    private String system;
    private String overallStatus;
    private String reportDate;
    private Boolean includeTestCases = Boolean.TRUE;
    private Boolean includeDetails = Boolean.TRUE;
    private Boolean includeMessage = Boolean.FALSE;
    private Boolean includeTestStatus = Boolean.TRUE;
    private Boolean includePageNumbers = Boolean.TRUE;
    private Boolean includeConformanceItems = Boolean.TRUE;
    private String message;
    private String labelDomain;
    private String labelSpecificationGroup;
    private String labelSpecificationInGroup;
    private String labelSpecification;
    private String labelActor;
    private String labelOrganisation;
    private String labelSystem;
    private String testDomain;
    private String testSpecificationGroup;
    private String testSpecification;
    private String testActor;
    private Integer completedStatements;
    private Integer failedStatements;
    private Integer undefinedStatements;
    private Collection<ConformanceItem> conformanceItems;
    private List<ConformanceStatementData> conformanceStatements;

    public String getLabelSpecificationInGroup() {
        return labelSpecificationInGroup;
    }

    public void setLabelSpecificationInGroup(String labelSpecificationInGroup) {
        this.labelSpecificationInGroup = labelSpecificationInGroup;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
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

    public Collection<ConformanceItem> getConformanceItems() {
        return conformanceItems;
    }

    public void setConformanceItems(List<ConformanceItem> conformanceItems) {
        this.conformanceItems = conformanceItems;
    }

    public String getLabelSpecificationGroup() {
        return labelSpecificationGroup;
    }

    public void setLabelSpecificationGroup(String labelSpecificationGroup) {
        this.labelSpecificationGroup = labelSpecificationGroup;
    }

    public String getTestDomain() {
        return testDomain;
    }

    public void setTestDomain(String testDomain) {
        this.testDomain = testDomain;
    }

    public String getTestSpecificationGroup() {
        return testSpecificationGroup;
    }

    public void setTestSpecificationGroup(String testSpecificationGroup) {
        this.testSpecificationGroup = testSpecificationGroup;
    }

    public String getTestSpecification() {
        return testSpecification;
    }

    public void setTestSpecification(String testSpecification) {
        this.testSpecification = testSpecification;
    }

    public String getTestActor() {
        return testActor;
    }

    public void setTestActor(String testActor) {
        this.testActor = testActor;
    }

    public Boolean getIncludeConformanceItems() {
        return includeConformanceItems;
    }

    public void setIncludeConformanceItems(Boolean includeConformanceItems) {
        this.includeConformanceItems = includeConformanceItems;
    }

    public List<ConformanceStatementData> getConformanceStatements() {
        if (conformanceStatements == null) {
            conformanceStatements = ConformanceItem.flattenStatements(conformanceItems);
        }
        return conformanceStatements;
    }

    public Integer getCompletedStatements() {
        if (completedStatements == null) {
            initialiseResultCounts();
        }
        return completedStatements;
    }

    public Integer getFailedStatements() {
        if (failedStatements == null) {
            initialiseResultCounts();
        }
        return failedStatements;
    }

    public Integer getUndefinedStatements() {
        if (undefinedStatements == null) {
            initialiseResultCounts();
        }
        return undefinedStatements;
    }

    private void initialiseResultCounts() {
        int completed = 0;
        int failed = 0;
        int undefined = 0;
        for (var data: getConformanceStatements()) {
            if ("SUCCESS".equals(data.getOverallStatus())) {
                completed += 1;
            } else if ("FAILURE".equals(data.getOverallStatus())) {
                failed += 1;
            } else {
                undefined += 1;
            }
        }
        completedStatements = completed;
        failedStatements = failed;
        undefinedStatements = undefined;
    }

    public String getStatementStatus() {
        var resultText = new StringBuilder();
        int completed = getCompletedStatements();
        int failed = getFailedStatements();
        int undefined = getUndefinedStatements();
        var total = completed + failed + undefined;
        resultText.append(completed).append(" of ").append(total).append(" statements completed");
        if (total > completed) {
            resultText.append(" (");
            if (failed > 0) {
                resultText.append(failed).append(" failed");
                if (undefined > 0) {
                    resultText.append(", ");
                }
            }
            if (undefined > 0) {
                resultText.append(undefined).append(" incomplete");
            }
            resultText.append(")");
        }
        return resultText.toString();
    }

}
