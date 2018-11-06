package com.gitb.validation.common;

import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.utils.XMLDateTimeUtils;

import javax.xml.datatype.DatatypeConfigurationException;

/**
 * Created by senan on 10/10/14.
 */
public abstract class AbstractReportHandler {

    protected TAR report;
    protected ObjectFactory objectFactory;

    protected AbstractReportHandler() {
        objectFactory = new ObjectFactory();
        report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        try {
            report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Exception while creating XMLGregorianCalendar", e);
        }
    }

    public abstract TAR createReport();
}
