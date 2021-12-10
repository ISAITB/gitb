package com.gitb.engine.actors.processors;

import akka.actor.ActorRef;
import com.gitb.core.StepStatus;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.Sequence;
import com.gitb.types.BooleanType;

public class RootSequenceProcessorActor<T extends Sequence> extends SequenceProcessorActor<T> {

    public RootSequenceProcessorActor(T sequence, TestCaseScope scope, String stepId) {
        super(sequence, scope, stepId);
        // Set overall test status to success to begin with.
        var variable = scope.createVariable(TestCaseContext.TEST_SUCCESS);
        variable.setValue(new BooleanType(true));
    }

    public static ActorRef create(ActorContext context, Sequence step, TestCaseScope scope, String stepId) throws Exception {
        return create(RootSequenceProcessorActor.class, context, step, scope, stepId);
    }

    @Override
    protected void handleStatusEvent(StatusEvent event) throws Exception {
        // Calculate the overall status of the test session.
        if (event.getStatus() == StepStatus.COMPLETED || event.getStatus() == StepStatus.WARNING || event.getStatus() == StepStatus.ERROR) {
            var variable = scope.getVariable(TestCaseContext.TEST_SUCCESS, true);
            BooleanType currentValue = (BooleanType) variable.getValue();
            currentValue.setValue(((Boolean)currentValue.getValue()) && (event.getStatus() == StepStatus.COMPLETED || event.getStatus() == StepStatus.WARNING));
        }
        // Handle regular processing.
        super.handleStatusEvent(event);
    }

}
