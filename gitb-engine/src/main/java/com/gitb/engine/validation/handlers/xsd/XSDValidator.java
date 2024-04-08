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

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by senan on 9/16/14.
 */
@ValidationHandler(name="XSDValidator")
public class XSDValidator extends AbstractValidator {

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
        // Get inputs.
        ObjectType contentToProcess = (ObjectType) inputs.get(CONTENT_ARGUMENT_NAME).convertTo(DataType.OBJECT_DATA_TYPE);
        SchemaType xsd = (SchemaType) inputs.get(SCHEMA_ARGUMENT_NAME).convertTo(DataType.SCHEMA_DATA_TYPE);
        var showSchema = getAndConvert(inputs, SHOW_SCHEMA_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        var sortBySeverity = getAndConvert(inputs, SORT_BY_SEVERITY_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        // Create error handler.
        XSDReportHandler handler = new XSDReportHandler(contentToProcess, (showSchema == null || (Boolean)showSchema.getValue())?xsd:null);
        // Validate.
        try {
            XMLUtils.validateAgainstSchema(
                    // Use a StreamSource rather than a DomSource below to be able to get the line & column number of possible errors.
                    new StreamSource(new ByteArrayInputStream(contentToProcess.toString().getBytes())),
                    new DOMSource((Node)xsd.getValue()),
                    handler,
                    new XSDResolver(xsd.getImportTestSuite(), getTestCaseId(inputs), xsd.getImportPath())
            );
        } catch (XMLStreamException | SAXException e) {
            // This case would only come up if the content cannot be parsed.
            throw new GITBEngineInternalError("Unable to read input as XML document.", e);
        }
        var report = handler.createReport();
        if (sortBySeverity != null && ((Boolean) sortBySeverity.getValue()) && report.getReports() != null) {
            report.getReports().getInfoOrWarningOrError().sort(new ReportItemComparator(ReportItemComparator.SortType.SEVERITY_THEN_LOCATION));
        }
        return report;
    }
}
