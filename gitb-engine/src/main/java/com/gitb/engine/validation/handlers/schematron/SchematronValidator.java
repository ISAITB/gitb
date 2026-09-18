/*
 * Copyright (C) 2026 European Union
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

package com.gitb.engine.validation.handlers.schematron;

import com.gitb.core.Configuration;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.AbstractValidator;
import com.gitb.engine.validation.handlers.common.XmlInputProvider;
import com.gitb.engine.validation.handlers.xml.XmlValidator;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.api.xslt.AbstractSchematronXSLTBasedResource;
import com.helger.schematron.pure.SchematronResourcePure;
import com.helger.schematron.svrl.SVRLMarshaller;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import com.helger.schematron.xslt.SchematronResourceXSLT;
import org.w3c.dom.Document;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Created by senan on 24.10.2014.
 */
@ValidationHandler(name="SchematronValidator")
public class SchematronValidator extends AbstractValidator {

    public static final String CONTENT_ARGUMENT_NAME     = "xml";
    public static final String SCHEMATRON_ARGUMENT_NAME  = "schematron";
    public static final String SCHEMATRON_TYPE_ARGUMENT_NAME = "type";
    public static final String SHOW_SCHEMATRON_ARGUMENT_NAME = "showSchematron";
    public static final String SORT_BY_SEVERITY_ARGUMENT_NAME  = "sortBySeverity";
    public static final String SHOW_TESTS_ARGUMENT_NAME = "showTests";
    public static final String SHOW_PATHS_ARGUMENT_NAME  = "showLocationPaths";
    public static final String FROM_XML_VALIDATOR_ARGUMENT_NAME  = "com.gitb.fromXmlValidator";
    private static final String MODULE_DEFINITION_XML = "/validation/schematron-validator-definition.xml";

