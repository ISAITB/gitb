package com.gitb.engine.actors.processors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.gitb.engine.processing.ProcessingContext;
import com.gitb.engine.processing.ProcessingManager;
import com.gitb.engine.testcase.TestCaseScope;
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

        ProcessingContext context = new ProcessingContext(step.getHandler());
        String session = context.getHandler().beginTransaction(step.getConfig());
        context.setSession(session);
        ProcessingManager.INSTANCE.setTransaction(step.getTxnId(), context);

        completed();
    }

    @Override
    protected void stop() {

    }

}