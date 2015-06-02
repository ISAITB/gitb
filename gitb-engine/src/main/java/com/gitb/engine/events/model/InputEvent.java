package com.gitb.engine.events.model;

import com.gitb.tbs.UserInput;

import java.util.List;

/**
 * Created by tuncay on 9/23/14.
 */
public class InputEvent {
    private final String sessionId;
    private final String stepId;
    private final List<UserInput> userInputs;

    public InputEvent(String sessionId, String stepId, List<UserInput> userInputs) {
        this.sessionId = sessionId;
        this.stepId = stepId;
        this.userInputs = userInputs;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getStepId() {
        return stepId;
    }

    public List<UserInput> getUserInputs() {
        return userInputs;
    }
}
