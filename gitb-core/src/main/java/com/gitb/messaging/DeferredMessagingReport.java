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
