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

import com.gitb.core.StepStatus;
import com.gitb.engine.PropertyConstants;
import com.gitb.engine.events.model.StatusEvent;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.tdl.Sequence;
import com.gitb.tr.TestResultType;
import com.gitb.types.BooleanType;
import org.apache.pekko.actor.ActorRef;

public class RootSequenceProcessorActor<T extends Sequence> extends SequenceProcessorActor<T> {

    public RootSequenceProcessorActor(T sequence, TestCaseScope scope, String stepId, StepContext stepContext) {
        super(sequence, scope, stepId, stepContext);
        // Set overall test status to success to begin with.
        var variable = scope.createVariable(PropertyConstants.TEST_SUCCESS);
        variable.setValue(new BooleanType(true));
    }

    public static ActorRef create(ActorContext context, Sequence step, TestCaseScope scope, String stepId) throws Exception {
        return create(RootSequenceProcessorActor.class, context, step, scope, stepId, null);
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
