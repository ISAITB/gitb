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

import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.AbstractReportHandler;
import com.gitb.engine.validation.handlers.common.SimpleValidator;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Created by simatosc
 */
@ValidationHandler(name="RegExpValidator")
public class RegExpValidator extends SimpleValidator {

    private static final String INPUT_ARGUMENT_NAME   = "input";
    private static final String EXPRESSION_ARGUMENT_NAME = "expression";
    private static final String MODULE_DEFINITION_XML = "/validation/regexp-validator-definition.xml";

    public RegExpValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        String input = (String)inputs.get(INPUT_ARGUMENT_NAME).convertTo(DataType.STRING_DATA_TYPE).getValue();
        String expression = (String)inputs.get(EXPRESSION_ARGUMENT_NAME).convertTo(DataType.STRING_DATA_TYPE).getValue();
        Objects.requireNonNull(input, "No input provided.");
        Objects.requireNonNull(input, "No expression provided.");
        // Process expression.
        BooleanType result = new BooleanType(Pattern.matches(expression, input));
        // Return report.
        return createReport(inputs, () -> new RegExpReportHandler(input, expression, result).createReport());
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
