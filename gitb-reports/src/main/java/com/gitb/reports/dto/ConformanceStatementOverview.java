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

public class ConformanceStatementOverview extends ConformanceStatementData {

    private String title;
    private String organisation;
    private String system;
    private String reportDate;
    private Boolean includeTestCases;
    private Boolean includeDetails = Boolean.TRUE;
    private Boolean includeMessage = Boolean.FALSE;
    private Boolean includeTestStatus = Boolean.TRUE;
    private Boolean includePageNumbers = Boolean.TRUE;
    private String message;
    private String labelDomain;
    private String labelSpecificationGroup;
    private String labelSpecificationInGroup;
    private String labelSpecification;
    private String labelActor;
    private String labelOrganisation;
    private String labelSystem;

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

    public String getLabelSpecificationGroup() {
        return labelSpecificationGroup;
    }

    public void setLabelSpecificationGroup(String labelSpecificationGroup) {
        this.labelSpecificationGroup = labelSpecificationGroup;
    }
}
