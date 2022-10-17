package com.gitb.engine.validation.handlers.xsd;

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
    public final static String CONTENT_ARGUMENT_NAME = "xmldocument";
    public final static String SCHEMA_ARGUMENT_NAME  = "xsddocument";
    public final static String SHOW_SCHEMA_ARGUMENT_NAME  = "showSchema";
    public final static String SORT_BY_SEVERITY_ARGUMENT_NAME  = "sortBySeverity";
    private final static String MODULE_DEFINITION_XML = "/validation/xsd-validator-definition.xml";

    public XSDValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        //get inputs
        ObjectType contentToProcess = (ObjectType) inputs.get(CONTENT_ARGUMENT_NAME).convertTo(DataType.OBJECT_DATA_TYPE);
        SchemaType xsd = (SchemaType) inputs.get(SCHEMA_ARGUMENT_NAME).convertTo(DataType.SCHEMA_DATA_TYPE);
        var showSchema = getAndConvert(inputs, SHOW_SCHEMA_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        var sortBySeverity = getAndConvert(inputs, SORT_BY_SEVERITY_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);

        //create error handler
        XSDReportHandler handler = new XSDReportHandler(contentToProcess, (showSchema == null || (Boolean)showSchema.getValue())?xsd:null);

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
        var report = handler.createReport();
        if (sortBySeverity != null && ((Boolean) sortBySeverity.getValue()) && report.getReports() != null) {
            report.getReports().getInfoOrWarningOrError().sort(new ReportItemComparator(ReportItemComparator.SortType.SEVERITY_THEN_LOCATION));
        }
        return report;
    }
}
