package com.gitb.processing;

import com.gitb.tr.TAR;

public class ProcessingReport {

    private final TAR report;
    private final ProcessingData data;

    public ProcessingReport(TAR report) {
        this(report, null);
    }

    public ProcessingReport(TAR report, ProcessingData data) {
        this.report = report;
        this.data = data;
    }

    public TAR getReport() {
        return report;
    }

    public ProcessingData getData() {
        return data;
    }

}