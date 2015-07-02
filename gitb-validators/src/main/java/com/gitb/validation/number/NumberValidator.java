package com.gitb.validation.number;

import java.util.List;
import java.util.Map;

import org.kohsuke.MetaInfServices;

import com.gitb.core.Configuration;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.NumberType;
import com.gitb.validation.IValidationHandler;
import com.gitb.validation.common.AbstractValidator;

/**
 * Created by Roch Bertucat
 */
@MetaInfServices(IValidationHandler.class)
public class NumberValidator extends AbstractValidator {

    private final String ACTUAL_NUMBER_ARGUMENT_NAME = "actualnumber";
    private final String EXPECTED_NUMBER_ARGUMENT_NAME = "expectednumber";

    private final String MODULE_DEFINITION_XML = "/number-validator-definition.xml";

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
