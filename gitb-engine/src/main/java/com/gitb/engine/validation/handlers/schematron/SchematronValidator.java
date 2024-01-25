package com.gitb.engine.validation.handlers.schematron;

import com.gitb.core.Configuration;
import com.gitb.engine.utils.ReportItemComparator;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.AbstractValidator;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import com.gitb.utils.XMLUtils;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;
import com.helger.schematron.svrl.SVRLMarshaller;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import com.helger.schematron.xslt.SchematronResourceXSLT;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Created by senan on 24.10.2014.
 */
@ValidationHandler(name="SchematronValidator")
public class SchematronValidator extends AbstractValidator {

    public final static String CONTENT_ARGUMENT_NAME     = "xmldocument";
    public final static String SCHEMATRON_ARGUMENT_NAME  = "schematron";
    public final static String SCHEMATRON_TYPE_ARGUMENT_NAME = "type";
    public final static String SHOW_SCHEMATRON_ARGUMENT_NAME = "showSchematron";
    public final static String SORT_BY_SEVERITY_ARGUMENT_NAME  = "sortBySeverity";
    public final static String SHOW_TESTS_ARGUMENT_NAME = "showTests";
    private final static String MODULE_DEFINITION_XML = "/validation/schematron-validator-definition.xml";

    public SchematronValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        // Process inputs.
        ObjectType xml = (ObjectType) inputs.get(CONTENT_ARGUMENT_NAME).convertTo(DataType.OBJECT_DATA_TYPE);
        SchemaType sch = (SchemaType) inputs.get(SCHEMATRON_ARGUMENT_NAME).convertTo(DataType.SCHEMA_DATA_TYPE);
        var showSchematron = getAndConvert(inputs, SHOW_SCHEMATRON_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        var sortBySeverity = getAndConvert(inputs, SORT_BY_SEVERITY_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        var showTests = getAndConvert(inputs, SHOW_TESTS_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        // Process schematron resource.
        SchematronType validationType = determineSchematronType(inputs, sch);
        ISchematronResource schematron;
        boolean convertXPathExpressions = false;
        if (validationType == SchematronType.SCH) {
            // Use the pure implementation as SCH can be very resource and time-consuming
            schematron = new SchematronResourcePure(new StringResource(sch.toString(), sch.getImportPath()));
            convertXPathExpressions = true;
        } else {
            schematron = new SchematronResourceXSLT(new StringResource(sch.toString(), sch.getImportPath()));
            ((SchematronResourceXSLT) schematron).setURIResolver(new SchematronResolver(sch.getImportTestSuite(), getTestCaseId(inputs), sch.getImportPath()));
        }
        schematron.setUseCache(false);
        // Carry out validation.
        Document inputDocument;
        try {
            inputDocument = XMLUtils.readXMLWithLineNumbers(new ByteArrayInputStream(xml.serializeByDefaultEncoding()));
        } catch (Exception e) {
            throw new GITBEngineInternalError("Unable to read input as XML document.", e);
        }
        Document resultDocument;
        try {
            resultDocument = schematron.applySchematronValidation(new DOMSource(inputDocument));
        } catch (Exception e) {
            throw new GITBEngineInternalError("Invalid schematron file.", e);
        }
        if (resultDocument == null) {
            throw new GITBEngineInternalError("Invalid schematron file.");
        }
        SVRLMarshaller marshaller = new SVRLMarshaller(false);
        SchematronOutputType svrlOutput = marshaller.read(resultDocument);
        // Produce validation report.
        SchematronReportHandler handler = new SchematronReportHandler(
                xml,
                (showSchematron == null || (Boolean)showSchematron.getValue())?sch:null, inputDocument, svrlOutput,
                convertXPathExpressions,
                (showTests != null && ((Boolean) showTests.getValue()))
        );
        var report = handler.createReport();
        if (sortBySeverity != null && ((Boolean) sortBySeverity.getValue()) && report.getReports() != null) {
            report.getReports().getInfoOrWarningOrError().sort(new ReportItemComparator(ReportItemComparator.SortType.SEVERITY_THEN_LOCATION));
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
