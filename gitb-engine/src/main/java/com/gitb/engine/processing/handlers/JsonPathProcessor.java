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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.gitb.core.ConfigurationParameters;
import com.gitb.core.ConfigurationType;
import com.gitb.core.Metadata;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.*;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.gitb.engine.utils.HandlerUtils.*;

@ProcessingHandler(name="JsonPathProcessor")
public class JsonPathProcessor extends AbstractProcessingHandler {

    private static final String OPERATION_PROCESS = "process";
    private static final String OPERATION_COUNT = "count";
    private static final String OPERATION_EXISTS = "exists";

    private static final String INPUT_CONTENT = "content";
    private static final String INPUT_EXPRESSION = "expression";
    private static final String INPUT_OUTPUT_TYPE = "outputType";
    private static final String INPUT_AS_YAML = "asYaml";
    private static final String OUTPUT_OUTPUT = "output";

    @Override
    protected ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId("JsonPathProcessor");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION_PROCESS,
                List.of(
                        createParameter(INPUT_CONTENT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The JSON content to evaluate the expression on."),
                        createParameter(INPUT_EXPRESSION, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The JSON path expression to evaluate."),
                        createParameter(INPUT_OUTPUT_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The type of output to return ('raw', 'list', 'default')."),
                        createParameter(INPUT_AS_YAML, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether the provided input should be parsed as YAML.")
                ),
                List.of(createParameter(OUTPUT_OUTPUT, "list", UsageEnumeration.R, ConfigurationType.SIMPLE, "The result after evaluating the expression."))
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_COUNT,
                List.of(
                        createParameter(INPUT_CONTENT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The JSON content to evaluate the expression on."),
                        createParameter(INPUT_EXPRESSION, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The JSON path expression to evaluate."),
                        createParameter(INPUT_AS_YAML, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether the provided input should be parsed as YAML.")
                ),
                List.of(createParameter(OUTPUT_OUTPUT, "number", UsageEnumeration.R, ConfigurationType.SIMPLE, "The result count."))
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_EXISTS,
                List.of(
                        createParameter(INPUT_CONTENT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The JSON content to evaluate the expression on."),
                        createParameter(INPUT_EXPRESSION, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The JSON path expression to evaluate."),
                        createParameter(INPUT_AS_YAML, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether the provided input should be parsed as YAML.")
                ),
                List.of(createParameter(OUTPUT_OUTPUT, "boolean", UsageEnumeration.R, ConfigurationType.SIMPLE, "Whether matches exist."))
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        ProcessingData data = new ProcessingData();
        // Collect common inputs.
        String content = getRequiredInputForName(input, INPUT_CONTENT, StringType.class).getValue();
        String expression = getRequiredInputForName(input, INPUT_EXPRESSION, StringType.class).getValue();
        boolean asYaml = Optional.ofNullable(getInputForName(input, INPUT_AS_YAML, BooleanType.class)).map(BooleanType::getValue).orElse(false);
        if (operation == null || OPERATION_PROCESS.equalsIgnoreCase(operation)) {
            OutputType outputType = OutputType.parse(Optional.ofNullable(getInputForName(input, INPUT_OUTPUT_TYPE, StringType.class)).map(StringType::getValue).orElse(null));
            // Process expression.
            var result = processJson(content, expression, asYaml);
            // Construct result.
            DataType output = switch (outputType) {
                case OutputType.RAW -> new StringType(serialise(result, asYaml));
                case OutputType.LIST -> convertToResult(result, true, asYaml);
                case OutputType.DEFAULT -> convertToResult(result, false, asYaml);
            };
            data.getData().put(OUTPUT_OUTPUT, output);
        } else if (OPERATION_COUNT.equalsIgnoreCase(operation)) {
            // Process expression.
            var result = processJson(content, expression, asYaml);
            data.getData().put(OUTPUT_OUTPUT, new NumberType(result.size()));
        } else if (OPERATION_EXISTS.equalsIgnoreCase(operation)) {
            // Process expression.
            var result = processJson(content, expression, asYaml);
            data.getData().put(OUTPUT_OUTPUT, new BooleanType(!result.isEmpty()));
        } else {
            throw new IllegalArgumentException("Unknown operation [" + operation + "]");
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

    private ArrayNode processJson(String content, String expression, boolean asYaml) {
        if (asYaml) {
            content = convertYamlToJson(content);
        }
        var config = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);
        return JsonPath.using(config).parse(content).read(expression, ArrayNode.class);
    }

    private String serialise(JsonNode node, boolean asYaml) {
        if (asYaml) {
            return writeAsYaml(node);
        } else {
            return writeAsJson(node);
        }
    }

    private DataType convertToResult(ArrayNode result, boolean resultAsList, boolean asYaml) {
        int resultCount = result.size();
        DataType resultType;
        if (resultCount == 0) {
            // Empty list.
            resultType = new ListType();
        } else if (resultCount == 1) {
            DataType data = convertToDataType(result.get(0), asYaml);
            if (resultAsList) {
                var value = new ArrayList<DataType>();
                value.add(data);
                resultType = new ListType(value);
            } else {
                resultType = data;
            }
        } else {
            resultType = convertArrayToDataType(result, asYaml);
        }
        return resultType;
    }

    private DataType convertArrayToDataType(ArrayNode node, boolean asYaml) {
        Optional<JsonNodeType> commonType =  commonNodeType(node);
        var listType = new ListType();
        if (commonType.isEmpty()) {
            for (JsonNode jsonNode : node) {
                listType.append(new StringType(serialise(jsonNode, asYaml)));
            }
        } else {
            // All elements have the same type.
            for (JsonNode jsonNode : node) {
                listType.append(convertToDataType(jsonNode, asYaml));
            }
        }
        return listType;
    }

    private Optional<JsonNodeType> commonNodeType(ArrayNode nodes) {
        var iterator = nodes.elements();
        JsonNodeType currentType = null;
        while (iterator.hasNext()) {
            var node = iterator.next();
            if (currentType == null) {
                currentType = node.getNodeType();
            } else if (currentType != node.getNodeType()) {
                currentType = null;
                break;
            }
        }
        return Optional.ofNullable(currentType);
    }

    private DataType convertToDataType(JsonNode node, boolean asYaml) {
        if (node instanceof ValueNode valueNode) {
            // Simple (leaf) value.
            return switch (valueNode.getNodeType()) {
                case JsonNodeType.STRING -> new StringType(valueNode.asText());
                case JsonNodeType.BOOLEAN -> new BooleanType(valueNode.asBoolean());
                case JsonNodeType.NUMBER -> new NumberType(valueNode.asDouble());
                default -> new StringType("");
            };
        } else if (node instanceof ArrayNode arrayNode) {
            return convertArrayToDataType(arrayNode, asYaml);
        } else {
            // Non-leaf value.
            return new StringType(serialise(node, asYaml));
        }
    }

    private enum OutputType {

        RAW, LIST, DEFAULT;

        private static OutputType parse(String providedType) {
            if (providedType == null || "default".equalsIgnoreCase(providedType)) {
                return DEFAULT;
            } else if ("raw".equalsIgnoreCase(providedType)) {
                return RAW;
            } else if ("list".equalsIgnoreCase(providedType)) {
                return LIST;
            } else {
                throw new IllegalArgumentException("Unsupported '%s' value [%s]".formatted(INPUT_OUTPUT_TYPE, providedType));
            }
        }
    }

}
