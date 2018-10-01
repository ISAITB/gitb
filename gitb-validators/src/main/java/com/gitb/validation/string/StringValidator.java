package com.gitb.validation.string;

import com.gitb.core.Configuration;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.StringType;
import com.gitb.validation.IValidationHandler;
import com.gitb.validation.common.AbstractValidator;
import org.kohsuke.MetaInfServices;

import java.util.List;
import java.util.Map;

/**
 * Created by senan
 */
@MetaInfServices(IValidationHandler.class)
public class StringValidator extends AbstractValidator{

    private final String ACTUAL_STRING_ARGUMENT_NAME   = "actualstring";
    private final String EXPECTED_STRING_ARGUMENT_NAME = "expectedstring";

    private final String MODULE_DEFINITION_XML = "/string-validator-definition.xml";

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

        // return report
        StringReportHandler handler = new StringReportHandler(actualString, expectedString, result);
        return handler.createReport();
    }
}
