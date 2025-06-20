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

package com.gitb.engine.messaging.handlers.layer.application.http;

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

import static com.gitb.engine.messaging.handlers.layer.application.http.HttpMessagingHandlerV2.*;
import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.getAndConvert;

public class ReportVisibilitySettings {

    private final static String SHOW_REQUEST_URI_ARGUMENT_NAME = "showRequestUri";
    private final static String SHOW_REQUEST_METHOD_ARGUMENT_NAME = "showRequestMethod";
    private final static String SHOW_REQUEST_BODY_ARGUMENT_NAME = "showRequestBody";
    private final static String SHOW_REQUEST_HEADERS_ARGUMENT_NAME = "showRequestHeaders";
    private final static String REQUEST_HEADERS_TO_SHOW_ARGUMENT_NAME = "requestHeadersToShow";
    private final static String REQUEST_HEADERS_TO_HIDE_ARGUMENT_NAME = "requestHeadersToHide";
    private final static String SHOW_RESPONSE_BODY_ARGUMENT_NAME = "showResponseBody";
    private final static String SHOW_RESPONSE_STATUS_ARGUMENT_NAME = "showResponseStatus";
    private final static String SHOW_RESPONSE_HEADERS_ARGUMENT_NAME = "showResponseHeaders";
    private final static String RESPONSE_HEADERS_TO_SHOW_ARGUMENT_NAME = "responseHeadersToShow";
    private final static String RESPONSE_HEADERS_TO_HIDE_ARGUMENT_NAME = "responseHeadersToHide";

    private final boolean showRequestUri;
    private final boolean showRequestMethod;
    private final boolean showRequestBody;
    private final boolean showRequestHeaders;
    private final boolean showResponseBody;
    private final boolean showResponseStatus;
    private final boolean showResponseHeaders;
    private final Set<String> requestHeadersToShow;
    private final Set<String> requestHeadersToHide;
    private final Set<String> responseHeadersToShow;
    private final Set<String> responseHeadersToHide;

    public ReportVisibilitySettings(Message message) {
        showRequestUri = parseFlag(SHOW_REQUEST_URI_ARGUMENT_NAME, message);
        showRequestMethod = parseFlag(SHOW_REQUEST_METHOD_ARGUMENT_NAME, message);
        showRequestBody = parseFlag(SHOW_REQUEST_BODY_ARGUMENT_NAME, message);
        showRequestHeaders = parseFlag(SHOW_REQUEST_HEADERS_ARGUMENT_NAME, message);
        showResponseBody = parseFlag(SHOW_RESPONSE_BODY_ARGUMENT_NAME, message);
        showResponseHeaders = parseFlag(SHOW_RESPONSE_HEADERS_ARGUMENT_NAME, message);
        showResponseStatus = parseFlag(SHOW_RESPONSE_STATUS_ARGUMENT_NAME, message);
        if (showRequestHeaders) {
            requestHeadersToShow = parseSet(REQUEST_HEADERS_TO_SHOW_ARGUMENT_NAME, message);
            requestHeadersToHide = parseSet(REQUEST_HEADERS_TO_HIDE_ARGUMENT_NAME, message);
        } else {
            requestHeadersToShow = Collections.emptySet();
            requestHeadersToHide = Collections.emptySet();
        }
        if (showResponseHeaders) {
            responseHeadersToShow = parseSet(RESPONSE_HEADERS_TO_SHOW_ARGUMENT_NAME, message);
            responseHeadersToHide = parseSet(RESPONSE_HEADERS_TO_HIDE_ARGUMENT_NAME, message);
        } else {
            responseHeadersToShow = Collections.emptySet();
            responseHeadersToHide = Collections.emptySet();
        }
    }

    private Set<String> parseSet(String name, Message message) {
        return Optional.ofNullable(getAndConvert(message.getFragments(), name, DataType.LIST_DATA_TYPE, ListType.class))
                .map(values -> values.getElements()
                        .stream().map(v -> ((String) v.convertTo(DataType.STRING_DATA_TYPE).getValue()).toLowerCase())
                        .collect(Collectors.toUnmodifiableSet())
                ).orElseGet(Collections::emptySet);
    }

