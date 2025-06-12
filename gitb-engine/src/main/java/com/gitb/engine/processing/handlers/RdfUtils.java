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
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.ListType;
import com.gitb.types.StringType;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@ProcessingHandler(name="RdfUtils")
public class RdfUtils extends AbstractProcessingHandler {

    private static final String OPERATION__CONVERT = "convert";
    private static final String OPERATION__MERGE = "merge";
    private static final String OPERATION__ASK = "ask";
    private static final String OPERATION__SELECT = "select";
    private static final String OPERATION__CONSTRUCT = "construct";

    private static final String INPUT__MODEL = "model";
    private static final String INPUT__MODELS = "models";
    private static final String INPUT__QUERY = "query";
    private static final String INPUT__INPUT_CONTENT_TYPE = "inputContentType";
    private static final String INPUT__INPUT_CONTENT_TYPES = "inputContentTypes";
    private static final String INPUT__OUTPUT_CONTENT_TYPE = "outputContentType";
    private static final String OUTPUT__OUTPUT = "output";

    private static final ContentType APPLICATION_XML =  ContentType.create("application/xml");
    private static final ContentType APPLICATION_SPARQL_RESULTS_XML = ContentType.create("application/sparql-results+xml");
    private static final ContentType TEXT_XML =  ContentType.create("text/xml");
    private static final ContentType APPLICATION_JSON =  ContentType.create("application/json");
    private static final ContentType APPLICATION_SPARQL_RESULTS_JSON = ContentType.create("application/sparql-results+json");
    private static final ContentType TEXT_CSV =  ContentType.create("text/csv");
    private static final ContentType TEXT_TSV =  ContentType.create("text/tab-separated-values");

    private static final Map<String, Lang> EQUIVALENT_CONTENT_TYPES = Map.of(
            APPLICATION_XML.getContentTypeStr(), RDFLanguages.RDFXML,
            TEXT_XML.getContentTypeStr(), RDFLanguages.RDFXML,
            APPLICATION_JSON.getContentTypeStr(), RDFLanguages.JSONLD
    );

