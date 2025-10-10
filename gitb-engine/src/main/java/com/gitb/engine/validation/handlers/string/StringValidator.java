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

    private static final String ACTUAL_ARGUMENT_NAME = "actual";
    private static final String EXPECTED_ARGUMENT_NAME = "expected";
    private static final String MODULE_DEFINITION_XML = "/validation/string-validator-definition.xml";

    public StringValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        StringType actualString    = getAndConvert(inputs, ACTUAL_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class);
        StringType expectedString  = getAndConvert(inputs, EXPECTED_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class);

        String actualStringValue   = actualString.getValue();
        String expectedStringValue = expectedString.getValue();

        // process xpath
        BooleanType result = new BooleanType(actualStringValue.equals(expectedStringValue));

        return createReport(inputs, () -> new StringReportHandler(actualString, expectedString, result).createReport());
    }
}
