package com.gitb.engine.utils;

import com.gitb.tdl.Sequence;

public record StepContext(Boolean parentStopOnError, Boolean parentStopOnChildError) {

    public static StepContext from(Sequence parentStep) {
        return new StepContext(parentStep.isStopOnError(), parentStep.isStopOnChildError());
    }

}
