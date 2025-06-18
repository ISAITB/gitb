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

package com.gitb.reports.dto.tar;

import java.util.ArrayList;
import java.util.List;

public class Report {

    private String title;
    private String reportDate;
    private String reportResult;
    private int errorCount;
    private int warningCount;
    private int messageCount;
    private List<ReportItem> reportItems = new ArrayList<>();
    private List<ContextItem> contextItems = new ArrayList<>();

    public Report() {
    }

    public Report(String title, String reportDate, String reportResult, int errorCount, int warningCount, int messageCount) {
        this.title = title;
        this.reportDate = reportDate;
        this.reportResult = reportResult;
        this.errorCount = errorCount;
        this.warningCount = warningCount;
        this.messageCount = messageCount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getReportResult() {
        return reportResult;
    }

    public void setReportResult(String reportResult) {
        this.reportResult = reportResult;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public List<ReportItem> getReportItems() {
        return reportItems;
    }

    public void setReportItems(List<ReportItem> reportItems) {
        this.reportItems = reportItems;
    }

    public List<ContextItem> getContextItems() {
        return contextItems;
    }

    public void setContextItems(List<ContextItem> contextItems) {
        this.contextItems = contextItems;
    }
}
