package com.gitb.validation.schematron;

import com.gitb.core.Configuration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.BinaryType;
import com.gitb.types.DataType;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import com.gitb.utils.XMLUtils;
import com.gitb.validation.IValidationHandler;
import com.gitb.validation.common.AbstractValidator;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;
import com.helger.schematron.svrl.SVRLMarshaller;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import com.helger.schematron.xslt.SchematronResourceXSLT;
import org.kohsuke.MetaInfServices;
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
@MetaInfServices(IValidationHandler.class)
public class SchematronValidator extends AbstractValidator {

    private final static String CONTENT_ARGUMENT_NAME     = "xmldocument";
    private final static String SCHEMATRON_ARGUMENT_NAME  = "schematron";
    private final static String SCHEMATRON_TYPE  = "type";

    private final static String MODULE_DEFINITION_XML = "/schematron-validator-definition.xml";

    public SchematronValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        // Process inputs.
        ObjectType xml;
        DataType content = inputs.get(CONTENT_ARGUMENT_NAME);
        if (content instanceof BinaryType) {
            xml = new ObjectType();
            xml.deserialize((byte[])content.getValue());
        } else {
            xml = (ObjectType) inputs.get(CONTENT_ARGUMENT_NAME);
        }
        SchemaType sch = (SchemaType) inputs.get(SCHEMATRON_ARGUMENT_NAME);
        // Process schematron resource.
        SchematronType validationType = determineSchematronType(inputs, sch);
        ISchematronResource schematron;
        boolean convertXPathExpressions = false;
        if (validationType == SchematronType.SCH) {
            // Use the pure implementation as SCH can be very resource and time consuming
            schematron = new SchematronResourcePure(new StringResource(sch.toString(), sch.getSchemaLocation()));
            convertXPathExpressions = true;
        } else {
            schematron = new SchematronResourceXSLT(new StringResource(sch.toString(), sch.getSchemaLocation()));
            ((SchematronResourceXSLT) schematron).setURIResolver(new SchematronResolver(sch.getTestSuiteId(), getTestCaseId(), sch.getSchemaLocation()));
        }
        schematron.setUseCache(false);
        // Carry out validation.
        Document inputDocument;
        try {
            inputDocument = XMLUtils.readXMLWithLineNumbers(new ByteArrayInputStream(xml.serializeByDefaultEncoding()));
        } catch (Exception e) {
            throw new GITBEngineInternalError("Unable to read input as XML document.");
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
        SchematronReportHandler handler = new SchematronReportHandler(xml, sch, inputDocument, svrlOutput, convertXPathExpressions);
        return handler.createReport();
    }

    private SchematronType determineSchematronType(Map<String, DataType> inputs, SchemaType schematron) {
        Optional<String> extensionToCheck = Optional.empty();
        if (inputs.containsKey(SCHEMATRON_TYPE)) {
            // Determine from input.
            String providedType = ((String) inputs.get(SCHEMATRON_TYPE).getValue()).toLowerCase(Locale.ROOT);
            extensionToCheck = Optional.of(providedType);
        }
        if (extensionToCheck.isEmpty()) {
            // Determine from file extension.
            if (schematron.getSchemaLocation() != null) {
                int dotIndex = schematron.getSchemaLocation().lastIndexOf('.');
                if (dotIndex != -1 && dotIndex < schematron.getSchemaLocation().length() - 1) {
                    extensionToCheck = Optional.of(schematron.getSchemaLocation().toLowerCase(Locale.ROOT).substring(dotIndex+1));
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
