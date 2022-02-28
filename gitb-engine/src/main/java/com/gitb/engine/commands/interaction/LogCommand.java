package com.gitb.engine.commands.interaction;

import com.gitb.core.LogLevel;
import com.gitb.engine.commands.common.SessionCommand;
import com.gitb.tbs.TestStepStatus;

public class LogCommand extends SessionCommand {

	private final TestStepStatus testStepStatus;
	private final LogLevel logLevel;

	public LogCommand(String sessionId, TestStepStatus testStepStatus, LogLevel logLevel) {
		super(sessionId);
		this.testStepStatus = testStepStatus;
		this.logLevel = logLevel;
	}

	public TestStepStatus getTestStepStatus() {
		return testStepStatus;
	}

	public LogLevel getLogLevel() {
		return logLevel;
	}
}
