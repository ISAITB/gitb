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

package com.gitb.messaging;

import com.gitb.messaging.callback.CallbackData;
import com.gitb.tr.TAR;

import java.util.concurrent.CompletableFuture;

public class DeferredMessagingReport extends MessagingReport {

    private final CompletableFuture<MessagingReport> deferredReport;
    private final CallbackData callbackData;

    public DeferredMessagingReport() {
        this(null, null);
    }

    public DeferredMessagingReport(CompletableFuture<MessagingReport> deferredReport) {
        this(deferredReport, null);
    }

    public DeferredMessagingReport(CallbackData callbackData) {
        this(null, callbackData);
    }

    private DeferredMessagingReport(CompletableFuture<MessagingReport> deferredReport, CallbackData callbackData) {
        super(null);
        this.deferredReport = deferredReport;
        this.callbackData = callbackData;

    }

    @Override
    public TAR getReport() {
        throw new UnsupportedOperationException("This is a deferred report to be received asynchronously");
    }

    @Override
    public Message getMessage() {
        throw new UnsupportedOperationException("This is a deferred report to be received asynchronously");
    }

    public CompletableFuture<MessagingReport> getDeferredReport() {
        return deferredReport;
    }

    public CallbackData getCallbackData() {
        return callbackData;
    }
}