    public SchematronValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        ObjectType xml = (ObjectType) inputs.get(CONTENT_ARGUMENT_NAME).convertTo(DataType.OBJECT_DATA_TYPE);
        return validate(configurations, inputs, new XmlInputProvider(xml));
    }

    /**
     * Carry out the validation using an {@link XmlInputProvider} supplied by the caller, rather than building one
     * from scratch. Used by {@link XmlValidator} to share a single serialisation of the input content (and, if
     * needed, a single line-numbered DOM) across the XSD and every Schematron validation of one {@code verify} step.
     *
     * @param configurations The step configuration.
     * @param inputs The step inputs.
     * @param inputProvider The shared input provider.
     * @return The validation report.
     */
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs, XmlInputProvider inputProvider) {
        // Process inputs.
        SchemaType sch = (SchemaType) inputs.get(SCHEMATRON_ARGUMENT_NAME).convertTo(DataType.SCHEMA_DATA_TYPE);
        var showSchematron = getAndConvert(inputs, SHOW_SCHEMATRON_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        var sortBySeverity = getAndConvert(inputs, SORT_BY_SEVERITY_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        var showTests = getAndConvert(inputs, SHOW_TESTS_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        var showPaths = getAndConvert(inputs, SchematronValidator.SHOW_PATHS_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        // Process schematron resource.
        SchematronType validationType = determineSchematronType(inputs, sch);
        ISchematronResource schematron;
        boolean convertXPathExpressions = false;
        Document resultDocument;
        // Only set (and only ever closed) for the pure engine - see below.
        TempFileSchematronResource pureResource = null;
        try {
            if (validationType == SchematronType.SCH) {
                /*
                 * Use the pure implementation as SCH can be very resource and time-consuming. Its content must be
                 * backed by a genuine temporary file rather than an in-memory StringResource: ph-commons reads a
                 * Schematron resource differently depending on whether it is actually an instanceof
                 * FileSystemResource (a fast, single, already-open stream) or not (a lazily re-opened one via
                 * ReadableResourceSAXInputSource) - and that lazy path was found to silently leave a real-world
                 * schema's own rules unregistered.
                 */
                pureResource = new TempFileSchematronResource(sch.toString(), sch.getImportPath());
                schematron = new SchematronResourcePure(pureResource);
                ((SchematronResourcePure) schematron).setErrorHandler(new PureSchematronErrorHandler());
                convertXPathExpressions = true;
            } else {
                schematron = new SchematronResourceXSLT(new StringResource(sch.toString(), sch.getImportPath()));
                ((SchematronResourceXSLT) schematron).setURIResolver(new SchematronResolver(sch.getImportTestSuite(), getTestCaseId(inputs), sch.getImportPath()));
            }
            schematron.setUseCache(false);
            // Carry out validation.
            boolean isXsltBased = schematron instanceof AbstractSchematronXSLTBasedResource<?>;
            Source source;
            if (isXsltBased) {
                /*
                 * XSLT-based Schematron (i.e. both compiled XSLT and SCH resources, which are compiled to XSLT
                 * internally) transform the input source directly, without ever needing a DOM. Streaming the input
                 * lets Saxon build its own compact, namespace-aware tree instead of adapting the line-numbered DOM
                 * built for error localisation (see XmlInputProvider.getLineNumberedDocument()), which is considerably
                 * faster for large documents. The Schematron rules' own namespace/prefix handling is unaffected: the
                 * SVRL @location values they produce depend only on the rules themselves, not on the input source type.
                 */
                source = inputProvider.newSource();
            } else {
                // Pure Schematron validation requires a DOM node as its input, and keeps using the line-numbered one:
                // switching it to a namespace-aware DOM would change the SVRL @location syntax it produces (prefixes
                // resolved from the Schematron's own <sch:ns> declarations rather than the document's), which
                // SchematronReportHandler could then fail to re-resolve against the document's own namespace context.
                source = new DOMSource(inputProvider.getLineNumberedDocument());
            }
            try {
                resultDocument = schematron.applySchematronValidation(source);
            } catch (TransformerException e) {
                if (isXsltBased) {
                    // The Schematron rules' own validity is checked internally by applySchematronValidation and
                    // surfaces as a null result (handled below), not an exception - so reaching here means the
                    // transformation of the *input* itself failed (e.g. malformed XML).
                    throw new GITBEngineInternalError("Unable to read input as XML content.", e);
                }
                throw new GITBEngineInternalError("Invalid schematron file.", e);
            } catch (Exception e) {
                if (schematron instanceof SchematronResourcePure pureSchematron
                        && pureSchematron.getErrorHandler() instanceof PureSchematronErrorHandler errorHandler
                        && errorHandler.isDueToExternalFunctionCall()) {
                    boolean isFromXmlValidator = inputs.containsKey(FROM_XML_VALIDATOR_ARGUMENT_NAME);
                    throw new GITBEngineInternalError("You are using Schematron rules provided in the pure Schematron format (a '.sch' file if provided as a file) within which you are referring to functions (built-in or custom). To be able to use functions you must (a) convert the Schematron file to XSLT format, (b) include the XSLT file in the test suite and import it from the test case, and (c) define in the test case's 'verify' step, the '%s' input of the '%s' with a value of 'xslt'.".formatted((isFromXmlValidator?XmlValidator.SCHEMATRON_TYPE_ARGUMENT_NAME:SCHEMATRON_TYPE_ARGUMENT_NAME), (isFromXmlValidator?XmlValidator.class.getSimpleName():SchematronValidator.class.getSimpleName())), e);
                } else {
                    throw new GITBEngineInternalError("Invalid schematron file.", e);
                }
            }
        } finally {
            if (pureResource != null) {
                pureResource.close();
            }
        }
        if (resultDocument == null) {
            throw new GITBEngineInternalError("Invalid schematron file.");
        }
        SVRLMarshaller marshaller = new SVRLMarshaller(false);
        SchematronOutputType svrlOutput = marshaller.read(resultDocument);
        // Produce validation report.
        SchematronReportHandler handler = new SchematronReportHandler(
                inputProvider.getContentAsString(),
                (showSchematron == null || showSchematron.getValue())?sch:null, inputProvider::getLineNumberedDocument, svrlOutput,
                convertXPathExpressions,
                (showTests != null && showTests.getValue()),
                (showPaths != null && showPaths.getValue())
        );
        var report = handler.createReport();
        if (sortBySeverity != null && sortBySeverity.getValue()) {
            sortReport(report, false);
        }
        return report;
    }

    private SchematronType determineSchematronType(Map<String, DataType> inputs, SchemaType schematron) {
        Optional<String> extensionToCheck = Optional.empty();
        if (inputs.containsKey(SCHEMATRON_TYPE_ARGUMENT_NAME)) {
            // Determine from input.
            String providedType = ((String) inputs.get(SCHEMATRON_TYPE_ARGUMENT_NAME).getValue()).toLowerCase(Locale.ROOT);
            extensionToCheck = Optional.of(providedType);
        }
        if (extensionToCheck.isEmpty()) {
            // Determine from file extension.
            if (schematron.getImportPath() != null) {
                int dotIndex = schematron.getImportPath().lastIndexOf('.');
                if (dotIndex != -1 && dotIndex < schematron.getImportPath().length() - 1) {
                    extensionToCheck = Optional.of(schematron.getImportPath().toLowerCase(Locale.ROOT).substring(dotIndex+1));
                }
            }
        }
        SchematronType result = SchematronType.SCH;
        if (extensionToCheck.isPresent() && (extensionToCheck.get().equals("xsl") || extensionToCheck.get().equals("xslt"))) {
            result = SchematronType.XSLT;
        }
        return result;
    }

    enum SchematronType {
        SCH, XSLT
    }
}
