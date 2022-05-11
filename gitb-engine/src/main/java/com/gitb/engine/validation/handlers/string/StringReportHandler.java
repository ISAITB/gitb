package com.gitb.engine.validation.handlers.string;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.validation.handlers.common.AbstractReportHandler;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestResultType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.StringType;

/**
 * Created by senan
 */
public class StringReportHandler extends AbstractReportHandler {
    public static final String ACTUALSTRING_ITEM_NAME = "actual";
    public static final String EXPECTEDSTRING_ITEM_NAME = "expected";

    protected StringReportHandler(StringType actualstring, StringType expectedstring, BooleanType result) {
        super();

        report.setName("String Validation");
        report.setReports(new TestAssertionGroupReportsType());

        AnyContent attachment = new AnyContent();
        attachment.setType(DataType.MAP_DATA_TYPE);

        AnyContent xml = new AnyContent();
        xml.setName(ACTUALSTRING_ITEM_NAME);
        xml.setType(DataType.STRING_DATA_TYPE);
        xml.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        xml.setValue(new String(actualstring.serializeByDefaultEncoding()));
        attachment.getItem().add(xml);

        AnyContent schema = new AnyContent();
        schema.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        schema.setType(DataType.STRING_DATA_TYPE);
        schema.setName(EXPECTEDSTRING_ITEM_NAME);
        schema.setValue(new String(expectedstring.serializeByDefaultEncoding()));
        attachment.getItem().add(schema);

        if ((boolean) result.getValue()) {
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
