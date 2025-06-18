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

package com.gitb.engine.events.model;

import org.apache.pekko.actor.ActorRef;
import com.gitb.core.StepStatus;
import com.gitb.engine.testcase.TestCaseScope;

/**
 * Created by serbay on 9/11/14.
 */
public class StatusEvent {
	private final StepStatus status;
	private final TestCaseScope scope;
	private final ActorRef sender;

	public StatusEvent(StepStatus status, TestCaseScope scope, ActorRef sender) {
		this.status = status;
		this.scope = scope;
		this.sender = sender;
	}

	public StepStatus getStatus() {
		return status;
	}

	public TestCaseScope getScope() {
		return scope;
	}

	public ActorRef getSender() {
		return sender;
	}

	@Override
	public String toString() {
		return "StatusEvent: " + status.name();
	}
}
