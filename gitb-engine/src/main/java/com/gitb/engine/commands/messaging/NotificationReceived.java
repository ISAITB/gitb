package com.gitb.engine.commands.messaging;

import com.gitb.messaging.MessagingReport;

public class NotificationReceived {

    private final MessagingReport report;
    private final Exception error;

    public NotificationReceived(MessagingReport report) {
        this(report, null);
    }

    public NotificationReceived(MessagingReport report, Exception error) {
        this.report = report;
        this.error = error;
    }

    public MessagingReport getReport() {
        return report;
    }

    public Exception getError() {
        return error;
    }
}
