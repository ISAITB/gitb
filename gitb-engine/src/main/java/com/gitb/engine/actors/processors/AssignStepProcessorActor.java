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

import com.gitb.engine.utils.StepContext;
import org.apache.pekko.actor.ActorRef;
import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.processors.AssignProcessor;
import com.gitb.engine.processors.IProcessor;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.Assign;

/**
 * Created by serbay on 9/10/14.
 *
 * Assign step executor actor
 */
public class AssignStepProcessorActor extends AbstractProcessorActor<Assign> {

	public static final String NAME = "assign-p";

	private AssignProcessor processor;

	public AssignStepProcessorActor(Assign step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
		initialize();
	}

	protected void initialize() {
		processor = new AssignProcessor(scope);
	}

	@Override
	protected IProcessor getProcessor() {
		return processor;
	}

	public static ActorRef create(ActorContext context, Assign step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception{
		return context.actorOf(props(AssignStepProcessorActor.class, step, scope, stepId, stepContext).withDispatcher(ActorSystem.BLOCKING_DISPATCHER), getName(NAME));
	}
}
