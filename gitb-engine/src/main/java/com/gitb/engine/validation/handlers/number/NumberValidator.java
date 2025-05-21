package com.gitb.engine.validation.handlers.number;

import com.gitb.core.Configuration;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.SimpleValidator;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.NumberType;

import java.util.List;
import java.util.Map;

/**
 * Created by Roch Bertucat
 */
@ValidationHandler(name="NumberValidator")
public class NumberValidator extends SimpleValidator {

    private final static String ACTUAL_NUMBER_ARGUMENT_NAME = "actual";
    private final static String EXPECTED_NUMBER_ARGUMENT_NAME = "expected";
    private final static String MODULE_DEFINITION_XML = "/validation/number-validator-definition.xml";

    public NumberValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        NumberType actual = getAndConvert(inputs, ACTUAL_NUMBER_ARGUMENT_NAME, DataType.NUMBER_DATA_TYPE, NumberType.class);
        NumberType expected = getAndConvert(inputs, EXPECTED_NUMBER_ARGUMENT_NAME, DataType.NUMBER_DATA_TYPE, NumberType.class);

        // process xpath
        BooleanType result = new BooleanType(actual.doubleValue() == expected.doubleValue());

        return createReport(inputs, () -> new NumberReportHandler(actual, expected, result).createReport());
    }
}
