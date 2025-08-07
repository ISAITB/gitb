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

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.gitb.core.ConfigurationParameters;
import com.gitb.core.ConfigurationType;
import com.gitb.core.Metadata;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.engine.utils.HandlerUtils;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.BooleanType;
import com.gitb.types.StringType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@ProcessingHandler(name="JsonPointerProcessor")
public class JsonPointerProcessor extends AbstractProcessingHandler {

    private static final String OPERATION_PROCESS = "process";
    private static final String INPUT_CONTENT = "content";
    private static final String INPUT_POINTER = "pointer";
    private static final String INPUT_AS_YAML = "asYaml";
    private static final String OUTPUT_OUTPUT = "output";

    @Override
    public ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId("JsonPointerProcessor");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION_PROCESS,
            List.of(
                    createParameter(INPUT_CONTENT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The JSON content to evaluate the pointer on."),
                    createParameter(INPUT_POINTER, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The JSON pointer expression to evaluate."),
                    createParameter(INPUT_AS_YAML, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether the provided input should be parsed as YAML.")
            ),
            List.of(createParameter(OUTPUT_OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The result after evaluating the pointer."))
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        // Collect inputs
        String inputContent = getRequiredInputForName(input, INPUT_CONTENT, StringType.class).getValue();
        boolean asYaml = Optional.ofNullable(getInputForName(input, INPUT_AS_YAML, BooleanType.class)).map(BooleanType::getValue).orElse(false);
        JsonPointer pointer;
        try {
            pointer = JsonPointer.compile(getRequiredInputForName(input, INPUT_POINTER, StringType.class).getValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurred while compiling the JSON pointer: "+e.getMessage());
        }
        // Carry out operation
        Function<String, JsonNode> reader;
        Function<JsonNode, String> writer;
        if (asYaml) {
            reader = HandlerUtils::readAsYaml;
            writer = HandlerUtils::writeAsYaml;
        } else {
            reader = HandlerUtils::readAsJson;
            writer = HandlerUtils::writeAsJson;
        }
        JsonNode documentNode = reader.apply(inputContent);
        String resultString = null;
        var resultNode = documentNode.at(pointer);
        if (resultNode instanceof ValueNode) {
            resultString = resultNode.asText();
        } else if (resultNode != null) {
            resultString = writer.apply(resultNode);
        }
        ProcessingData data = new ProcessingData();
        data.getData().put(OUTPUT_OUTPUT, new StringType(StringUtils.defaultString(resultString)));
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

}
