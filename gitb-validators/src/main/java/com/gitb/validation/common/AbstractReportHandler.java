package com.gitb.validation.common;

import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.utils.XMLDateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.UUID;

/**
 * Created by senan on 10/10/14.
 */
public abstract class AbstractReportHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractReportHandler.class);

    protected TAR report;
    protected ObjectFactory objectFactory;

    protected AbstractReportHandler() {
        objectFactory = new ObjectFactory();
        report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        try {
            report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
        } catch (DatatypeConfigurationException e) {
            logger.error("Exception while creating XMLGregorianCalendar");
        }
    }

    public abstract TAR createReport();
}
