package com.gitb.engine.processing.handlers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.gitb.types.DataType;
import com.gitb.types.StringType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@ProcessingHandler(name="JSONPointerProcessor")
public class JSONPointerProcessor extends AbstractProcessingHandler {

    private static final String OPERATION__PROCESS = "process";
    private static final String INPUT__CONTENT = "content";
    private static final String INPUT__POINTER = "pointer";
    private static final String OUTPUT__OUTPUT = "output";

    @Override
    public ProcessingModule getModuleDefinition() {
        ProcessingModule module = new ProcessingModule();
        module.setId("JSONPointerProcessor");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION__PROCESS,
            List.of(
                    createParameter(INPUT__CONTENT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The JSON content to evaluate the pointer on."),
                    createParameter(INPUT__POINTER, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The JSON pointer expression to evaluate.")
            ),
            List.of(createParameter(OUTPUT__OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The result after evaluating the pointer."))
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        // Collect inputs
        String inputContent;
        if (!input.getData().containsKey(INPUT__CONTENT)) {
            throw new IllegalArgumentException("The JSON content to evaluate the pointer on is required");
        } else {
            inputContent = (String) input.getData().get(INPUT__CONTENT).convertTo(DataType.STRING_DATA_TYPE).getValue();
        }

        JsonPointer pointer;
        if (!input.getData().containsKey(INPUT__POINTER)) {
            throw new IllegalArgumentException("The JSON pointer is required");
        } else {
            try {
                pointer = JsonPointer.compile((String) input.getData().get(INPUT__POINTER).convertTo(DataType.STRING_DATA_TYPE).getValue());
            } catch (Exception e) {
                throw new IllegalArgumentException("An error occurred while compiling the JSON pointer: "+e.getMessage());
            }
        }
        // Carry out operation
        var mapper = new ObjectMapper();
        JsonNode documentNode;
        try {
            documentNode = mapper.readTree(inputContent);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("An error occurred while reading the provided JSON content: "+e.getMessage());
        }
        String resultString = null;
        var resultNode = documentNode.at(pointer);
        if (resultNode != null) {
            try {
                if (resultNode instanceof ValueNode) {
                    resultString = resultNode.asText();
                } else {
                    resultString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultNode);
                }
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("An error occurred while writing the JSON output: "+e.getMessage());
            }
        }
        ProcessingData data = new ProcessingData();
        data.getData().put(OUTPUT__OUTPUT, new StringType(StringUtils.defaultString(resultString)));
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

}
