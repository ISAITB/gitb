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

    private static final String ACTUAL_NUMBER_ARGUMENT_NAME = "actual";
    private static final String EXPECTED_NUMBER_ARGUMENT_NAME = "expected";
    private static final String MODULE_DEFINITION_XML = "/validation/number-validator-definition.xml";

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
