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
    public static final String ACTUAL_ITEM_NAME = "actual";
    public static final String EXPECTED_ITEM_NAME = "expected";

    protected StringReportHandler(StringType actual, StringType expected, BooleanType result) {
        super();

        report.setName("String Validation");
        report.setReports(new TestAssertionGroupReportsType());

        AnyContent attachment = new AnyContent();
        attachment.setType(DataType.MAP_DATA_TYPE);

        AnyContent actualItem = new AnyContent();
        actualItem.setName(ACTUAL_ITEM_NAME);
        actualItem.setType(DataType.STRING_DATA_TYPE);
        actualItem.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        actualItem.setValue(new String(actual.serializeByDefaultEncoding()));
        attachment.getItem().add(actualItem);

        AnyContent expectedItem = new AnyContent();
        expectedItem.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        expectedItem.setType(DataType.STRING_DATA_TYPE);
        expectedItem.setName(EXPECTED_ITEM_NAME);
        expectedItem.setValue(new String(expected.serializeByDefaultEncoding()));
        attachment.getItem().add(expectedItem);

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