    private boolean parseFlag(String name, Message message) {
        return Optional.ofNullable(getAndConvert(message.getFragments(), name, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(value -> (Boolean) value.getValue())
                .orElse(Boolean.TRUE);
    }

    private void hideItem(String itemName, AnyContent parentItem) {
        parentItem.getItem().stream()
                .filter(item -> itemName.equalsIgnoreCase(item.getName()))
                .findFirst()
                .ifPresent(item -> item.setForDisplay(false));
    }

    private void hideHeaderItems(boolean showHeaders, Set<String> headersToShow, Set<String> headersToHide, AnyContent parentItem) {
        if (!showHeaders || !headersToShow.isEmpty() || !headersToHide.isEmpty()) {
            parentItem.getItem().stream()
                    .filter(item -> REPORT_ITEM_HEADERS.equalsIgnoreCase(item.getName()))
                    .findFirst()
                    .ifPresent(headersItem -> {
                        if (!showHeaders) {
                            headersItem.setForDisplay(false);
                        } else {
                            // Hide the headers we shouldn't be showing.
                            headersItem.getItem().stream()
                                    .filter(headerItem -> {
                                        if (headerItem.getName() != null) {
                                            String lowerCaseHeader = headerItem.getName().toLowerCase();
                                            return (!headersToShow.isEmpty() && !headersToShow.contains(lowerCaseHeader)) || (!headersToHide.isEmpty() && headersToHide.contains(lowerCaseHeader));
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

    public void apply(MessagingReport report) {
        if (report != null) {
            apply(report.getReport());
        }
    }

    private void apply(TAR report) {
        if (report != null && report.getContext() != null) {
            if (!showRequestBody || !showRequestMethod || !showRequestUri || (!showRequestHeaders || !requestHeadersToShow.isEmpty() || !requestHeadersToHide.isEmpty())) {
                // Request.
                report.getContext().getItem().stream()
                        .filter(item -> REPORT_ITEM_REQUEST.equalsIgnoreCase(item.getName()))
                        .findAny()
                        .ifPresent((requestItem) -> {
                            if (!showRequestBody) {
                                hideItem(REPORT_ITEM_BODY, requestItem);
                            }
                            if (!showRequestMethod) {
                                hideItem(REPORT_ITEM_METHOD, requestItem);
                            }
                            if (!showRequestUri) {
                                hideItem(REPORT_ITEM_URI, requestItem);
                            }
                            hideHeaderItems(showRequestHeaders, requestHeadersToShow, requestHeadersToHide, requestItem);
                            // If no visible children are visible hide overall map.
                            if (!showRequestBody && !showRequestMethod && !showRequestUri && (!showRequestHeaders || allChildrenHidden(requestItem))) {
                                requestItem.setForDisplay(false);
                            }
                        });
                // Response.
                report.getContext().getItem().stream()
                        .filter(item -> REPORT_ITEM_RESPONSE.equalsIgnoreCase(item.getName()))
                        .findAny()
                        .ifPresent((responseItem) -> {
                            if (!showResponseBody) {
                                hideItem(REPORT_ITEM_BODY, responseItem);
                            }
                            if (!showResponseStatus) {
                                hideItem(REPORT_ITEM_STATUS, responseItem);
                            }
                            hideHeaderItems(showResponseHeaders, responseHeadersToShow, responseHeadersToHide, responseItem);
                            // If no visible children are visible hide overall map.
                            if (!showResponseBody && !showResponseStatus && (!showResponseHeaders || allChildrenHidden(responseItem))) {
                                responseItem.setForDisplay(false);
                            }
                        });
                // Keep only items for display.
                DataTypeFactory.getInstance().applyFilter(report.getContext(), AnyContent::isForDisplay);
            }
        }
    }

    private boolean allChildrenHidden(AnyContent item) {
        return item.getItem().stream().noneMatch(AnyContent::isForDisplay);
    }

}
