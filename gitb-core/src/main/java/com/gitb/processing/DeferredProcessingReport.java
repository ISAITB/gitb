package com.gitb.processing;

import com.gitb.tr.TAR;

public class DeferredProcessingReport extends ProcessingReport {

    private final long delayToApply;

    public DeferredProcessingReport(TAR report, long delayToApply) {
        this(report, null, delayToApply);
    }

    public DeferredProcessingReport(TAR report, ProcessingData data, long delayToApply) {
        super(report, data);
        this.delayToApply = delayToApply;
    }

    public long getDelayToApply() {
        return delayToApply;
    }
}
