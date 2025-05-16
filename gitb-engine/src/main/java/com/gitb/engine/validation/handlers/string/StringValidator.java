package com.gitb.engine.validation.handlers.string;

import com.gitb.core.Configuration;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.SimpleValidator;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.StringType;

import java.util.List;
import java.util.Map;

/**
 * Created by senan
 */
@ValidationHandler(name="StringValidator")
public class StringValidator extends SimpleValidator {

    private static final String ACTUAL_STRING_ARGUMENT_NAME   = "actualstring";
    private static final String EXPECTED_STRING_ARGUMENT_NAME = "expectedstring";
    private static final String MODULE_DEFINITION_XML = "/validation/string-validator-definition.xml";

    public StringValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        StringType actualString    = (StringType)(inputs.get(ACTUAL_STRING_ARGUMENT_NAME)).convertTo(DataType.STRING_DATA_TYPE);
        StringType expectedString  = (StringType)(inputs.get(EXPECTED_STRING_ARGUMENT_NAME)).convertTo(DataType.STRING_DATA_TYPE);

        String actualStringValue   = (String) actualString.getValue();
        String expectedStringValue = (String) expectedString.getValue();

        // process xpath
        BooleanType result = new BooleanType(actualStringValue.equals(expectedStringValue));

        return createReport(inputs, () -> new StringReportHandler(actualString, expectedString, result).createReport());
    }
}
