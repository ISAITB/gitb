package com.gitb.engine.validation.handlers.rdf;

import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.http.DefaultHttpClient;
import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.loader.FileLoader;
import com.apicatalog.jsonld.loader.HttpLoader;
import com.apicatalog.jsonld.loader.SchemeRouter;
import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.utils.ReportItemComparator;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.AbstractValidator;
import com.gitb.tr.*;
import com.gitb.types.*;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.models.ModelMaker;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapStd;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.shacl.validation.ValidationUtil;

import java.io.StringWriter;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Supplier;

import static org.apache.jena.riot.lang.LangJSONLD11.JSONLD_OPTIONS;

@ValidationHandler(name="ShaclValidator")
public class ShaclValidator extends AbstractValidator {

    private static final Logger LOG = LoggerFactory.getLogger(ShaclValidator.class);
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private final static String MODULE_DEFINITION_XML = "/validation/shacl-validator-definition.xml";

    private static final String MODEL_ARGUMENT_NAME = "model";
    private static final String CONTENT_TYPE_ARGUMENT_NAME = "contentType";
    private static final String SHAPES_ARGUMENT_NAME = "shapes";
    private static final String REPORT_CONTENT_TYPE_ARGUMENT_NAME = "reportContentType";
    private static final String SHOW_SHAPES_ARGUMENT_NAME = "showShapes";
    private static final String SORT_BY_SEVERITY_ARGUMENT_NAME = "sortBySeverity";
    private static final String SHOW_REPORT_ARGUMENT_NAME = "showReport";
    private static final String LOAD_IMPORTS_ARGUMENT_NAME = "loadImports";
    private static final String MERGE_MODELS_BEFORE_VALIDATION = "mergeModelsBeforeValidation";

    private static final String NS_SHACL = "http://www.w3.org/ns/shacl#";
    private static final String RESULT_URI = NS_SHACL+"result";
    private static final String RESULT_MESSAGE_URI = NS_SHACL+"resultMessage";
    private static final String FOCUS_NODE_URI = NS_SHACL+"focusNode";
    private static final String RESULT_PATH_URI = NS_SHACL+"resultPath";
    private static final String SOURCE_SHAPE_URI = NS_SHACL+"sourceShape";
    private static final String RESULT_SEVERITY_URI = NS_SHACL+"resultSeverity";
    private static final String VALUE_URI = NS_SHACL+"value";
    private static final String INFO_URI = NS_SHACL+"Info";
    private static final String WARNING_URI = NS_SHACL+"Warning";
    private static final Property VALIDATION_REPORT_PROPERTY = ResourceFactory.createProperty(NS_SHACL, "ValidationReport");
    private static final Property CONFORMS_PROPERTY = ResourceFactory.createProperty(NS_SHACL, "conforms");
    private static final ContentType APPLICATION_XML =  ContentType.create("application/xml");
    private static final ContentType TEXT_XML =  ContentType.create("text/xml");
    private static final ContentType APPLICATION_JSON =  ContentType.create("application/json");

    private static final Map<String, Lang> EQUIVALENT_CONTENT_TYPES = Map.of(
            APPLICATION_XML.getContentTypeStr(), RDFLanguages.RDFXML,
            TEXT_XML.getContentTypeStr(), RDFLanguages.RDFXML,
            APPLICATION_JSON.getContentTypeStr(), RDFLanguages.JSONLD
    );

    private static final Map<Lang, String> CONTENT_TYPES_FOR_REPORT = Map.of(
            RDFLanguages.RDFXML, APPLICATION_XML.getContentTypeStr(),
            RDFLanguages.JSONLD, APPLICATION_JSON.getContentTypeStr()
    );

