package com.gitb.engine.validation.handlers.string;

import com.gitb.core.Configuration;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.AbstractReportHandler;
import com.gitb.engine.validation.handlers.common.SimpleValidator;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;

import java.util.List;
import java.util.Map;

@ValidationHandler(name="ExpressionValidator")
public class ExpressionValidator extends SimpleValidator {

    private static final String EXPRESSION_ARGUMENT_NAME = "expression";
    private static final String MODULE_DEFINITION_XML = "/validation/expression-validator-definition.xml";

    public ExpressionValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        String expression = (String)inputs.get(EXPRESSION_ARGUMENT_NAME).convertTo(DataType.STRING_DATA_TYPE).getValue();
        BooleanType result = new BooleanType(Boolean.parseBoolean(expression));
        return createReport(inputs, () -> new ExpressionValidator.ExpressionReportHandler(result).createReport());
    }

    static class ExpressionReportHandler extends AbstractReportHandler {

        ExpressionReportHandler(BooleanType result) {
            if ((boolean) result.getValue()) {
                report.setResult(TestResultType.SUCCESS);
            } else {
                report.setResult(TestResultType.FAILURE);
            }
        }

        @Override
        public TAR createReport() {
            return report;
        }
    }
}
