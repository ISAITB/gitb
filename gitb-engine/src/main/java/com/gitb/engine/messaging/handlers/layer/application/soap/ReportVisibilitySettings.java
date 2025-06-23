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

package com.gitb.engine.messaging.handlers.layer.application.soap;

import com.gitb.engine.messaging.handlers.layer.application.BaseReportVisibilitySettings;
import com.gitb.messaging.Message;
import com.gitb.tr.TAR;

import java.util.Collections;
import java.util.Set;

import static com.gitb.engine.messaging.handlers.layer.application.soap.SoapMessagingHandlerV2.*;

public class ReportVisibilitySettings extends BaseReportVisibilitySettings {

    private final static String SHOW_REQUEST_URI_ARGUMENT_NAME = "showRequestUri";
    private final static String SHOW_REQUEST_BODY_ARGUMENT_NAME = "showRequestBody";
    private final static String SHOW_REQUEST_ENVELOPE_ARGUMENT_NAME = "showRequestEnvelope";
    private final static String SHOW_REQUEST_HEADERS_ARGUMENT_NAME = "showRequestHeaders";
    private final static String SHOW_REQUEST_ATTACHMENTS_ARGUMENT_NAME = "showRequestAttachments";
    private final static String SHOW_RESPONSE_BODY_ARGUMENT_NAME = "showResponseBody";
    private final static String SHOW_RESPONSE_ATTACHMENTS_ARGUMENT_NAME = "showResponseAttachments";
    private final static String SHOW_RESPONSE_ENVELOPE_ARGUMENT_NAME = "showResponseEnvelope";
    private final static String SHOW_RESPONSE_STATUS_ARGUMENT_NAME = "showResponseStatus";
    private final static String SHOW_RESPONSE_HEADERS_ARGUMENT_NAME = "showResponseHeaders";
    private final static String SHOW_RESPONSE_ERROR_ARGUMENT_NAME = "showResponseError";
    private final static String REQUEST_HEADERS_TO_SHOW_ARGUMENT_NAME = "requestHeadersToShow";
    private final static String REQUEST_HEADERS_TO_HIDE_ARGUMENT_NAME = "requestHeadersToHide";
    private final static String REQUEST_ATTACHMENTS_TO_SHOW_ARGUMENT_NAME = "requestAttachmentsToShow";
    private final static String REQUEST_ATTACHMENTS_TO_HIDE_ARGUMENT_NAME = "requestAttachmentsToHide";
    private final static String RESPONSE_HEADERS_TO_SHOW_ARGUMENT_NAME = "responseHeadersToShow";
    private final static String RESPONSE_HEADERS_TO_HIDE_ARGUMENT_NAME = "responseHeadersToHide";
    private final static String RESPONSE_ATTACHMENTS_TO_SHOW_ARGUMENT_NAME = "responseAttachmentsToShow";
    private final static String RESPONSE_ATTACHMENTS_TO_HIDE_ARGUMENT_NAME = "responseAttachmentsToHide";


    private final boolean showRequestUri;
    private final boolean showRequestEnvelope;
    private final boolean showRequestBody;
    private final boolean showRequestAttachments;
    private final boolean showRequestHeaders;
    private final boolean showResponseBody;
    private final boolean showResponseStatus;
    private final boolean showResponseHeaders;
    private final boolean showResponseEnvelope;
    private final boolean showResponseAttachments;
    private final boolean showResponseError;
    private final Set<String> requestHeadersToShow;
    private final Set<String> requestHeadersToHide;
    private final Set<String> requestAttachmentsToShow;
    private final Set<String> requestAttachmentsToHide;
    private final Set<String> responseHeadersToShow;
    private final Set<String> responseHeadersToHide;
    private final Set<String> responseAttachmentsToShow;
    private final Set<String> responseAttachmentsToHide;