    public ShaclValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        // Retrieve and check inputs.
        String modelContent = (String) Objects.requireNonNull(getAndConvert(inputs, MODEL_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class), "Input ["+MODEL_ARGUMENT_NAME+"] must be provided").getValue();
        Lang modelLanguage = Optional.of((String) Objects.requireNonNull(getAndConvert(inputs, CONTENT_TYPE_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class), "Input ["+CONTENT_TYPE_ARGUMENT_NAME+"] must be provided").getValue())
                .map(type -> parseLanguage(type, () -> "Unsupported content type [%s] provided for model".formatted(type)))
                .get();
        List<ContentInfo> shapes = Optional.ofNullable(getAndConvert(inputs, SHAPES_ARGUMENT_NAME, DataType.LIST_DATA_TYPE, ListType.class))
                .map(list -> {
                    // ListType of MapType
                    return list.getElements().stream()
                            .map(shapeData -> {
                                String content = (String) ((MapType)shapeData).getItem("content").convertTo(DataType.STRING_DATA_TYPE).getValue();
                                String contentType = (String) ((MapType)shapeData).getItem("contentType").convertTo(DataType.STRING_DATA_TYPE).getValue();
                                Lang language = parseLanguage(contentType, () -> "Unsupported content type [%s] provided for shapes".formatted(contentType));
                                return new ContentInfo(content, language);
                            })
                            .toList();
                })
                .orElseGet(Collections::emptyList);
        Lang reportContentType = Optional.ofNullable(getAndConvert(inputs, REPORT_CONTENT_TYPE_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class))
                .map(type -> parseLanguage((String) type.getValue(), () -> "Unsupported content type [%s] provided for report".formatted(type)))
                .orElse(modelLanguage);
        boolean showShapes = Optional.ofNullable(getAndConvert(inputs, SHOW_SHAPES_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(value -> (Boolean) value.getValue())
                .orElse(true);
        boolean sortBySeverity = Optional.ofNullable(getAndConvert(inputs, SORT_BY_SEVERITY_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(value -> (Boolean) value.getValue())
                .orElse(false);
        boolean showReport = Optional.ofNullable(getAndConvert(inputs, SHOW_REPORT_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(value -> (Boolean) value.getValue())
                .orElse(false);
        boolean loadImports = Optional.ofNullable(getAndConvert(inputs, LOAD_IMPORTS_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(value -> (Boolean) value.getValue())
                .orElse(false);
        boolean mergeModelsBeforeValidation = Optional.ofNullable(getAndConvert(inputs, MERGE_MODELS_BEFORE_VALIDATION, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(value -> (Boolean) value.getValue())
                .orElse(true);
        // Proceed with validation.
        Model inputModel;
        Model shapesModel;
        Model reportModel;
        if (shapes.isEmpty()) {
            // No validation to carry out.
            reportModel = emptyValidationReport();
            shapesModel = ModelFactory.createDefaultModel();
            inputModel = readModel(modelContent, modelLanguage);
        } else {
            shapesModel = getShapesModel(shapes);
            inputModel = getInputModel(new ContentInfo(modelContent, modelLanguage), shapesModel, loadImports, mergeModelsBeforeValidation);
            reportModel = ValidationUtil.validateModel(inputModel, shapesModel, false).getModel();
        }
        reportModel.setNsPrefix("sh", NS_SHACL);
        // Produce report.
        Optional<ModelInfo> shapesToReport = Optional.empty();
        if (showShapes) {
            shapesToReport = Optional.of(new ModelInfo(shapesModel, shapes.stream().findFirst().map(ContentInfo::language).orElse(modelLanguage)));
        }
        return createReport(new ReportSpecs(
                new ModelInfo(inputModel, modelLanguage),
                shapesToReport,
                new ModelInfo(reportModel, reportContentType),
                showShapes,
                showReport,
                sortBySeverity
        ));
    }

    private Model getInputModel(ContentInfo input, Model shapesModel, boolean loadImports, boolean mergeModelsBeforeValidation) {
        Model model = readModel(input.content(), input.language(), shapesModel.getNsPrefixMap());
        if (loadImports) {
            loadImportsForModel(model);
        }
        if (mergeModelsBeforeValidation) {
            model.add(shapesModel);
        }
        return model;
    }

    private Model getShapesModel(List<ContentInfo> shapes) {
        Model model = ModelFactory.createDefaultModel();
        for (var shapesInfo: shapes) {
            model.add(readModel(shapesInfo.content(), shapesInfo.language()));
        }
        // Load also owl:imports.
        loadImportsForModel(model);
        return model;
    }

    private Model readModel(String content, Lang language) {
        return readModel(content, language, null);
    }

    private Model readModel(String content, Lang language, Map<String, String> nsPrefixes) {
        var builder = RDFParserBuilder
                .create()
                .lang(language)
                .fromString(content);
        if (nsPrefixes != null) {
            // Before parsing set the prefixes of the model to avoid mismatches.
            PrefixMap prefixes = new PrefixMapStd();
            prefixes.putAll(nsPrefixes);
            builder = builder.prefixes(prefixes);
        }
        if (Lang.JSONLD11.equals(language) || Lang.JSONLD.equals(language)) {
            var options = new JsonLdOptions();
            var httpLoader = new HttpLoader(DefaultHttpClient.defaultInstance());
            /*
             * Set fallback type for remote contexts to avoid errors for non JSON/JSON-LD Content Types.
             * This allows us to proceed if e.g. the Content Type originally returned is "text/plain".
             */
            httpLoader.fallbackContentType(MediaType.JSON);
            options.setDocumentLoader(new SchemeRouter()
                    .set("http", httpLoader)
                    .set("https", httpLoader)
                    .set("file", new FileLoader()));
            builder = builder.context(Context.create().set(JSONLD_OPTIONS, options));
        }
        return builder.build().toModel();
    }

    private Lang parseLanguage(String contentTypeString, Supplier<String> messageSupplier) {
        ContentType contentType = ContentType.create(contentTypeString);
        Lang rdfLanguage = RDFLanguages.contentTypeToLang(contentType);
        if (rdfLanguage == null) {
            rdfLanguage = EQUIVALENT_CONTENT_TYPES.get(contentType.getContentTypeStr());
            if (rdfLanguage == null) {
                throw new IllegalArgumentException(messageSupplier.get());
            }
        }
        return rdfLanguage;
    }


    /**
     * Create an empty SHACL validation report.
     *
     * @return The report's model.
     */
    private Model emptyValidationReport() {
        Model reportModel = ModelFactory.createDefaultModel();
        List<Statement> statements = new ArrayList<>();
        Resource reportResource = reportModel.createResource();
        statements.add(reportModel.createStatement(reportResource, RDF.type, VALIDATION_REPORT_PROPERTY));
        statements.add(reportModel.createLiteralStatement(reportResource, CONFORMS_PROPERTY, true));
        reportModel.add(statements);
        return reportModel;
    }

    private String toString(ModelInfo modelInfo) {
        var writer = new StringWriter();
        modelInfo.model().write(writer, modelInfo.language().getName());
        return writer.toString();
    }

    private String toContentTypeForStepReport(Lang language) {
        return CONTENT_TYPES_FOR_REPORT.getOrDefault(language, language.getContentType().getContentTypeStr());
    }

    private TAR createReport(ReportSpecs reportSpecs) {
        var report = TestCaseUtils.createEmptyReport();
        report.setName("SHACL validation");
        var context = new AnyContent();
        report.setContext(context);
        // Input model.
        context.getItem().add(toAnyContent(reportSpecs.inputInfo(), "model"));
        // Shapes model.
        if (reportSpecs.showShapes() && reportSpecs.shapesInfo().isPresent()) {
            context.getItem().add(toAnyContent(reportSpecs.shapesInfo().get(), "shapes"));
        }
        // Report model.
        if (reportSpecs.showReport()) {
            context.getItem().add(toAnyContent(reportSpecs.reportInfo(), "report"));
        }
        // Add report items.
        addReportItems(report, reportSpecs.reportInfo().model(), reportSpecs.sortReport());
        return report;
    }

    private AnyContent toAnyContent(ModelInfo modelInfo, String name) {
        var contextItem = new AnyContent();
        contextItem.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        contextItem.setName(name);
        contextItem.setType("string");
        contextItem.setForContext(true);
        contextItem.setForDisplay(true);
        contextItem.setValue(toString(modelInfo));
        contextItem.setMimeType(toContentTypeForStepReport(modelInfo.language()));
        return contextItem;
    }

    private void addReportItems(TAR report, Model reportModel, boolean sortBySeverity) {
        NodeIterator niValidationResult = reportModel.listObjectsOfProperty(reportModel.getProperty(RESULT_URI));
        var reports = new ArrayList<JAXBElement<TestAssertionReportType>>();
        int infos = 0;
        int warnings = 0;
        int errors = 0;
        while (niValidationResult.hasNext()) {
            RDFNode node = niValidationResult.next();
            StmtIterator it = reportModel.listStatements(node.asResource(), null, (RDFNode)null);
            BAR error = new BAR();
            String message = "";
            String focusNode = "";
            String resultPath = "";
            String severity = "";
            String value = "";
            String shape = "";
            while (it.hasNext()) {
                Statement statement = it.next();
                if (statement.getPredicate().hasURI(RESULT_MESSAGE_URI)) {
                    message = getStatementSafe(statement);
                }
                if (statement.getPredicate().hasURI(FOCUS_NODE_URI)) {
                    focusNode = getStatementSafe(statement);
                }
                if (statement.getPredicate().hasURI(RESULT_PATH_URI)) {
                    resultPath = getStatementSafe(statement);
                }
                if (statement.getPredicate().hasURI(SOURCE_SHAPE_URI)) {
                    shape = getStatementSafe(statement);
                }
                if (statement.getPredicate().hasURI(RESULT_SEVERITY_URI)) {
                    severity = getStatementSafe(statement);
                }
                if (statement.getPredicate().hasURI(VALUE_URI)) {
                    value = getStatementSafe(statement);
                }
            }
            error.setDescription(message);
            error.setLocation(createStringMessageFromParts(new String[] { "Focus node", "Result path" }, new String[] { focusNode, resultPath }));
            error.setTest(createStringMessageFromParts(new String[] { "Shape", "Value" }, new String[] { shape, value }));
            JAXBElement<TestAssertionReportType> element;
            if (isInfoSeverity(severity)) {
                element = OBJECT_FACTORY.createTestAssertionGroupReportsTypeInfo(error);
                infos += 1;
            } else if (isWarningSeverity(severity)) {
                element = OBJECT_FACTORY.createTestAssertionGroupReportsTypeWarning(error);
                warnings += 1;
            } else { // ERROR, FATAL_ERROR
                element = OBJECT_FACTORY.createTestAssertionGroupReportsTypeError(error);
                errors += 1;
            }
            reports.add(element);
        }
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfErrors(BigInteger.valueOf(errors));
        report.getCounters().setNrOfAssertions(BigInteger.valueOf(infos));
        report.getCounters().setNrOfWarnings(BigInteger.valueOf(warnings));
        if (errors > 0) {
            report.setResult(TestResultType.FAILURE);
        } else {
            report.setResult(TestResultType.SUCCESS);
        }
        if (sortBySeverity) {
            reports.sort(new ReportItemComparator(ReportItemComparator.SortType.SEVERITY_THEN_LOCATION));
        }
        report.setReports(new TestAssertionGroupReportsType());
        report.getReports().getInfoOrWarningOrError().addAll(reports);
    }

    private boolean isInfoSeverity(String severity) {
        return Objects.equals(severity, INFO_URI);
    }

    private boolean isWarningSeverity(String severity) {
        return Objects.equals(severity, WARNING_URI);
    }

    private String createStringMessageFromParts(String[] labels, String[] values) {
        if (labels.length != values.length) {
            throw new IllegalArgumentException("Wrong number of arguments supplied ["+labels.length+"]["+values.length+"]");
        }
        StringBuilder str = new StringBuilder();
        for (int i=0; i < labels.length; i++) {
            if (StringUtils.isNotBlank(values[i])) {
                if (!str.isEmpty()) {
                    str.append(" - ");
                }
                str.append(String.format("[%s] - [%s]", labels[i], values[i]));
            }
        }
        if (!str.isEmpty()) {
            return str.toString();
        } else {
            return null;
        }
    }

    /**
     * Convert the provided RDF statement to a string.
     *
     * @param statement The statement.
     * @return The resulting string.
     */
    private String getStatementSafe(Statement statement) {
        String result = null;
        if (statement != null) {
            try {
                RDFNode node = statement.getObject();
                if (node.isAnon()) {
                    result = "";
                } else if (node.isLiteral()) {
                    result = node.asLiteral().getLexicalForm();
                } else {
                    result = node.toString();
                }
            } catch (Exception e) {
                LOG.warn("Error while getting statement string", e);
                result = "";
            }
        }
        return result;
    }

    private void loadImportsForModel(Model model) {
        ModelMaker modelMaker = ModelFactory.createMemModelMaker();
        OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM_RULE_INF);
        spec.setBaseModelMaker(modelMaker);
        spec.setImportModelMaker(modelMaker);
        addImports(model, ModelFactory.createOntologyModel(spec, model), new HashSet<>());
    }

    private void addImports(Model model, OntModel baseOntModel, Set<String> reachedURIs) {
        baseOntModel.loadImports();
        Set<String> listImportedURI = baseOntModel.listImportedOntologyURIs();
        for (String importedURI: listImportedURI) {
            if (!reachedURIs.contains(importedURI)) {
                OntModel importedModel = baseOntModel.getImportedModel(importedURI);
                if (importedModel != null) {
                    model.add(importedModel.getBaseModel());
                    // Avoid re-processing the same URIs.
                    reachedURIs.add(importedURI);
                    addImports(model, importedModel, reachedURIs);
                }
            }
        }
    }

    private record ReportSpecs(ModelInfo inputInfo, Optional<ModelInfo> shapesInfo, ModelInfo reportInfo, boolean showShapes, boolean showReport, boolean sortReport) {}
    private record ContentInfo(String content, Lang language) {}
    private record ModelInfo(Model model, Lang language) {}

}
