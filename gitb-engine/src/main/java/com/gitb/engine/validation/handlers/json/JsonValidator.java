package com.gitb.engine.validation.handlers.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.utils.HandlerUtils;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.AbstractValidator;
import com.gitb.tr.*;
import com.gitb.types.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.networknt.schema.*;
import com.networknt.schema.i18n.ResourceBundleMessageSource;
import com.networknt.schema.serialization.JsonNodeReader;
import com.networknt.schema.utils.JsonNodes;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@ValidationHandler(name="JsonValidator")
public class JsonValidator extends AbstractValidator {

    private final static String MODULE_DEFINITION_XML = "/validation/json-validator-definition.xml";

    private static final String JSON_ARGUMENT_NAME = "json";
    private static final String SCHEMA_ARGUMENT_NAME = "schema";
    private static final String SHOW_SCHEMA_ARGUMENT_NAME = "showSchema";
    private static final String SUPPORT_YAML_ARGUMENT_NAME = "supportYaml";
    private static final String SCHEMA_COMBINATION_APPROACH_ARGUMENT_NAME = "schemaCombinationApproach";

    private final ObjectMapper objectMapper = new ObjectMapper(); // ObjectMapper is thread-safe.

    public JsonValidator() {
        this(MODULE_DEFINITION_XML);
    }

