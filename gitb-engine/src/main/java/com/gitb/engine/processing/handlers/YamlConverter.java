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

package com.gitb.engine.processing.handlers;

import com.gitb.core.ConfigurationParameters;
import com.gitb.core.ConfigurationType;
import com.gitb.core.Metadata;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.StringType;

import java.util.List;

import static com.gitb.engine.utils.HandlerUtils.convertJsonToYaml;
import static com.gitb.engine.utils.HandlerUtils.convertYamlToJson;

@ProcessingHandler(name="YamlConverter")
public class YamlConverter extends AbstractProcessingHandler {

    private static final String OPERATION_YAML_TO_JSON = "yamlToJson";
    private static final String OPERATION_JSON_TO_YAML = "jsonToYaml";

    private static final String INPUT_CONTENT = "content";
    private static final String OUTPUT_OUTPUT = "output";

    @Override
    protected ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId("YamlConverter");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION_YAML_TO_JSON,
                List.of(createParameter(INPUT_CONTENT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The YAML content to convert.")),
                List.of(createParameter(OUTPUT_OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The converted JSON."))
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_JSON_TO_YAML,
                List.of(createParameter(INPUT_CONTENT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The JSON content to convert.")),
                List.of(createParameter(OUTPUT_OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The converted YAML."))
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        ProcessingData data = new ProcessingData();
        String content = getRequiredInputForName(input, INPUT_CONTENT, StringType.class).getValue();
        String output;
        if (operation == null || OPERATION_YAML_TO_JSON.equalsIgnoreCase(operation)) {
            output = convertYamlToJson(content);
        } else if (OPERATION_JSON_TO_YAML.equalsIgnoreCase(operation)) {
            output = convertJsonToYaml(content);
        } else {
            throw new IllegalArgumentException("Unknown operation [" + operation + "]");
        }
        data.getData().put(OUTPUT_OUTPUT, new StringType(output));
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

}
