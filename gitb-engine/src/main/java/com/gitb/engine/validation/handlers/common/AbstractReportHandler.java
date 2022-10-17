package com.gitb.engine.validation.handlers.common;

import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;

/**
 * Created by senan on 10/10/14.
 */
public abstract class AbstractReportHandler {

    protected TAR report;
    protected ObjectFactory objectFactory;

    protected AbstractReportHandler() {
        objectFactory = new ObjectFactory();
        report = TestCaseUtils.createEmptyReport();
    }

    public abstract TAR createReport();
}