    @Override
    public ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId("RdfUtils");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION__CONVERT,
                List.of(
                        createParameter(INPUT__MODEL, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The RDF model to process."),
                        createParameter(INPUT__INPUT_CONTENT_TYPE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The content type of the input model."),
                        createParameter(INPUT__OUTPUT_CONTENT_TYPE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The content type of the output model.")
                ),
                List.of(
                        createParameter(OUTPUT__OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The converted model.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION__MERGE,
                List.of(
                        createParameter(INPUT__MODELS, "list", UsageEnumeration.R, ConfigurationType.SIMPLE, "The list of models to merge."),
                        createParameter(INPUT__INPUT_CONTENT_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The content type to consider for all provided models."),
                        createParameter(INPUT__INPUT_CONTENT_TYPES, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The content types of the provided models."),
                        createParameter(INPUT__OUTPUT_CONTENT_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The content type of the output model.")
                ),
                List.of(
                        createParameter(OUTPUT__OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The merged model.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION__ASK,
                List.of(
                        createParameter(INPUT__MODEL, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The model to query."),
                        createParameter(INPUT__QUERY, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The SPARQL query."),
                        createParameter(INPUT__INPUT_CONTENT_TYPE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The model's content type.")
                ),
                List.of(
                        createParameter(OUTPUT__OUTPUT, "boolean", UsageEnumeration.R, ConfigurationType.SIMPLE, "The query result.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION__CONSTRUCT,
                List.of(
                        createParameter(INPUT__MODEL, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The model to query."),
                        createParameter(INPUT__QUERY, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The SPARQL query."),
                        createParameter(INPUT__INPUT_CONTENT_TYPE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The model's content type."),
                        createParameter(INPUT__OUTPUT_CONTENT_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The content type of the constructed model.")
                ),
                List.of(
                        createParameter(OUTPUT__OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The output result set.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION__SELECT,
                List.of(
                        createParameter(INPUT__MODEL, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The model to query."),
                        createParameter(INPUT__QUERY, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The SPARQL query."),
                        createParameter(INPUT__INPUT_CONTENT_TYPE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The model's content type."),
                        createParameter(INPUT__OUTPUT_CONTENT_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The content type of the result set.")
                ),
                List.of(
                        createParameter(OUTPUT__OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The output result set.")
                )
        ));
        return module;
    }


    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        if (StringUtils.isBlank(operation)) {
            throw new IllegalArgumentException("No operation provided");
        }
        ProcessingData data = new ProcessingData();
        if (OPERATION__CONVERT.equalsIgnoreCase(operation)) {
            // Read inputs.
            var inputModel = getRequiredInputForName(input, INPUT__MODEL, StringType.class);
            var inputContentType = getRequiredInputForName(input, INPUT__INPUT_CONTENT_TYPE, StringType.class);
            var outputContentType = getRequiredInputForName(input, INPUT__OUTPUT_CONTENT_TYPE, StringType.class);
            // Parse and ensure from/to formats are valid.
            Lang inputLanguage = parseLanguage(inputContentType, () -> "Provided content type [%s] for input model is not a supported RDF type.".formatted(inputContentType.getValue()));
            Lang outputLanguage = parseLanguage(outputContentType, () -> "Provided content type [%s] for output model is not a supported RDF type.".formatted(outputContentType.getValue()));
            // Convert model.
            Model model = parseModel(inputModel, inputLanguage);
            // Write output
            data.getData().put(OUTPUT__OUTPUT, toStringType(model, outputLanguage));
        } else if (OPERATION__MERGE.equalsIgnoreCase(operation)) {
            var inputModels = getRequiredInputForName(input, INPUT__MODELS, ListType.class);
            // Parse input language.
            List<Lang> inputLanguages = Optional.ofNullable(getInputForName(input, INPUT__INPUT_CONTENT_TYPES, ListType.class))
                    .map(types -> {
                        // We have received a list of content types.
                        if (inputModels.getSize() != types.getSize()) {
                            throw new IllegalArgumentException("The number of provided models [%s] did not match the provided content types [%s]".formatted(inputModels.getSize(), types.getSize()));
                        }
                        return types.getElements().stream().map(item -> {
                            StringType contentType = (StringType) item.convertTo(DataType.STRING_DATA_TYPE);
                            return parseLanguage(contentType, () -> "A content type [%s] was provided that is not a supported RDF type.".formatted(contentType));
                        });
                    })
                    .orElseGet(() -> {
                        // We have not received a list of content types so we check and use the single one.
                        var inputContentType = Optional.ofNullable(getInputForName(input, INPUT__INPUT_CONTENT_TYPE, StringType.class));
                        if (inputContentType.isEmpty()) {
                            throw new IllegalArgumentException("No content type was provided for the input models.");
                        }
                        StringType contentType = (StringType) inputContentType.get().convertTo(DataType.STRING_DATA_TYPE);
                        Lang language = parseLanguage(contentType, () -> "The provided content type [%s] is not a supported RDF type.".formatted(contentType));
                        int modelCount = inputModels.getSize();
                        var languages = new ArrayList<Lang>(modelCount);
                        for (int i = 0; i < modelCount; i ++) {
                            languages.add(language);
                        }
                        return languages.stream();
                    })
                    .toList();
            // Parse output language (if not provided use the first available input language).
            Lang outputLanguage = Optional.ofNullable(getInputForName(input, INPUT__OUTPUT_CONTENT_TYPE, StringType.class))
                    .map(type -> parseLanguage(type, () -> "Provided content type for merged model [%s] is not a supported RDF type.".formatted(type.getValue())))
                    .orElseGet(inputLanguages::getFirst);
            // Merge models.
            Model outputModel = ModelFactory.createDefaultModel();
            for (int i = 0; i < inputModels.getSize(); i++) {
                Model currentModel = parseModel((StringType) inputModels.getItem(i).convertTo(DataType.STRING_DATA_TYPE), inputLanguages.get(i));
                outputModel.add(currentModel);
            }
            // Write output
            data.getData().put(OUTPUT__OUTPUT, toStringType(outputModel, outputLanguage));
        } else if (OPERATION__CONSTRUCT.equalsIgnoreCase(operation)) {
            QueryOperationInput inputs = parseQueryOperationInputs(input, QueryType.CONSTRUCT);
            Lang outputLanguage = Optional.ofNullable(getInputForName(input, INPUT__OUTPUT_CONTENT_TYPE, StringType.class))
                    .map(type -> parseLanguage(type, () -> "Provided content type for output model [%s] is not a supported RDF type.".formatted(type.getValue())))
                    .orElse(inputs.inputLanguage);
            Model outputModel;
            try (QueryExecution queryExecution = QueryExecutionFactory.create(inputs.query, inputs.inputModel)) {
                outputModel = queryExecution.execConstruct();
            }
            data.getData().put(OUTPUT__OUTPUT, toStringType(outputModel, outputLanguage));
        } else if (OPERATION__ASK.equalsIgnoreCase(operation)) {
            QueryOperationInput inputs = parseQueryOperationInputs(input, QueryType.ASK);
            boolean result;
            try (QueryExecution queryExecution = QueryExecutionFactory.create(inputs.query, inputs.inputModel)) {
                result = queryExecution.execAsk();
            }
            data.getData().put(OUTPUT__OUTPUT, new BooleanType(result));
        } else if (OPERATION__SELECT.equalsIgnoreCase(operation)) {
            var outputContentType = Optional.ofNullable(getInputForName(input, INPUT__OUTPUT_CONTENT_TYPE, StringType.class))
                    .map(type -> ContentType.create((String) type.getValue()))
                    .orElse(APPLICATION_XML);
            BiConsumer<ByteArrayOutputStream, ResultSet> resultSetConsumer;
            if (APPLICATION_XML.equals(outputContentType) || TEXT_XML.equals(outputContentType) || APPLICATION_SPARQL_RESULTS_XML.equals(outputContentType)) {
                resultSetConsumer = ResultSetFormatter::outputAsXML;
            } else if (APPLICATION_JSON.equals(outputContentType) || APPLICATION_SPARQL_RESULTS_JSON.equals(outputContentType)) {
                resultSetConsumer = ResultSetFormatter::outputAsJSON;
            } else if (TEXT_TSV.equals(outputContentType)) {
                resultSetConsumer = ResultSetFormatter::outputAsTSV;
            } else if (TEXT_CSV.equals(outputContentType)) {
                resultSetConsumer = ResultSetFormatter::outputAsCSV;
            } else {
                throw new IllegalArgumentException("Provided content type [%s] is not supported for SPARQL result sets.".formatted(outputContentType.getContentTypeStr()));
            }
            QueryOperationInput inputs = parseQueryOperationInputs(input, QueryType.SELECT);
            var buffer = new ByteArrayOutputStream();
            try (QueryExecution queryExecution = QueryExecutionFactory.create(inputs.query, inputs.inputModel)) {
                ResultSet resultSet = queryExecution.execSelect();
                resultSetConsumer.accept(buffer, resultSet);
            }
            String result = buffer.toString(StandardCharsets.UTF_8);
            data.getData().put(OUTPUT__OUTPUT, new StringType(result));
        } else {
            throw new IllegalArgumentException("Unknown operation [" + operation + "]");
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

    private QueryOperationInput parseQueryOperationInputs(ProcessingData input, QueryType expectedQueryType) {
        var inputModel = getRequiredInputForName(input, INPUT__MODEL, StringType.class);
        var queryString = getRequiredInputForName(input, INPUT__QUERY, StringType.class);
        var inputContentType = getRequiredInputForName(input, INPUT__INPUT_CONTENT_TYPE, StringType.class);
        Lang inputLanguage = parseLanguage(inputContentType, () -> "Provided content type for input model [%s] is not a supported RDF type.".formatted(inputContentType.getValue()));
        Query query = parseQuery(queryString, expectedQueryType);
        Model model = parseModel(inputModel, inputLanguage);
        return new QueryOperationInput(model, inputLanguage, query);
    }

    private Query parseQuery(StringType queryString, QueryType expectedQueryType) {
        Query query = QueryFactory.create((String) queryString.getValue());
        if (query.queryType() != expectedQueryType) {
            throw new IllegalArgumentException("Unexpected query type [%s]".formatted(query.queryType().name()));
        }
        return query;
    }

    private Lang parseLanguage(StringType syntax, Supplier<String> messageSupplier) {
        var contentType = ContentType.create((String) syntax.getValue());
        Lang rdfLanguage = RDFLanguages.contentTypeToLang(contentType);
        if (rdfLanguage == null) {
            rdfLanguage = EQUIVALENT_CONTENT_TYPES.get(contentType.getContentTypeStr());
            if (rdfLanguage == null) {
                throw new IllegalArgumentException(messageSupplier.get());
            }
        }
        return rdfLanguage;
    }

    private Model parseModel(StringType modelContent, Lang modeLanguage) {
        Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader((String) modelContent.getValue()), null, modeLanguage.getName());
        return model;
    }

    private StringType toStringType(Model model, Lang outputLanguage) {
        var writer = new StringWriter();
        model.write(writer, outputLanguage.getName());
        return new StringType(writer.toString());
    }

    private record QueryOperationInput(Model inputModel, Lang inputLanguage, Query query) {}
}
