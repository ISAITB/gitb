package com.gitb.validation.number;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestResultType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.NumberType;
import com.gitb.validation.common.AbstractReportHandler;

/**
 * Created by Roch Bertucat
 */
public class NumberReportHandler extends AbstractReportHandler {

    public static final String ACTUALNUMBER_ITEM_NAME = "actual";
    public static final String EXPECTEDNUMBER_ITEM_NAME = "expected";

    protected NumberReportHandler(NumberType actualnumber, NumberType expectednumber, BooleanType result) {
        super();

        report.setName("Number Validation");
        report.setReports(new TestAssertionGroupReportsType());

        AnyContent attachment = new AnyContent();
        attachment.setType(DataType.MAP_DATA_TYPE);

        AnyContent xml = new AnyContent();
        xml.setName(ACTUALNUMBER_ITEM_NAME);
        xml.setType(DataType.NUMBER_DATA_TYPE);
        xml.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        xml.setValue(new String(actualnumber.serializeByDefaultEncoding()));
        attachment.getItem().add(xml);

        AnyContent schema = new AnyContent();
        schema.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        schema.setType(DataType.NUMBER_DATA_TYPE);
        schema.setName(EXPECTEDNUMBER_ITEM_NAME);
        schema.setValue(new String(expectednumber.serializeByDefaultEncoding()));
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
