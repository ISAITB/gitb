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

import java.util.List;

public class TestSuiteOverview {

    private Long testSuiteId;
    private String testSuiteName;
    private String testSuiteDescription;
    private String overallStatus;
    private String specReference;
    private String specDescription;
    private String specLink;
    private String version;
    private List<TestCaseGroup> testCaseGroups;
    private List<TestCaseOverview> testCases;

    public Long getTestSuiteId() {
        return testSuiteId;
    }

    public void setTestSuiteId(Long testSuiteId) {
        this.testSuiteId = testSuiteId;
    }

    public List<TestCaseGroup> getTestCaseGroups() {
        return testCaseGroups;
    }

    public void setTestCaseGroups(List<TestCaseGroup> testCaseGroups) {
        this.testCaseGroups = testCaseGroups;
    }

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

    public String getSpecReference() {
        return specReference;
    }

    public void setSpecReference(String specReference) {
        this.specReference = specReference;
    }

    public String getSpecDescription() {
        return specDescription;
    }

    public void setSpecDescription(String specDescription) {
        this.specDescription = specDescription;
    }

    public String getSpecLink() {
        return specLink;
    }

    public void setSpecLink(String specLink) {
        this.specLink = specLink;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
