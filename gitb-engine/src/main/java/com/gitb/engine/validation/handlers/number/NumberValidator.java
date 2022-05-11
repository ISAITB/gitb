package com.gitb.engine.validation.handlers.number;

import com.gitb.core.Configuration;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.AbstractValidator;
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
public class NumberValidator extends AbstractValidator {

    private final static String ACTUAL_NUMBER_ARGUMENT_NAME = "actualnumber";
    private final static String EXPECTED_NUMBER_ARGUMENT_NAME = "expectednumber";
    private final static String MODULE_DEFINITION_XML = "/validation/number-validator-definition.xml";

    public NumberValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        NumberType actualnumber = (NumberType) inputs.get(ACTUAL_NUMBER_ARGUMENT_NAME);
        NumberType expectednumber = (NumberType) inputs.get(EXPECTED_NUMBER_ARGUMENT_NAME);

        // process xpath
        BooleanType result = new BooleanType(actualnumber.doubleValue() == expectednumber.doubleValue());

        // return report
        NumberReportHandler handler = new NumberReportHandler(actualnumber, expectednumber, result);
        return handler.createReport();
    }
}
