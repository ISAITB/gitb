package com.gitb.engine.commands.interaction;

import com.gitb.engine.commands.common.SessionCommand;
import com.gitb.tbs.TestStepStatus;

public class LogCommand extends SessionCommand {

	private TestStepStatus testStepStatus;

	public LogCommand(String sessionId, TestStepStatus testStepStatus) {
		super(sessionId);
		this.testStepStatus = testStepStatus;
	}

	public TestStepStatus getTestStepStatus() {
		return testStepStatus;
	}
}
