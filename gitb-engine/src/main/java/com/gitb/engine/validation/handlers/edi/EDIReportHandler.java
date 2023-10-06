package com.gitb.engine.validation.handlers.edi;

import jakarta.xml.bind.JAXBElement;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.BAR;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestAssertionReportType;
import com.gitb.tr.TestResultType;
import com.gitb.types.DataType;
import com.gitb.types.StringType;
import com.gitb.engine.validation.handlers.common.AbstractReportHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EDIReportHandler extends AbstractReportHandler{

    private static final Logger logger = LoggerFactory.getLogger(EDIReportHandler.class);

	public static final String EDI_ITEM_NAME = "EDI";

    protected EDIReportHandler(StringType edi) {
        super();

        AnyContent attachment = new AnyContent();
	    attachment.setType(DataType.MAP_DATA_TYPE);

	    AnyContent ediAttachment = new AnyContent();
	    ediAttachment.setName(EDI_ITEM_NAME);
	    ediAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
	    ediAttachment.setType(StringType.STRING_DATA_TYPE);
	    ediAttachment.setValue((String)edi.getValue());
	    attachment.getItem().add(ediAttachment);
	    
	    report.setName("EDI Document Validation");
        report.setReports(new TestAssertionGroupReportsType());
	    report.setContext(attachment);
    }

    public TAR createReport() {
        //Report is filled by ErrorHandler methods
        return report;
    }

    public void addError(int result, String errorMessage) {
        report.setResult(TestResultType.FAILURE);
        BAR error = new BAR();
        error.setDescription(errorMessage);
        error.setLocation(EDI_ITEM_NAME+":"+(-result/100)+":"+(-result%100));
	    JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeError(error);
	    report.getReports().getInfoOrWarningOrError().add(element);
    }

    public void addError(String errorMessage) {
        report.setResult(TestResultType.FAILURE);
        BAR error = new BAR();
        error.setDescription(errorMessage);
        JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeError(error);
        report.getReports().getInfoOrWarningOrError().add(element);
    }
}