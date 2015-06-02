package com.gitb.engine.commands.interaction;

import com.gitb.engine.commands.common.SessionCommand;
import com.gitb.tbs.UserInput;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by serbay on 9/4/14.
 */
public class UserInputCommand extends SessionCommand {
	private final String stepId;
    private final List<UserInput> inputs;

	public UserInputCommand(String sessionId, String stepId, List<UserInput> inputs) {
		super(sessionId);
		this.stepId = stepId;
		this.inputs = new CopyOnWriteArrayList<>(inputs);
	}

	public String getStepId() {
		return stepId;
	}

	public List<UserInput> getInputs() {
		return inputs;
	}
}
