package com.gitb.engine.actors.processors;

import com.gitb.core.StepStatus;
import com.gitb.engine.PropertyConstants;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.Sequence;
import com.gitb.tr.TestResultType;
import com.gitb.types.BooleanType;
import org.apache.pekko.actor.ActorRef;

public class RootSequenceProcessorActor<T extends Sequence> extends SequenceProcessorActor<T> {

    public RootSequenceProcessorActor(T sequence, TestCaseScope scope, String stepId) {
        super(sequence, scope, stepId, true);
        // Set overall test status to success to begin with.
        var variable = scope.createVariable(PropertyConstants.TEST_SUCCESS);
        variable.setValue(new BooleanType(true));
    }

    public static ActorRef create(ActorContext context, Sequence step, TestCaseScope scope, String stepId) throws Exception {
        return create(RootSequenceProcessorActor.class, context, step, scope, stepId);
    }

    @Override
    protected void handleStatusEvent(StatusEvent event) throws Exception {
        // Calculate the overall status of the test session.
        if (event.getStatus() == StepStatus.COMPLETED || event.getStatus() == StepStatus.WARNING || event.getStatus() == StepStatus.ERROR) {
            var variable = scope.getVariable(PropertyConstants.TEST_SUCCESS, true);
            BooleanType currentValue = (BooleanType) variable.getValue();
            TestResultType forcedResult = scope.getContext().getForcedFinalResult();
            if (forcedResult == TestResultType.SUCCESS) {
                currentValue.setValue(true);
            } else {
                currentValue.setValue(forcedResult == null && ((Boolean)currentValue.getValue()) && (event.getStatus() == StepStatus.COMPLETED || event.getStatus() == StepStatus.WARNING));
            }
        }
        // Handle regular processing.
        super.handleStatusEvent(event);
    }

}
