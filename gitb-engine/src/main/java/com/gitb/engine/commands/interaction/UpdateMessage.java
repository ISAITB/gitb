package com.gitb.engine.commands.interaction;

import com.gitb.tbs.TestStepStatus;

import java.util.UUID;

public class UpdateMessage {

    String uuid;
    private final TestStepStatus statusMessage;

    public UpdateMessage(TestStepStatus statusMessage) {
        this.statusMessage = statusMessage;
        this.uuid = UUID.randomUUID().toString();
    }

    public String getUuid() {
        return uuid;
    }

    public TestStepStatus getStatusMessage() {
        return statusMessage;
    }
}
