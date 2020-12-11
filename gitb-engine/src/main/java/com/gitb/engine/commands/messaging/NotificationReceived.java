package com.gitb.engine.commands.messaging;

import com.gitb.messaging.MessagingReport;

public class NotificationReceived {

    private final MessagingReport report;

    public NotificationReceived(MessagingReport report) {
        this.report = report;
    }

    public MessagingReport getReport() {
        return report;
    }
}
