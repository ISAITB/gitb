package com.gitb.engine.commands.interaction;

import com.gitb.core.StepStatus;
import com.gitb.engine.commands.common.SessionCommand;
import com.gitb.tr.TestStepReportType;

public class TestSessionFinishedCommand extends SessionCommand {

	private final StepStatus status;
	private final TestStepReportType resultReport;

	public TestSessionFinishedCommand(String sessionId, StepStatus status, TestStepReportType resultReport) {
		super(sessionId);
		this.status = status;
		this.resultReport = resultReport;
	}

	public StepStatus getStatus() {
		return status;
	}

	public TestStepReportType getResultReport() {
		return resultReport;
	}
}