    public JsonValidator(String moduleDefinitionPath) {
        this.validatorDefinition = readModuleDefinition(moduleDefinitionPath);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        // Retrieve and check inputs.
        String content = (String) Objects.requireNonNull(getAndConvert(inputs, getInputArgumentName(), DataType.STRING_DATA_TYPE, StringType.class), "Input [%s] must be provided".formatted(getInputArgumentName())).getValue();
        List<MapType> schemas = Optional.ofNullable(getAndConvert(inputs, SCHEMA_ARGUMENT_NAME, DataType.LIST_DATA_TYPE, ListType.class))
                .map(list -> list.getElements().stream()
                        .map(schemaData -> {
                            if (DataType.MAP_DATA_TYPE.equals(schemaData.getType())) {
                                if (((MapType) schemaData).getItem(SCHEMA_ARGUMENT_NAME) == null) {
                                    throw new IllegalArgumentException("Provided a map for input [%s] that did not contain a key named [%s]".formatted(SCHEMA_ARGUMENT_NAME, SCHEMA_ARGUMENT_NAME));
                                }
                                return (MapType) schemaData;
                            } else {
                                MapType schemaMap = new MapType();
                                schemaMap.addItem(SCHEMA_ARGUMENT_NAME, schemaData.convertTo(DataType.STRING_DATA_TYPE));
                                schemaMap.setImportPath(schemaData.getImportPath());
                                schemaMap.setImportTestSuite(schemaData.getImportTestSuite());
                                return schemaMap;
                            }
                        })
                        .toList()
                )
                .orElseGet(Collections::emptyList);
        boolean showSchemas = Optional.ofNullable(getAndConvert(inputs, SHOW_SCHEMA_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(value -> (Boolean) value.getValue())
                .orElse(true);
        boolean supportYaml = supportsYaml(inputs);
        boolean supportJson = supportsJson(inputs);
        SchemaCombinationApproach schemaCombinationApproach = Optional.ofNullable(getAndConvert(inputs, SCHEMA_COMBINATION_APPROACH_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class))
                .map(value -> SchemaCombinationApproach.byName((String) value.getValue()))
                .orElse(SchemaCombinationApproach.ALL);
        String testCaseId = getTestCaseId(inputs);
        String sessionId = (String) inputs.get(HandlerUtils.SESSION_INPUT).getValue();
        // Proceed with validation.
        InputInfo inputInfo = prepareInput(content, supportYaml, supportJson);
        List<Message> messages = validateAgainstSetOfSchemas(inputInfo, schemas, schemaCombinationApproach, testCaseId, sessionId);
        return createReport(new ReportSpecs(messages, inputInfo, schemas, showSchemas));
    }

    protected String getInputArgumentName() {
        return JSON_ARGUMENT_NAME;
    }

    protected boolean supportsYaml(Map<String, DataType> inputs) {
        return Optional.ofNullable(getAndConvert(inputs, SUPPORT_YAML_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(value -> (Boolean) value.getValue())
                .orElse(false);
    }

    protected boolean supportsJson(Map<String, DataType> inputs) {
        return true;
    }

    private InputInfo prepareInput(String input, boolean supportYaml, boolean supportJson) {
        boolean isYaml = isYaml(input);
        if (isYaml && !supportYaml) {
            throw new IllegalArgumentException("Input [%s] must be provided in JSON format".formatted(getInputArgumentName()));
        } else if (!isYaml && !supportJson) {
            throw new IllegalArgumentException("Input [%s] must be provided in YAML format".formatted(getInputArgumentName()));
        } else {
            if (isYaml) {
                // No need to pretty-print YAML
                return new InputInfo(input, true);
            } else {
                return new InputInfo(prettyPrintJson(input), false);
            }
        }
    }

    private String prettyPrintJson(String input) {
        try (StringReader in = new StringReader(input)) {
            JsonElement json = com.google.gson.JsonParser.parseReader(in);
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .serializeNulls()
                    .create();
            return gson.toJson(json);
        } catch (JsonSyntaxException e) {
            throw new IllegalStateException("Unable to parse provided input as a JSON document", e);
        }
    }

    protected boolean isYaml(String input) {
        try (var reader = new BufferedReader(new StringReader(input))) {
            String firstLine = reader.readLine();
            return firstLine != null && !(firstLine.startsWith("{") || firstLine.startsWith("["));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read provided input", e);
        }
    }

    private List<Message> validateAgainstSetOfSchemas(InputInfo contentInfo, List<MapType> schemas, SchemaCombinationApproach combinationApproach, String testCaseId, String sessionId) {
        JsonNodeReader jsonReader = getJsonReader();
        JsonNode contentNode = getContentNode(contentInfo, jsonReader);
        var aggregatedMessages = new LinkedList<Message>();
        if (combinationApproach == SchemaCombinationApproach.ALL) {
            // All schema validations must result in success.
            for (MapType schema: schemas) {
                aggregatedMessages.addAll(validateAgainstSchema(contentNode, schema, jsonReader, testCaseId, sessionId));
            }
        } else if (combinationApproach == SchemaCombinationApproach.ONE_OF) {
            // All schemas need to be validated but only one should validate successfully.
            int successCount = 0;
            int branchCounter = 1;
            for (MapType schema: schemas) {
                var latestErrors = validateAgainstSchema(contentNode, schema, jsonReader, testCaseId, sessionId);
                if (latestErrors.isEmpty()) {
                    successCount += 1;
                } else {
                    addBranchErrors(aggregatedMessages, latestErrors, branchCounter++);
                }
            }
            if (successCount == 0) {
                aggregatedMessages.addFirst(new Message("Exactly one of the following sets of problems must be resolved."));
            } else if (successCount == 1) {
                aggregatedMessages.clear();
            } else if (successCount > 1) {
                aggregatedMessages.clear();
                aggregatedMessages.add(new Message("Only one schema should be valid. Instead the content validated against %s schemas.".formatted(successCount)));
            }
        } else {
            // Any of the schemas should validate successfully.
            int branchCounter = 1;
            for (MapType schema: schemas) {
                List<Message> latestErrors = validateAgainstSchema(contentNode, schema, jsonReader, testCaseId, sessionId);
                if (latestErrors.isEmpty()) {
                    aggregatedMessages.clear();
                    break;
                } else {
                    addBranchErrors(aggregatedMessages, latestErrors, branchCounter++);
                }
            }
            if (!aggregatedMessages.isEmpty()) {
                aggregatedMessages.addFirst(new Message("At least one of the following sets of problems must be resolved."));
            }
        }
        return aggregatedMessages;
    }

    private List<Message> validateAgainstSchema(JsonNode contentNode, MapType schema, JsonNodeReader jsonReader, String testCaseId, String sessionId) {
        JsonSchema parsedSchema = readSchema(schema, jsonReader, testCaseId, sessionId);
        var locationMapper = getLocationMapper();
        return parsedSchema.validate(contentNode).stream().map((message) -> new Message(StringUtils.removeStart(message.getMessage(), "[] "), locationMapper.apply(message))).collect(Collectors.toList());
    }

    private void addBranchErrors(List<Message> aggregatedMessages, List<Message> branchMessages, int branchCounter) {
        for (var error: branchMessages) {
            error.setDescription("["+branchCounter+"]: "+error.getDescription());
            aggregatedMessages.add(error);
        }
    }

    private Function<ValidationMessage, String> getLocationMapper() {
        return (msg) -> {
            var nodeLocation = JsonNodes.tokenLocationOf(msg.getInstanceNode());
            int lineNumber = 0;
            if (nodeLocation != null && nodeLocation.getLineNr() > 0) {
                lineNumber = nodeLocation.getLineNr();
            }
            return "%s:%s:0".formatted(getInputArgumentName(), lineNumber);
        };
    }

    private JsonSchema readSchema(MapType schema, JsonNodeReader jsonReader, String testCaseId, String sessionId) {
        try {
            var jsonNode = objectMapper.readTree((String) schema.getItem(SCHEMA_ARGUMENT_NAME).convertTo(DataType.STRING_DATA_TYPE).getValue());
            var jsonSchemaVersion = JsonSchemaFactory.checkVersion(SpecVersionDetector.detect(jsonNode));
            var metaSchema = jsonSchemaVersion.getInstance();
            /*
             * The schema factory is created per validation. This is done to avoid caching of schemas across validations that
             * may be remotely loaded or schemas that are user-provided. In addition, it allows us to treat schemas that
             * may use different specification versions.
             */
            var sharedSchemaInfo = new SharedSchemaInfo(
                    Optional.ofNullable(schema.getItem(SCHEMA_ARGUMENT_NAME).getImportTestSuite()),
                    testCaseId,
                    Optional.ofNullable(schema.getItem("sharedSchemaPaths"))
                            .map(paths -> ((ListType) paths.convertTo(DataType.LIST_DATA_TYPE)).getElements().stream()
                                    .map(pathItem -> (String) pathItem.convertTo(DataType.STRING_DATA_TYPE).getValue())
                                    .toList()
                            ).orElseGet(Collections::emptyList)
            );
            var schemaFactory = JsonSchemaFactory.builder()
                    .schemaLoaders(schemaLoaders -> schemaLoaders.add(new LocalSchemaResolver(sharedSchemaInfo, getScope(sessionId).getContext())))
                    .metaSchema(metaSchema)
                    .defaultMetaSchemaIri(metaSchema.getIri())
                    .jsonNodeReader(jsonReader)
                    .build();
            var schemaConfig = SchemaValidatorsConfig.builder()
                    .pathType(PathType.JSON_POINTER)
                    .locale(Locale.ENGLISH)
                    .messageSource(new ResourceBundleMessageSource("com.gitb.i18n.jsv-messages"))
                    .build();
            return schemaFactory.getSchema(jsonNode, schemaConfig);
        } catch (IOException e) {
            throw new IllegalStateException("Error while parsing JSON schema: %s".formatted(e.getMessage()), e);
        }
    }

    private JsonNode getContentNode(InputInfo inputInfo, JsonNodeReader reader) {
        try {
            return reader.readTree(inputInfo.content(), inputInfo.isYaml()?InputFormat.YAML:InputFormat.JSON);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse JSON input", e);
        }
    }

    private JsonNodeReader getJsonReader() {
        var jsonReaderBuilder = JsonNodeReader.builder();
        jsonReaderBuilder = jsonReaderBuilder.locationAware();
        return jsonReaderBuilder.build();
    }

    private AnyContent toAnyContent(String value, Optional<String> name, Optional<MimeType> mimeType) {
        var contextItem = new AnyContent();
        contextItem.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        name.ifPresent(contextItem::setName);
        contextItem.setType("string");
        contextItem.setForContext(true);
        contextItem.setForDisplay(true);
        contextItem.setValue(value);
        mimeType.ifPresent((type) -> contextItem.setMimeType(type.toString()));
        return contextItem;
    }

    private TAR createReport(ReportSpecs reportSpecs) {
        var report = TestCaseUtils.createEmptyReport();
        report.setName("JSON validation");
        var context = new AnyContent();
        report.setContext(context);
        // Input.
        context.getItem().add(toAnyContent(reportSpecs.inputInfo().content(), Optional.of(getInputArgumentName()), Optional.of(reportSpecs.getInputMimeType())));
        // Schema(s).
        if (reportSpecs.showSchema() && !reportSpecs.schemas().isEmpty()) {
            if (reportSpecs.schemas().size() == 1) {
                context.getItem().add(toAnyContent((String) reportSpecs.schemas().getFirst().getItem(SCHEMA_ARGUMENT_NAME).convertTo(DataType.STRING_DATA_TYPE).getValue(), Optional.of(SCHEMA_ARGUMENT_NAME), Optional.of(MediaType.APPLICATION_JSON)));
            } else {
                var schemasContent = new AnyContent();
                schemasContent.setType("list[string]");
                schemasContent.setForDisplay(true);
                schemasContent.setForContext(true);
                schemasContent.setName(SCHEMA_ARGUMENT_NAME);
                for (MapType schema: reportSpecs.schemas()) {
                    schemasContent.getItem().add(toAnyContent((String) schema.getItem(SCHEMA_ARGUMENT_NAME).convertTo(DataType.STRING_DATA_TYPE).getValue(), Optional.empty(), Optional.of(MediaType.APPLICATION_JSON)));
                }
                context.getItem().add(schemasContent);
            }
        }
        // Create TAR report.
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfWarnings(BigInteger.ZERO);
        report.getCounters().setNrOfAssertions(BigInteger.ZERO);
        if (reportSpecs.messages() == null || reportSpecs.messages().isEmpty()) {
            report.setResult(TestResultType.SUCCESS);
            report.getCounters().setNrOfErrors(BigInteger.ZERO);
        } else {
            report.setResult(TestResultType.FAILURE);
            report.getCounters().setNrOfErrors(BigInteger.valueOf(reportSpecs.messages().size()));
            report.setReports(new TestAssertionGroupReportsType());
            for (var message: reportSpecs.messages()) {
                BAR error = new BAR();
                error.setDescription(message.getDescription());
                error.setLocation(message.getContentPath());
                var elementForReport = objectFactory.createTestAssertionGroupReportsTypeError(error);
                report.getReports().getInfoOrWarningOrError().add(elementForReport);
            }
        }
        return report;
    }

    private record ReportSpecs(List<Message> messages, InputInfo inputInfo, List<MapType> schemas, boolean showSchema) {

        MimeType getInputMimeType() {
            if (inputInfo().isYaml()) {
                return MediaType.TEXT_PLAIN;
            } else {
                return MediaType.APPLICATION_JSON;
            }
        }

    }
    private record InputInfo(String content, boolean isYaml) {}
}
