package com.gitb.engine.actors.processors;

import akka.actor.ActorRef;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.Log;
import com.gitb.types.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

public class LogStepProcessorActor extends AbstractTestStepActor<Log> {

    private static final Logger LOG = LoggerFactory.getLogger(LogStepProcessorActor.class);
    public static final String NAME = "log-p";

    public LogStepProcessorActor(Log step, TestCaseScope scope, String stepId) {
        super(step, scope, stepId);
    }

    @Override
    protected void init() {
        // Do nothing.
    }

    @Override
    protected void start() {
        processing();
        ExpressionHandler exprHandler = new ExpressionHandler(scope);
        StringType result = exprHandler.processExpression(step).toStringType();
        if (result != null) {
            LOG.debug(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), (String)result.getValue());
        }
        completed();
    }

    @Override
    protected void stop() {
        // Do nothing.
    }

    public static ActorRef create(ActorContext context, Log step, TestCaseScope scope, String stepId) throws Exception{
        return create(LogStepProcessorActor.class, context, step, scope, stepId);
    }

}
