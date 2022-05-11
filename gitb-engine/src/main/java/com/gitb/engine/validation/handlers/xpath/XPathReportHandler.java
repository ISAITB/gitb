package com.gitb.engine.validation.handlers.xpath;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.validation.handlers.common.AbstractReportHandler;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestResultType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.ObjectType;
import com.gitb.types.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by root on 20.02.2015.
 */
public class XPathReportHandler extends AbstractReportHandler {
    private static final Logger logger = LoggerFactory.getLogger(XPathReportHandler.class);

    public static final String XML_ITEM_NAME    = "xml";
    public static final String XPATH_ITEM_NAME  = "xpath";

    protected XPathReportHandler(ObjectType content, StringType xpath, BooleanType result) {
        super();

        report.setName("XPath Validation");
        report.setReports(new TestAssertionGroupReportsType());

	    AnyContent attachment = new AnyContent();
	    attachment.setType(DataType.MAP_DATA_TYPE);

	    AnyContent xml = new AnyContent();
	    xml.setName(XML_ITEM_NAME);
	    xml.setType(DataType.OBJECT_DATA_TYPE);
	    xml.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
	    xml.setValue(new String(content.serializeByDefaultEncoding()));
	    attachment.getItem().add(xml);

	    AnyContent schema = new AnyContent();
	    schema.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
	    schema.setType(DataType.SCHEMA_DATA_TYPE);
	    schema.setName(XPATH_ITEM_NAME);
	    schema.setValue(new String(xpath.serializeByDefaultEncoding()));
	    attachment.getItem().add(schema);

	    if((boolean) result.getValue()) {
		    report.setResult(TestResultType.SUCCESS);
	    } else {
		    report.setResult(TestResultType.FAILURE);
	    }
	    report.setContext(attachment);
    }

    @Override
    public TAR createReport() {
        return report;
    }
}
