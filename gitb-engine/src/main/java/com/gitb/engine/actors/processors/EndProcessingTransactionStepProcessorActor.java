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
import com.gitb.engine.processing.ProcessingContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.EndProcessingTransaction;

public class EndProcessingTransactionStepProcessorActor extends AbstractTestStepActor<EndProcessingTransaction> {

    public static final String NAME = "eptxn-p";

    public EndProcessingTransactionStepProcessorActor(EndProcessingTransaction step, TestCaseScope scope, String stepId, StepContext stepContext) {
        super(step, scope, stepId, stepContext);
    }

    public static ActorRef create(ActorContext context, EndProcessingTransaction step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
        return create(EndProcessingTransactionStepProcessorActor.class, context, step, scope, stepId, stepContext);
    }

    @Override
    protected void init() throws Exception {
        // Do nothing.
    }

    @Override
    protected void start() throws Exception {
        processing();
        ProcessingContext processingContext = this.scope.getContext().getProcessingContext(step.getTxnId());
        processingContext.getHandler().endTransaction(processingContext.getSession(), step.getId());
        this.scope.getContext().removeProcessingContext(step.getTxnId());
        completed();
    }

}
