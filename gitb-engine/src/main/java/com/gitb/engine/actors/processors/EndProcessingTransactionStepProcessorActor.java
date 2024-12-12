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
