package com.gitb;

public interface StepHandler {

    /**
     * Whether this handler is a remote one (as opposed to embedded).
     *
     * @return true for a remote handler.
     */
    default boolean isRemote() {
        return false;
    }

}
