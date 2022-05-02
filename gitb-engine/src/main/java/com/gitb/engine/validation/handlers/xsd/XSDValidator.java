package com.gitb.engine.validation.handlers.xsd;

import com.gitb.core.Configuration;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.AbstractValidator;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import com.gitb.utils.XMLUtils;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by senan on 9/16/14.
 */
@ValidationHandler(name="XSDValidator")
public class XSDValidator extends AbstractValidator {

    private final static String SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";
    private final static String CONTENT_ARGUMENT_NAME = "xmldocument";
    private final static String SCHEMA_ARGUMENT_NAME  = "xsddocument";
    private final static String MODULE_DEFINITION_XML = "/validation/xsd-validator-definition.xml";

    public XSDValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        //get inputs
        ObjectType contentToProcess = (ObjectType) inputs.get(CONTENT_ARGUMENT_NAME).convertTo(DataType.OBJECT_DATA_TYPE);
        SchemaType xsd = (SchemaType) inputs.get(SCHEMA_ARGUMENT_NAME).convertTo(DataType.SCHEMA_DATA_TYPE);

        //create error handler
        XSDReportHandler handler = new XSDReportHandler(contentToProcess, xsd);

        DocumentBuilderFactory factory;
        try {
            factory = XMLUtils.getSecureDocumentBuilderFactory();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
        factory.setNamespaceAware(true);

        //resolve schema
        SchemaFactory schemaFactory = SchemaFactory.newInstance(SCHEMA_LANGUAGE);
        schemaFactory.setErrorHandler(handler);
        schemaFactory.setResourceResolver(new XSDResolver(xsd.getImportTestSuite(), getTestCaseId(inputs), xsd.getImportPath()));
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
            StreamSource source = new StreamSource(new ByteArrayInputStream(contentToProcess.toString().getBytes()));
            validator.validate(source);
        } catch (Exception e) {
            throw new GITBEngineInternalError(e);
        }

        return handler.createReport();
    }
}
