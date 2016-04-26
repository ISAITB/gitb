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
import com.helger.schematron.xslt.SchematronResourceSCH;
import com.helger.schematron.xslt.SchematronResourceXSLT;
import org.kohsuke.MetaInfServices;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by senan on 24.10.2014.
 */
@MetaInfServices(IValidationHandler.class)
public class SchematronValidator extends AbstractValidator {
    private static final Logger logger = LoggerFactory.getLogger(SchematronValidator.class);

    private final String CONTENT_ARGUMENT_NAME     = "xmldocument";
    private final String SCHEMATRON_ARGUMENT_NAME  = "schematron";

    private final String MODULE_DEFINITION_XML = "/schematron-validator-definition.xml";

    public SchematronValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        //get inputs
        ObjectType xml;
        DataType content = (DataType) inputs.get(CONTENT_ARGUMENT_NAME);
        if (content instanceof BinaryType) {
            xml = new ObjectType();
            xml.deserialize((byte[])content.getValue());
        } else {
            xml = (ObjectType) inputs.get(CONTENT_ARGUMENT_NAME);
        }
        SchemaType sch = (SchemaType) inputs.get(SCHEMATRON_ARGUMENT_NAME);

        Node schematronInput;
        SchematronOutputType svrlOutput;

        StringResource resource         = new StringResource(sch.toString(), sch.getSchemaLocation());
        ByteArrayInputStream stream     = new ByteArrayInputStream(xml.serializeByDefaultEncoding());

        ISchematronResource schematron;

        if(sch.getSchemaLocation().endsWith(".sch")) {
            schematron  = new SchematronResourceSCH(resource, null, new SchematronResolver());
        }
        else if(sch.getSchemaLocation().endsWith(".xsl")) {
            schematron  = new SchematronResourceXSLT(resource, null, new SchematronResolver());
        }
        else {
            throw new GITBEngineInternalError("Invalid schematron extension. Must be either .sch or .xsl");
        }

        //apply schematron validation
        if(schematron.isValidSchematron()) {
            try {
                schematronInput = XMLUtils.readXMLWithLineNumbers(stream);
                Source source   = new DOMSource(schematronInput);
                svrlOutput      = schematron.applySchematronValidationToSVRL(source);
            } catch (Exception e) {
                throw new GITBEngineInternalError(e);
            }
        }
        else{
            throw new GITBEngineInternalError("Invalid Schematron File");
        }

        //handle validation report
        SchematronReportHandler handler = new SchematronReportHandler(xml, sch, schematronInput, svrlOutput);
        return handler.createReport();
    }
}
