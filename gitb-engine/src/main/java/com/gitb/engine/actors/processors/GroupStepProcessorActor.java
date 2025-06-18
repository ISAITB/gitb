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

package com.gitb.engine.actors.processors;

import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.tdl.Group;
import org.apache.pekko.actor.ActorRef;

/**
 * Created by serbay on 9/12/14.
 *
 * Group step executor actor
 */
public class GroupStepProcessorActor extends SequenceProcessorActor<Group> {
	public static final String NAME = "group-p";

	public GroupStepProcessorActor(Group step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
	}

	public static ActorRef create(ActorContext context, Group step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		return create(GroupStepProcessorActor.class, context, step, scope, stepId, stepContext);
	}
}
