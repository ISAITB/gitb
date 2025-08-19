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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@ProcessingHandler(name="RdfUtils")
public class RdfUtils extends AbstractProcessingHandler {

    private static final String OPERATION_CONVERT = "convert";
    private static final String OPERATION_MERGE = "merge";
    private static final String OPERATION_ASK = "ask";
    private static final String OPERATION_SELECT = "select";
    private static final String OPERATION_CONSTRUCT = "construct";

    private static final String INPUT_MODEL = "model";
    private static final String INPUT_MODELS = "models";
    private static final String INPUT_QUERY = "query";
    private static final String INPUT_INPUT_CONTENT_TYPE = "inputContentType";
    private static final String INPUT_INPUT_CONTENT_TYPES = "inputContentTypes";
    private static final String INPUT_OUTPUT_CONTENT_TYPE = "outputContentType";
    private static final String OUTPUT_OUTPUT = "output";

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
        module.getOperation().add(createProcessingOperation(OPERATION_CONVERT,
                List.of(
                        createParameter(INPUT_MODEL, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The RDF model to process."),
                        createParameter(INPUT_INPUT_CONTENT_TYPE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The content type of the input model."),
                        createParameter(INPUT_OUTPUT_CONTENT_TYPE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The content type of the output model.")
                ),
                List.of(
                        createParameter(OUTPUT_OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The converted model.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_MERGE,
                List.of(
                        createParameter(INPUT_MODELS, "list", UsageEnumeration.R, ConfigurationType.SIMPLE, "The list of models to merge."),
                        createParameter(INPUT_INPUT_CONTENT_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The content type to consider for all provided models."),
                        createParameter(INPUT_INPUT_CONTENT_TYPES, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The content types of the provided models."),
                        createParameter(INPUT_OUTPUT_CONTENT_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The content type of the output model.")
                ),
                List.of(
                        createParameter(OUTPUT_OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The merged model.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_ASK,
                List.of(
                        createParameter(INPUT_MODEL, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The model to query."),
                        createParameter(INPUT_QUERY, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The SPARQL query."),
                        createParameter(INPUT_INPUT_CONTENT_TYPE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The model's content type.")
                ),
                List.of(
                        createParameter(OUTPUT_OUTPUT, "boolean", UsageEnumeration.R, ConfigurationType.SIMPLE, "The query result.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_CONSTRUCT,
                List.of(
                        createParameter(INPUT_MODEL, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The model to query."),
                        createParameter(INPUT_QUERY, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The SPARQL query."),
                        createParameter(INPUT_INPUT_CONTENT_TYPE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The model's content type."),
                        createParameter(INPUT_OUTPUT_CONTENT_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The content type of the constructed model.")
                ),
                List.of(
                        createParameter(OUTPUT_OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The output result set.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_SELECT,
                List.of(
                        createParameter(INPUT_MODEL, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The model to query."),
                        createParameter(INPUT_QUERY, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The SPARQL query."),
                        createParameter(INPUT_INPUT_CONTENT_TYPE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The model's content type."),
                        createParameter(INPUT_OUTPUT_CONTENT_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The content type of the result set.")
                ),
                List.of(
                        createParameter(OUTPUT_OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The output result set.")
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
        if (OPERATION_CONVERT.equalsIgnoreCase(operation)) {
            // Read inputs.
            var inputModel = getRequiredInputForName(input, INPUT_MODEL, StringType.class);
            var inputContentType = getRequiredInputForName(input, INPUT_INPUT_CONTENT_TYPE, StringType.class);
            var outputContentType = getRequiredInputForName(input, INPUT_OUTPUT_CONTENT_TYPE, StringType.class);
            // Parse and ensure from/to formats are valid.
            Lang inputLanguage = parseLanguage(inputContentType, () -> "Provided content type [%s] for input model is not a supported RDF type.".formatted(inputContentType.getValue()));
            Lang outputLanguage = parseLanguage(outputContentType, () -> "Provided content type [%s] for output model is not a supported RDF type.".formatted(outputContentType.getValue()));
            // Convert model.
            Model model = parseModel(inputModel, inputLanguage);
            // Write output
            data.getData().put(OUTPUT_OUTPUT, toStringType(model, outputLanguage));
        } else if (OPERATION_MERGE.equalsIgnoreCase(operation)) {
            var inputModels = getRequiredInputForName(input, INPUT_MODELS, ListType.class);
            // Parse input language.
            List<Lang> inputLanguages = Optional.ofNullable(getInputForName(input, INPUT_INPUT_CONTENT_TYPES, ListType.class))
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
                        var inputContentType = Optional.ofNullable(getInputForName(input, INPUT_INPUT_CONTENT_TYPE, StringType.class));
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
            Lang outputLanguage = Optional.ofNullable(getInputForName(input, INPUT_OUTPUT_CONTENT_TYPE, StringType.class))
                    .map(type -> parseLanguage(type, () -> "Provided content type for merged model [%s] is not a supported RDF type.".formatted(type.getValue())))
                    .orElseGet(inputLanguages::getFirst);
            // Merge models.
            Model outputModel = ModelFactory.createDefaultModel();
            for (int i = 0; i < inputModels.getSize(); i++) {
                Model currentModel = parseModel((StringType) inputModels.getItem(i).convertTo(DataType.STRING_DATA_TYPE), inputLanguages.get(i));
                outputModel.add(currentModel);
            }
            // Write output
            data.getData().put(OUTPUT_OUTPUT, toStringType(outputModel, outputLanguage));
        } else if (OPERATION_CONSTRUCT.equalsIgnoreCase(operation)) {
            QueryOperationInput inputs = parseQueryOperationInputs(input, QueryType.CONSTRUCT);
            Lang outputLanguage = Optional.ofNullable(getInputForName(input, INPUT_OUTPUT_CONTENT_TYPE, StringType.class))
                    .map(type -> parseLanguage(type, () -> "Provided content type for output model [%s] is not a supported RDF type.".formatted(type.getValue())))
                    .orElse(inputs.inputLanguage);
            Model outputModel;
            try (QueryExecution queryExecution = QueryExecutionFactory.create(inputs.query, inputs.inputModel)) {
                outputModel = queryExecution.execConstruct();
            }
            data.getData().put(OUTPUT_OUTPUT, toStringType(outputModel, outputLanguage));
        } else if (OPERATION_ASK.equalsIgnoreCase(operation)) {
            QueryOperationInput inputs = parseQueryOperationInputs(input, QueryType.ASK);
            boolean result;
            try (QueryExecution queryExecution = QueryExecutionFactory.create(inputs.query, inputs.inputModel)) {
                result = queryExecution.execAsk();
            }
            data.getData().put(OUTPUT_OUTPUT, new BooleanType(result));
        } else if (OPERATION_SELECT.equalsIgnoreCase(operation)) {
            var outputContentType = Optional.ofNullable(getInputForName(input, INPUT_OUTPUT_CONTENT_TYPE, StringType.class))
                    .map(type -> ContentType.create(type.getValue()))
                    .orElse(APPLICATION_XML);
            BiConsumer<ByteArrayOutputStream, ResultSet> resultSetConsumer = getResultSetConsumer(outputContentType);
            QueryOperationInput inputs = parseQueryOperationInputs(input, QueryType.SELECT);
            var buffer = new ByteArrayOutputStream();
            try (QueryExecution queryExecution = QueryExecutionFactory.create(inputs.query, inputs.inputModel)) {
                ResultSet resultSet = queryExecution.execSelect();
                resultSetConsumer.accept(buffer, resultSet);
            }
            String result = buffer.toString(StandardCharsets.UTF_8);
            data.getData().put(OUTPUT_OUTPUT, new StringType(result));
        } else {
            throw new IllegalArgumentException("Unknown operation [" + operation + "]");
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

    private BiConsumer<ByteArrayOutputStream, ResultSet> getResultSetConsumer(ContentType outputContentType) {
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
        return resultSetConsumer;
    }

    private QueryOperationInput parseQueryOperationInputs(ProcessingData input, QueryType expectedQueryType) {
        var inputModel = getRequiredInputForName(input, INPUT_MODEL, StringType.class);
        var queryString = getRequiredInputForName(input, INPUT_QUERY, StringType.class);
        var inputContentType = getRequiredInputForName(input, INPUT_INPUT_CONTENT_TYPE, StringType.class);
        Lang inputLanguage = parseLanguage(inputContentType, () -> "Provided content type for input model [%s] is not a supported RDF type.".formatted(inputContentType.getValue()));
        Query query = parseQuery(queryString, expectedQueryType);
        Model model = parseModel(inputModel, inputLanguage);
        return new QueryOperationInput(model, inputLanguage, query);
    }

    private Query parseQuery(StringType queryString, QueryType expectedQueryType) {
        Query query = QueryFactory.create(queryString.getValue());
        if (query.queryType() != expectedQueryType) {
            throw new IllegalArgumentException("Unexpected query type [%s]".formatted(query.queryType().name()));
        }
        return query;
    }

    private Lang parseLanguage(StringType syntax, Supplier<String> messageSupplier) {
        var contentType = ContentType.create(syntax.getValue());
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
        model.read(new StringReader(modelContent.getValue()), null, modeLanguage.getName());
        return model;
    }

    private StringType toStringType(Model model, Lang outputLanguage) {
        var writer = new StringWriter();
        model.write(writer, outputLanguage.getName());
        return new StringType(writer.toString());
    }

    private record QueryOperationInput(Model inputModel, Lang inputLanguage, Query query) {}
}
