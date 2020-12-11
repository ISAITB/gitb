package com.gitb.messaging;

import com.gitb.tr.TAR;

public class DeferredMessagingReport extends MessagingReport {

    public DeferredMessagingReport() {
        super(null);
    }

    @Override
    public TAR getReport() {
        throw new UnsupportedOperationException("This is a deferred report to be received asynchronously");
    }

    @Override
    public Message getMessage() {
        throw new UnsupportedOperationException("This is a deferred report to be received asynchronously");
    }

}
