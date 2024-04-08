package com.gitb.reports.dto;

import java.util.ArrayList;
import java.util.Collection;
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

    public static List<ConformanceStatementData> flattenStatements(Collection<ConformanceItem> conformanceItems) {
        List<ConformanceStatementData> conformanceStatements = new ArrayList<>();
        if (conformanceItems != null) {
            for (var item: conformanceItems) {
                addConformanceStatements(item, conformanceStatements);
            }
        }
        return conformanceStatements;
    }

    private static void addConformanceStatements(ConformanceItem item, List<ConformanceStatementData> statements) {
        if (item != null) {
            if (item.getData() != null) {
                statements.add(item.getData());
            } else if (item.getItems() != null) {
                for (var child: item.getItems()) {
                    addConformanceStatements(child, statements);
                }
            }
        }
    }

}
