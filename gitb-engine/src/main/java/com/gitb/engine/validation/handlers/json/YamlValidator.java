package com.gitb.engine.validation.handlers.json;

import com.gitb.engine.validation.ValidationHandler;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;

import java.util.Map;
import java.util.Optional;

@ValidationHandler(name="YamlValidator")
public class YamlValidator extends JsonValidator {

    private static final String YAML_ARGUMENT_NAME = "yaml";
    private static final String SUPPORT_JSON_ARGUMENT_NAME = "supportJson";

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
