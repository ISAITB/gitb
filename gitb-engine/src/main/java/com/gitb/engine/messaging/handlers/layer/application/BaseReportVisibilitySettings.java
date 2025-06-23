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

package com.gitb.engine.messaging.handlers.layer.application;

import com.gitb.core.AnyContent;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.tr.TAR;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.ListType;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.getAndConvert;

public abstract class BaseReportVisibilitySettings {

    public void apply(MessagingReport report) {
        if (report != null && report.getReport() != null) {
            apply(report.getReport());
            // Keep only items for display.
            DataTypeFactory.getInstance().applyFilter(report.getReport().getContext(), AnyContent::isForDisplay);
        }
    }

    protected Set<String> parseSet(String name, Message message) {
        return Optional.ofNullable(getAndConvert(message.getFragments(), name, DataType.LIST_DATA_TYPE, ListType.class))
                .map(values -> values.getElements()
                        .stream().map(v -> ((String) v.convertTo(DataType.STRING_DATA_TYPE).getValue()).toLowerCase())
                        .collect(Collectors.toUnmodifiableSet())
                ).orElseGet(Collections::emptySet);
    }

    protected boolean parseFlag(String name, Message message) {
        return Optional.ofNullable(getAndConvert(message.getFragments(), name, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(value -> (Boolean) value.getValue())
                .orElse(Boolean.TRUE);
    }

    protected void hideItem(String itemName, AnyContent parentItem) {
        parentItem.getItem().stream()
                .filter(item -> itemName.equalsIgnoreCase(item.getName()))
                .findFirst()
                .ifPresent(item -> item.setForDisplay(false));
    }

    protected boolean allChildrenHidden(AnyContent item) {
        return item.getItem().stream().noneMatch(AnyContent::isForDisplay);
    }

    protected void hideMapItems(boolean showMap, String mapItemName, Set<String> keysToShow, Set<String> keysToHide, AnyContent parentItem) {
        if (!showMap || !keysToShow.isEmpty() || !keysToHide.isEmpty()) {
            parentItem.getItem().stream()
                    .filter(item -> mapItemName.equalsIgnoreCase(item.getName()))
                    .findFirst()
                    .ifPresent(headersItem -> {
                        if (!showMap) {
                            headersItem.setForDisplay(false);
                        } else {
                            // Hide the headers we shouldn't be showing.
                            headersItem.getItem().stream()
                                    .filter(headerItem -> {
                                        if (headerItem.getName() != null) {
                                            String lowerCaseHeader = headerItem.getName().toLowerCase();
                                            return (!keysToShow.isEmpty() && !keysToShow.contains(lowerCaseHeader)) || (!keysToHide.isEmpty() && keysToHide.contains(lowerCaseHeader));
                                        } else {
                                            return false;
                                        }
                                    })
                                    .forEach(headerItem -> headerItem.setForDisplay(false));
                            // If no headers are visible hide the overall header map.
                            if (allChildrenHidden(headersItem)) {
                                headersItem.setForDisplay(false);
                            }
                        }
                    });
        }
    }

    protected abstract void apply(TAR report);

}
