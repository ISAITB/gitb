package com.gitb.validation.xsd;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.types.DataType;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import com.gitb.validation.common.AbstractReportHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import javax.xml.bind.JAXBElement;

/**
 * Created by senan on 10/10/14.
 */
public class XSDReportHandler extends AbstractReportHandler implements ErrorHandler {

	private static final Logger logger = LoggerFactory.getLogger(XSDReportHandler.class);
	public static final String XML_ITEM_NAME = "XML";
	public static final String XSD_ITEM_NAME = "XSD";

    protected XSDReportHandler(ObjectType xml, SchemaType xsd) {
        super();

	    AnyContent attachment = new AnyContent();
	    attachment.setType(DataType.MAP_DATA_TYPE);

	    AnyContent xmlAttachment = new AnyContent();
	    xmlAttachment.setName(XML_ITEM_NAME);
	    xmlAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
	    xmlAttachment.setType(DataType.OBJECT_DATA_TYPE);
	    xmlAttachment.setValue(new String(xml.serializeByDefaultEncoding()));
	    attachment.getItem().add(xmlAttachment);

	    AnyContent xsdAttachment = new AnyContent();
	    xsdAttachment.setName(XSD_ITEM_NAME);
	    xmlAttachment.setType(DataType.SCHEMA_DATA_TYPE);
	    xsdAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
	    xsdAttachment.setValue(new String(xsd.serializeByDefaultEncoding()));
	    attachment.getItem().add(xsdAttachment);

        report.setName("XML Schema Validation");
        report.setReports(new TestAssertionGroupReportsType());
	    report.setContext(attachment);
    }

    @Override
    public TAR createReport() {
        //Report is filled by ErrorHandler methods
        return report;
    }

    @Override
    public void warning(SAXParseException exception) {
        logger.debug("warning: <"+exception.getLineNumber() + "," +
            exception.getColumnNumber() + ">" + exception.getMessage());

        BAR warning = new BAR();
        warning.setDescription( exception.getMessage());
	    warning.setLocation(XML_ITEM_NAME+":"+exception.getLineNumber()+":"+exception.getColumnNumber());
        JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeWarning(warning);
        report.getReports().getInfoOrWarningOrError().add(element);
    }

    @Override
    public void error(SAXParseException exception) {
        logger.debug("error: <"+exception.getLineNumber() + "," +
                exception.getColumnNumber() + ">" + exception.getMessage());

        report.setResult(TestResultType.FAILURE);
        BAR error = new BAR();
        error.setDescription(exception.getMessage());
	    error.setLocation(XML_ITEM_NAME+":"+exception.getLineNumber()+":"+exception.getColumnNumber());
	    JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeError(error);
	    report.getReports().getInfoOrWarningOrError().add(element);
    }

    @Override
    public void fatalError(SAXParseException exception){
        logger.debug("fatal error: <"+exception.getLineNumber() + "," +
                exception.getColumnNumber() + ">" + exception.getMessage());

        report.setResult(TestResultType.FAILURE);
        BAR error = new BAR();
        error.setDescription(exception.getMessage());
	    error.setLocation(XML_ITEM_NAME+":"+exception.getLineNumber()+":"+exception.getColumnNumber());
        JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeError(error);
        report.getReports().getInfoOrWarningOrError().add(element);
    }
}
