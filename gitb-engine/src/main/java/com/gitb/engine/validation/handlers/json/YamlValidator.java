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

package com.gitb.engine.validation.handlers.json;

import com.gitb.engine.validation.ValidationHandler;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;

import java.util.Map;
import java.util.Optional;

@ValidationHandler(name="YamlValidator")
public class YamlValidator extends JsonValidator {

    private static final String MODULE_DEFINITION_XML = "/validation/yaml-validator-definition.xml";

    private static final String YAML_ARGUMENT_NAME = "yaml";
    private static final String SUPPORT_JSON_ARGUMENT_NAME = "supportJson";

    public YamlValidator() {
        super(MODULE_DEFINITION_XML);
    }

    @Override
    protected String getInputArgumentName() {
        return YAML_ARGUMENT_NAME;
    }

    @Override
    protected boolean supportsYaml(Map<String, DataType> inputs) {
        return true;
    }

    @Override
    protected boolean supportsJson(Map<String, DataType> inputs) {
        return Optional.ofNullable(getAndConvert(inputs, SUPPORT_JSON_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(value -> (Boolean) value.getValue())
                .orElse(false);
    }

}
