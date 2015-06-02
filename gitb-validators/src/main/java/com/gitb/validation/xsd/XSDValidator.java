package com.gitb.validation.xsd;

import com.gitb.core.Configuration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import com.gitb.validation.IValidationHandler;
import com.gitb.validation.common.AbstractValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.*;
import org.kohsuke.MetaInfServices;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by senan on 9/16/14.
 */
@MetaInfServices(IValidationHandler.class)
public class XSDValidator extends AbstractValidator {

    private static final Logger logger = LoggerFactory.getLogger(XSDValidator.class);

    private final String SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";
    private final String CONTENT_ARGUMENT_NAME = "xmldocument";
    private final String SCHEMA_ARGUMENT_NAME  = "xsddocument";

    private final String MODULE_DEFINITION_XML = "/xsd-validator-definition.xml";

    public XSDValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        //get inputs
        ObjectType content = (ObjectType) inputs.get(CONTENT_ARGUMENT_NAME);
        SchemaType xsd     = (SchemaType) inputs.get(SCHEMA_ARGUMENT_NAME);

        //create error handler
        XSDReportHandler handler = new XSDReportHandler(content, xsd);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        //resolve schema
        SchemaFactory schemaFactory = SchemaFactory.newInstance(SCHEMA_LANGUAGE);
        schemaFactory.setErrorHandler(handler);
        schemaFactory.setResourceResolver(new XSDResolver(xsd.getSchemaLocation()));
        Schema schema;
        try {
            schema = schemaFactory.newSchema(new DOMSource((Node)xsd.getValue()));
        } catch (SAXException e) {
            throw new GITBEngineInternalError(e);
        }
        factory.setSchema(schema);

        //validate XML content against given XSD schema
        Validator validator = schema.newValidator();
        validator.setErrorHandler(handler);
        try {
            //Use a StreamSource rather than a DomSource below,
            //to be able to get the line & column number of possible errors
            StreamSource source = new StreamSource(new ByteArrayInputStream(content.toString().getBytes()));
            validator.validate(source);
        } catch (Exception e) {
            throw new GITBEngineInternalError(e);
        }

        return handler.createReport();
    }
}
