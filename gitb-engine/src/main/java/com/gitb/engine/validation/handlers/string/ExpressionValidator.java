/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
            if (result.getValue()) {
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
