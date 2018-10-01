package com.gitb.validation.string;

import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.StringType;
import com.gitb.validation.IValidationHandler;
import com.gitb.validation.common.AbstractReportHandler;
import com.gitb.validation.common.AbstractValidator;
import org.kohsuke.MetaInfServices;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by simatosc
 */
@MetaInfServices(IValidationHandler.class)
public class RegExpValidator extends AbstractValidator{

    final static String INPUT_ARGUMENT_NAME   = "input";
    final static String EXPRESSION_ARGUMENT_NAME = "expression";

    private final String MODULE_DEFINITION_XML = "/regexp-validator-definition.xml";

    public RegExpValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        String input = (String)inputs.get(INPUT_ARGUMENT_NAME).convertTo(DataType.STRING_DATA_TYPE).getValue();
        String expression = (String)inputs.get(EXPRESSION_ARGUMENT_NAME).convertTo(DataType.STRING_DATA_TYPE).getValue();
        // Process expression.
        BooleanType result = new BooleanType(Pattern.matches(expression, input));
        // Return report.
        RegExpReportHandler handler = new RegExpReportHandler(input, expression, result);
        return handler.createReport();
    }

    static class RegExpReportHandler extends AbstractReportHandler {

        RegExpReportHandler(String input, String expression, BooleanType result) {
            report.setName("Regular Expression Validation");
            report.setReports(new TestAssertionGroupReportsType());

            AnyContent attachment = new AnyContent();
            attachment.setType(DataType.MAP_DATA_TYPE);

            AnyContent xml = new AnyContent();
            xml.setName(RegExpValidator.INPUT_ARGUMENT_NAME);
            xml.setType(DataType.STRING_DATA_TYPE);
            xml.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            xml.setValue(input);
            attachment.getItem().add(xml);

            AnyContent schema = new AnyContent();
            schema.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            schema.setType(DataType.STRING_DATA_TYPE);
            schema.setName(RegExpValidator.EXPRESSION_ARGUMENT_NAME);
            schema.setValue(expression);
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

}
