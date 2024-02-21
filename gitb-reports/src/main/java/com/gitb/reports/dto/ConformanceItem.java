package com.gitb.reports.dto;

import java.util.List;

public class ConformanceItem {

    private String name;
    private String description;
    private String overallStatus;
    private ConformanceStatementData data;
    private List<ConformanceItem> items;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public ConformanceStatementData getData() {
        return data;
    }

    public void setData(ConformanceStatementData data) {
        this.data = data;
    }

    public List<ConformanceItem> getItems() {
        return items;
    }

    public void setItems(List<ConformanceItem> items) {
        this.items = items;
    }

    public boolean isStatement() {
        return data != null;
    }
}