    public ReportVisibilitySettings(Message message) {
        showRequestUri = parseFlag(SHOW_REQUEST_URI_ARGUMENT_NAME, message);
        showRequestEnvelope = parseFlag(SHOW_REQUEST_ENVELOPE_ARGUMENT_NAME, message);
        showRequestBody = parseFlag(SHOW_REQUEST_BODY_ARGUMENT_NAME, message);
        showRequestHeaders = parseFlag(SHOW_REQUEST_HEADERS_ARGUMENT_NAME, message);
        showRequestAttachments = parseFlag(SHOW_REQUEST_ATTACHMENTS_ARGUMENT_NAME, message);
        showResponseBody = parseFlag(SHOW_RESPONSE_BODY_ARGUMENT_NAME, message);
        showResponseHeaders = parseFlag(SHOW_RESPONSE_HEADERS_ARGUMENT_NAME, message);
        showResponseAttachments = parseFlag(SHOW_RESPONSE_ATTACHMENTS_ARGUMENT_NAME, message);
        showResponseError = parseFlag(SHOW_RESPONSE_ERROR_ARGUMENT_NAME, message);
        showResponseStatus = parseFlag(SHOW_RESPONSE_STATUS_ARGUMENT_NAME, message);
        showResponseEnvelope = parseFlag(SHOW_RESPONSE_ENVELOPE_ARGUMENT_NAME, message);
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
        if (showRequestAttachments) {
            requestAttachmentsToShow = parseSet(REQUEST_ATTACHMENTS_TO_SHOW_ARGUMENT_NAME, message);
            requestAttachmentsToHide = parseSet(REQUEST_ATTACHMENTS_TO_HIDE_ARGUMENT_NAME, message);
        } else {
            requestAttachmentsToShow = Collections.emptySet();
            requestAttachmentsToHide = Collections.emptySet();
        }
        if (showResponseAttachments) {
            responseAttachmentsToShow = parseSet(RESPONSE_ATTACHMENTS_TO_SHOW_ARGUMENT_NAME, message);
            responseAttachmentsToHide = parseSet(RESPONSE_ATTACHMENTS_TO_HIDE_ARGUMENT_NAME, message);
        } else {
            responseAttachmentsToShow = Collections.emptySet();
            responseAttachmentsToHide = Collections.emptySet();
        }
    }

    @Override
    protected void apply(TAR report) {
        if (report != null && report.getContext() != null) {
            if (!showRequestBody || !showRequestEnvelope || !showRequestUri
                    || (!showRequestHeaders || !requestHeadersToShow.isEmpty() || !requestHeadersToHide.isEmpty())
                    || (!showRequestAttachments || !requestAttachmentsToShow.isEmpty() || !requestAttachmentsToHide.isEmpty())) {
                // Request.
                report.getContext().getItem().stream()
                        .filter(item -> REPORT_ITEM_REQUEST.equalsIgnoreCase(item.getName()))
                        .findAny()
                        .ifPresent((requestItem) -> {
                            if (!showRequestBody) {
                                hideItem(REPORT_ITEM_BODY, requestItem);
                            }
                            if (!showRequestEnvelope) {
                                hideItem(REPORT_ITEM_ENVELOPE, requestItem);
                            }
                            if (!showRequestUri) {
                                hideItem(REPORT_ITEM_URI, requestItem);
                            }
                            hideMapItems(showRequestHeaders, REPORT_ITEM_HEADERS, requestHeadersToShow, requestHeadersToHide, requestItem);
                            hideMapItems(showRequestAttachments, REPORT_ITEM_ATTACHMENTS, requestAttachmentsToShow, requestAttachmentsToHide, requestItem);
                            // If no visible children are visible hide overall map.
                            if (allChildrenHidden(requestItem)) {
                                requestItem.setForDisplay(false);
                            }
                        });
            }
            if (!showResponseBody || !showResponseEnvelope || !showResponseStatus || !showResponseError
                    || (!showResponseHeaders || !responseHeadersToShow.isEmpty() || !responseHeadersToHide.isEmpty())
                    || (!showResponseAttachments || !responseAttachmentsToShow.isEmpty() || !responseAttachmentsToHide.isEmpty())) {
                // Response.
                report.getContext().getItem().stream()
                        .filter(item -> REPORT_ITEM_RESPONSE.equalsIgnoreCase(item.getName()))
                        .findAny()
                        .ifPresent((responseItem) -> {
                            if (!showResponseBody) {
                                hideItem(REPORT_ITEM_BODY, responseItem);
                            }
                            if (!showResponseEnvelope) {
                                hideItem(REPORT_ITEM_ENVELOPE, responseItem);
                            }
                            if (!showResponseStatus) {
                                hideItem(REPORT_ITEM_STATUS, responseItem);
                            }
                            if (!showResponseError) {
                                hideItem(REPORT_ITEM_ERROR, responseItem);
                            }
                            hideMapItems(showResponseHeaders, REPORT_ITEM_HEADERS, responseHeadersToShow, responseHeadersToHide, responseItem);
                            hideMapItems(showResponseAttachments, REPORT_ITEM_ATTACHMENTS, responseAttachmentsToShow, responseAttachmentsToHide, responseItem);
                            // If no visible children are visible hide overall map.
                            if (allChildrenHidden(responseItem)) {
                                responseItem.setForDisplay(false);
                            }
                        });
            }
        }
    }

}
