package com.gitb.engine.validation.handlers.schematron;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.validation.handlers.common.AbstractReportHandler;
import com.gitb.engine.validation.handlers.xml.DocumentNamespaceContext;
import com.gitb.tr.*;
import com.gitb.types.DataType;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import com.helger.schematron.svrl.AbstractSVRLMessage;
import com.helger.schematron.svrl.SVRLFailedAssert;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.SVRLSuccessfulReport;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by senan on 31.10.2014.
 */
public class SchematronReportHandler extends AbstractReportHandler {

    private static final Logger logger = LoggerFactory.getLogger(SchematronReportHandler.class);

    public static final String XML_ITEM_NAME  = "xml";
    public static final String SCH_ITEM_NAME  = "sch";
    private static final Pattern ARRAY_PATTERN = Pattern.compile("\\[\\d+\\]");
    private static final Pattern DEFAULTNS_PATTERN = Pattern.compile("\\/[\\w]+:?");

    private final Document node;
    private final SchematronOutputType svrlReport;
    private NamespaceContext namespaceContext;
    private final boolean convertXPathExpressions;
    private Boolean hasDefaultNamespace;

    protected SchematronReportHandler(ObjectType xml, SchemaType sch, Document node, SchematronOutputType svrl, boolean convertXPathExpressions) {
        super();

        this.node = node;
        this.svrlReport = svrl;
        this.convertXPathExpressions = convertXPathExpressions;

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
            List<SVRLFailedAssert> failedAssertions = SVRLHelper.getAllFailedAssertions(this.svrlReport);

            if(failedAssertions.size() > 0) {
                report.setResult(TestResultType.FAILURE);

                List<JAXBElement<TestAssertionReportType>> errorReports = traverseSVRLMessages(failedAssertions, true);
                report.getReports().getInfoOrWarningOrError().addAll(errorReports);
            }

            List<SVRLSuccessfulReport> successfulReports = SVRLHelper.getAllSuccessfulReports(this.svrlReport);
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

            JAXBElement<TestAssertionReportType> element;

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
        String xpathExpressionConverted = convertToXPathExpression(xpathExpression);
        XPath xPath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
        xPath.setNamespaceContext(getNamespaceContext());
        Node node;
        try {
            node = (Node) xPath.evaluate(xpathExpressionConverted, this.node, XPathConstants.NODE);
            return (String) node.getUserData("lineNumber");
        } catch (XPathExpressionException e) {
            logger.debug(e.getMessage());
            return "0";
        }
    }

    private String convertToXPathExpression(String xpathExpression) {
        /*
        Schematron reports arrays as 0-based whereas xpath has 1-based arrays.
        This is used to increment each array index by one.
         */
        if (isXPathConversionNeeded()) {
            try {
                StringBuffer s = new StringBuffer();
                Matcher m = ARRAY_PATTERN.matcher(xpathExpression);
                while (m.find()) {
                    m.appendReplacement(s, "["+String.valueOf(1 + Integer.parseInt(m.group(0).substring(1, m.group(0).length()-1)))+"]");
                }
                m.appendTail(s);
                if (documentHasDefaultNamespace(node)) {
                    m = DEFAULTNS_PATTERN.matcher(s.toString());
                    s.delete(0, s.length());
                    while (m.find()) {
                        String match = m.group(0);
                        if (match.indexOf(':') == -1) {
                            match = "/"+DocumentNamespaceContext.DEFAULT_NS+":"+match.substring(1);
                        }
                        m.appendReplacement(s, match);
                    }
                    m.appendTail(s);
                }
                return s.toString();
            } catch (Exception e) {
                logger.warn("Failed to convert XPath expression.", e);
                return xpathExpression;
            }
        } else {
            return xpathExpression;
        }
    }

    private boolean isXPathConversionNeeded() {
        return convertXPathExpressions;
    }

    private boolean documentHasDefaultNamespace(Document node) {
        if (hasDefaultNamespace == null) {
            NamedNodeMap attributes = node.getFirstChild().getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                if (attribute.getNodeName().equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                    hasDefaultNamespace = Boolean.TRUE;
                    break;
                }
            }
            if (hasDefaultNamespace == null) {
                hasDefaultNamespace = Boolean.FALSE;
            }
        }
        return hasDefaultNamespace.booleanValue();
    }

}
