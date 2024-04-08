package com.gitb.engine.actors.processors;

import org.apache.pekko.actor.ActorRef;
import com.gitb.engine.processing.ProcessingContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.EndProcessingTransaction;

public class EndProcessingTransactionStepProcessorActor extends AbstractTestStepActor<EndProcessingTransaction> {

    public static final String NAME = "eptxn-p";

    public EndProcessingTransactionStepProcessorActor(EndProcessingTransaction step, TestCaseScope scope, String stepId) {
        super(step, scope, stepId);
    }

    public static ActorRef create(ActorContext context, EndProcessingTransaction step, TestCaseScope scope, String stepId) throws Exception {
        return create(EndProcessingTransactionStepProcessorActor.class, context, step, scope, stepId);
    }

    @Override
    protected void init() throws Exception {

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
