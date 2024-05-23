package com.gitb.messaging;

import com.gitb.tr.TAR;

import java.util.concurrent.CompletableFuture;

public class DeferredMessagingReport extends MessagingReport {

    private final CompletableFuture<MessagingReport> deferredReport;

    public DeferredMessagingReport() {
        this(null);
    }

    public DeferredMessagingReport(CompletableFuture<MessagingReport> deferredReport) {
        super(null);
        this.deferredReport = deferredReport;
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

}
