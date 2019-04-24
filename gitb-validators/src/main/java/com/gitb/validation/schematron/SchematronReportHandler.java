package com.gitb.validation.schematron;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.types.DataType;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import com.gitb.validation.common.AbstractReportHandler;
import com.gitb.validation.xml.DocumentNamespaceContext;
import com.helger.schematron.svrl.AbstractSVRLMessage;
import com.helger.schematron.svrl.SVRLFailedAssert;
import com.helger.schematron.svrl.SVRLSuccessfulReport;
import com.helger.schematron.svrl.SVRLUtils;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by senan on 31.10.2014.
 */
public class SchematronReportHandler extends AbstractReportHandler {

    private static final Logger logger = LoggerFactory.getLogger(SchematronReportHandler.class);

    public static final String XML_ITEM_NAME  = "XML";
    public static final String SCH_ITEM_NAME  = "SCH";

    private Document node;
    private SchematronOutputType svrlReport;
    private NamespaceContext namespaceContext;

    protected SchematronReportHandler(ObjectType xml, SchemaType sch, Document node, SchematronOutputType svrl) {
        super();

        this.node = node;
        this.svrlReport = svrl;

        report.setName("Schematron Validation");
        report.setReports(new TestAssertionGroupReportsType());

	    AnyContent attachment = new AnyContent();
	    attachment.setType(DataType.MAP_DATA_TYPE);

	    AnyContent xmlAttachment = new AnyContent();
	    xmlAttachment.setName(XML_ITEM_NAME);
	    xmlAttachment.setType(DataType.OBJECT_DATA_TYPE);
	    xmlAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
	    xmlAttachment.setValue(new String(xml.serializeByDefaultEncoding()));
	    attachment.getItem().add(xmlAttachment);

	    AnyContent schemaAttachment = new AnyContent();
	    schemaAttachment.setName(SCH_ITEM_NAME);
	    schemaAttachment.setType(DataType.SCHEMA_DATA_TYPE);
	    schemaAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
	    schemaAttachment.setValue(new String(sch.serializeByDefaultEncoding()));
	    attachment.getItem().add(schemaAttachment);

	    report.setContext(attachment);
    }

    @Override
    public TAR createReport() {
        if (svrlReport != null) {
            List<SVRLFailedAssert> failedAssertions = SVRLUtils.getAllFailedAssertions(this.svrlReport);

            if(failedAssertions.size() > 0) {
                report.setResult(TestResultType.FAILURE);

                List<JAXBElement<TestAssertionReportType>> errorReports = traverseSVRLMessages(failedAssertions, true);
                report.getReports().getInfoOrWarningOrError().addAll(errorReports);
            }

            List<SVRLSuccessfulReport> successfulReports = SVRLUtils.getAllSuccesssfulReports(this.svrlReport);
            if(successfulReports.size() > 0) {
                List<JAXBElement<TestAssertionReportType>> successReports = traverseSVRLMessages(successfulReports, false);
                report.getReports().getInfoOrWarningOrError().addAll(successReports);
            }
        } else {
            //occurs when validator fails to generate SVRL, so create a default error an add to the report
            report.setResult(TestResultType.FAILURE);

            BAR error = new BAR();
            error.setDescription("An error occurred when generating Schematron output due to a problem in given XML content.");
            error.setLocation(XML_ITEM_NAME + ":1:0");

            JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeError(error);
            report.getReports().getInfoOrWarningOrError().add(element);
        }

        return report;
    }

    private <T extends AbstractSVRLMessage> List<JAXBElement<TestAssertionReportType> > traverseSVRLMessages(List<T> svrlMessages, boolean failure){
        List<JAXBElement<TestAssertionReportType>> reports = new ArrayList<>();

        for(T message : svrlMessages) {
            BAR error = new BAR();
            error.setDescription(message.getText());
            error.setLocation(XML_ITEM_NAME + ":" + getLineNumbeFromXPath(message.getLocation()) + ":0");
            error.setTest(message.getTest());

            JAXBElement<TestAssertionReportType> element = null;

            if(failure) {
                element = objectFactory.createTestAssertionGroupReportsTypeError(error);
            } else {
                element = objectFactory.createTestAssertionGroupReportsTypeInfo(error);
            }
            reports.add(element);
        }

        return reports;
    }

    private NamespaceContext getNamespaceContext() {
        if (namespaceContext == null) {
            namespaceContext = new DocumentNamespaceContext(node, false);
        }
        return namespaceContext;
    }

    private String getLineNumbeFromXPath(String xpathExpression) {
        XPath xPath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
        xPath.setNamespaceContext(getNamespaceContext());
        Node node;
        try {
            node = (Node) xPath.evaluate(xpathExpression, this.node, XPathConstants.NODE);
            return (String) node.getUserData("lineNumber");
        } catch (XPathExpressionException e) {
            logger.debug(e.getMessage());
            return "0";
        }
    }
}
