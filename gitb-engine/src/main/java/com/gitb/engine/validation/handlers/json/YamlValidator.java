package com.gitb.engine.validation.handlers.json;

import com.gitb.engine.validation.ValidationHandler;
import com.gitb.types.DataType;

import java.util.Map;

@ValidationHandler(name="YamlValidator")
public class YamlValidator extends JsonValidator {

    private static final String YAML_ARGUMENT_NAME = "yaml";

    @Override
    protected String getInputArgumentName() {
        return YAML_ARGUMENT_NAME;
    }

    @Override
    protected boolean supportsYaml(Map<String, DataType> inputs) {
        return true;
    }

    @Override
    protected boolean isYaml(String input) {
        boolean parsedContentIsYaml = super.isYaml(input);
        if (!parsedContentIsYaml) {
            throw new IllegalArgumentException("Input [%s] must be provided in YAML syntax".formatted(YAML_ARGUMENT_NAME));
        }
        return true;
    }

}
