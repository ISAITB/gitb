package com.gitb.engine.commands.interaction;

public class UpdateSentEvent {
	private final String eventUuid;

	public UpdateSentEvent(String eventUuid) {
		this.eventUuid = eventUuid;
	}

	public String getEventUuid() {
		return eventUuid;
	}
}
