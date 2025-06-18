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
