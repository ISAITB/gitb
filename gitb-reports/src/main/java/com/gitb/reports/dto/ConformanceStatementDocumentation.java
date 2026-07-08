/*
 * Copyright (C) 2026 European Union
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

import java.util.List;

public class ConformanceStatementDocumentation {

    private String title;
    private String labelDomain;
    private String labelSpecificationGroup;
    private String labelSpecificationInGroup;
    private String labelSpecification;
    private String labelActor;
    private String testDomain;
    private String testSpecificationGroup;
    private String testSpecification;
    private String testActor;
    private String reportDate;
    private boolean includePageNumbers = true;
    private boolean includeOverview = true;
    private boolean includeStatementDocumentation = true;
    private boolean includeTestCaseListing = true;
    private boolean includeTestSuiteDocumentation = true;
    private boolean includeTestCaseDocumentation = true;
    private String statementDocumentation;
    private List<TestSuiteDocumentation> testSuites;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLabelDomain() {
        return labelDomain;
    }

    public void setLabelDomain(String labelDomain) {
        this.labelDomain = labelDomain;
    }

    public String getLabelSpecificationGroup() {
        return labelSpecificationGroup;
    }

    public void setLabelSpecificationGroup(String labelSpecificationGroup) {
        this.labelSpecificationGroup = labelSpecificationGroup;
    }

    public String getLabelSpecificationInGroup() {
        return labelSpecificationInGroup;
    }

    public void setLabelSpecificationInGroup(String labelSpecificationInGroup) {
        this.labelSpecificationInGroup = labelSpecificationInGroup;
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

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public boolean getIncludePageNumbers() {
        return includePageNumbers;
    }

    public void setIncludePageNumbers(boolean includePageNumbers) {
        this.includePageNumbers = includePageNumbers;
    }

    public boolean getIncludeOverview() {
        return includeOverview;
    }

    public void setIncludeOverview(boolean includeOverview) {
        this.includeOverview = includeOverview;
    }

    public boolean getIncludeStatementDocumentation() {
        return includeStatementDocumentation;
    }

    public void setIncludeStatementDocumentation(boolean includeStatementDocumentation) {
        this.includeStatementDocumentation = includeStatementDocumentation;
    }

    public boolean getIncludeTestCaseListing() {
        return includeTestCaseListing;
    }

    public void setIncludeTestCaseListing(boolean includeTestCaseListing) {
        this.includeTestCaseListing = includeTestCaseListing;
    }

    public boolean getIncludeTestSuiteDocumentation() {
        return includeTestSuiteDocumentation;
    }

    public void setIncludeTestSuiteDocumentation(boolean includeTestSuiteDocumentation) {
        this.includeTestSuiteDocumentation = includeTestSuiteDocumentation;
    }

    public boolean getIncludeTestCaseDocumentation() {
        return includeTestCaseDocumentation;
    }

    public void setIncludeTestCaseDocumentation(boolean includeTestCaseDocumentation) {
        this.includeTestCaseDocumentation = includeTestCaseDocumentation;
    }

    public String getStatementDocumentation() {
        return statementDocumentation;
    }

    public void setStatementDocumentation(String statementDocumentation) {
        this.statementDocumentation = statementDocumentation;
    }

    public List<TestSuiteDocumentation> getTestSuites() {
        return testSuites;
    }

    public void setTestSuites(List<TestSuiteDocumentation> testSuites) {
        this.testSuites = testSuites;
    }

    public boolean hasAnySuiteOrCaseDocumentation() {
        if (testSuites == null) {
            return false;
        }
        return testSuites.stream().anyMatch(suite ->
                (includeTestSuiteDocumentation && suite.getDocumentation() != null)
                || (includeTestCaseDocumentation && suite.getTestCases() != null && suite.getTestCases().stream().anyMatch(tc -> tc.getDocumentation() != null))
        );
    }
}
