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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConformanceItem {

    private String name;
    private String description;
    private String reportMetadata;
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

    public String getReportMetadata() {
        return reportMetadata;
    }

    public void setReportMetadata(String reportMetadata) {
        this.reportMetadata = reportMetadata;
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
