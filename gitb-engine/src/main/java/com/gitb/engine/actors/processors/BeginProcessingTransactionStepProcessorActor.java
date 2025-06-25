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

import com.gitb.core.Configuration;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.processing.ProcessingContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.tdl.BeginProcessingTransaction;
import org.apache.pekko.actor.ActorRef;

public class BeginProcessingTransactionStepProcessorActor extends AbstractTestStepActor<BeginProcessingTransaction> {

    public static final String NAME = "bptxn-p";

    public BeginProcessingTransactionStepProcessorActor(BeginProcessingTransaction step, TestCaseScope scope, String stepId, StepContext stepContext) {
        super(step, scope, stepId, stepContext);
    }

    public static ActorRef create(ActorContext context, BeginProcessingTransaction step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception {
        return create(BeginProcessingTransactionStepProcessorActor.class, context, step, scope, stepId, stepContext);
    }

    @Override
    protected void init() throws Exception {
        // Do nothing.
    }

    @Override
    protected void start() throws Exception {
        processing();

        VariableResolver resolver = new VariableResolver(scope);
        String handlerIdentifier = resolveProcessingHandler(step.getHandler(), () -> resolver);
        if (step.getConfig() != null) {
            for (Configuration config: step.getConfig()) {
                if (VariableResolver.isVariableReference(config.getValue())) {
                    config.setValue(resolver.resolveVariableAsString(config.getValue()).toString());
                }
            }
        }

        ProcessingContext context = new ProcessingContext(handlerIdentifier, TestCaseUtils.getStepProperties(step.getProperty(), resolver), scope.getContext().getSessionId());
        String session = context.getHandler().beginTransaction(step.getId(), step.getConfig());
        if (session == null || session.isBlank()) {
            session = scope.getContext().getSessionId();
        }
        context.setSession(session);
        this.scope.getContext().addProcessingContext(step.getTxnId(), context);

        completed();
    }

}
