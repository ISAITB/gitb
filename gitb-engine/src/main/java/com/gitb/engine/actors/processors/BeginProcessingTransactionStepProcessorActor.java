package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.gitb.core.Configuration;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.processing.ProcessingContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.tdl.BeginProcessingTransaction;

public class BeginProcessingTransactionStepProcessorActor extends AbstractTestStepActor<BeginProcessingTransaction> {

    public static final String NAME = "bptxn-p";

    public BeginProcessingTransactionStepProcessorActor(BeginProcessingTransaction step, TestCaseScope scope, String stepId) {
        super(step, scope, stepId);
    }

    public static ActorRef create(ActorContext context, BeginProcessingTransaction step, TestCaseScope scope, String stepId) throws Exception {
        return create(BeginProcessingTransactionStepProcessorActor.class, context, step, scope, stepId);
    }

    @Override
    protected void init() throws Exception {

    }

    @Override
    protected void start() throws Exception {
        processing();

        String handlerIdentifier = step.getHandler();
        VariableResolver resolver = new VariableResolver(scope);

        if (resolver.isVariableReference(handlerIdentifier)) {
            handlerIdentifier = resolver.resolveVariableAsString(handlerIdentifier).toString();
        }
        if (step.getConfig() != null) {
            for (Configuration config: step.getConfig()) {
                if (resolver.isVariableReference(config.getValue())) {
                    config.setValue(resolver.resolveVariableAsString(config.getValue()).toString());
                }
            }
        }

        ProcessingContext context = new ProcessingContext(handlerIdentifier, TestCaseUtils.getStepProperties(step.getProperty(), resolver));
        String session = context.getHandler().beginTransaction(step.getConfig());
        context.setSession(session);
        this.scope.getContext().addProcessingContext(step.getTxnId(), context);

        completed();
    }

}
