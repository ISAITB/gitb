package com.gitb.messaging;

import com.gitb.tr.TAR;

/**
 * Created by tuncay on 9/2/14.
 */
public class MessagingReport {
	private final TAR report;
    private final Message message;

	public MessagingReport(TAR report) {
		this(report, null);
	}

	public MessagingReport(TAR report, Message message) {
		this.report = report;
		this.message = message;
	}

	public TAR getReport() {
		return report;
	}

	public Message getMessage() {
		return message;
	}
}
