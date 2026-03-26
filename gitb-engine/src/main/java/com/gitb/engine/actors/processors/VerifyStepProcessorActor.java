/*
 * Copyright (C) 2026 European Union
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

import com.gitb.engine.actors.ActorSystem;
import com.gitb.engine.processors.IProcessor;
import com.gitb.engine.processors.VerifyProcessor;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.tdl.Verify;
import org.apache.pekko.actor.ActorRef;
import scala.concurrent.ExecutionContext;

/**
 * Created by serbay on 9/10/14.
 * <p>
 * Verify test step executor actor
 */
public class VerifyStepProcessorActor extends AbstractProcessorActor<Verify> {

	public static final String NAME = "verify-p";

	private VerifyProcessor processor;

	public VerifyStepProcessorActor(Verify step, TestCaseScope scope, String stepId, StepContext stepContext) {
		super(step, scope, stepId, stepContext);
		initialize();
	}

	protected void initialize() {
		processor = new VerifyProcessor(scope);
	}

	@Override
	protected IProcessor getProcessor() {
		return processor;
	}

	public static ActorRef create(ActorContext context, Verify step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
		return create(VerifyStepProcessorActor.class, context, step, scope, stepId, stepContext);
	}

	@Override
	protected ExecutionContext stepDispatcher() {
		return getContext().getSystem().dispatchers().lookup(ActorSystem.BLOCKING_IO_DISPATCHER);
	}
}
