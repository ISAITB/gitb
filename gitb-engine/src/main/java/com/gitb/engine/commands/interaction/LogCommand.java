/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
