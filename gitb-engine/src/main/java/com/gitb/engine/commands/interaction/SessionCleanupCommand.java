package com.gitb.engine.commands.interaction;

import java.io.Serializable;
import java.util.Collection;

public class SessionCleanupCommand implements Serializable {

    private final UpdateMessage sessionEndMessage;
    private final Collection<UpdateMessage> pendingUpdates;

    public SessionCleanupCommand(UpdateMessage sessionEndMessage, Collection<UpdateMessage> pendingUpdates) {
        this.sessionEndMessage = sessionEndMessage;
        this.pendingUpdates = pendingUpdates;
    }

    public UpdateMessage getSessionEndMessage() {
        return sessionEndMessage;
    }

    public Collection<UpdateMessage> getPendingUpdates() {
        return pendingUpdates;
    }
}
