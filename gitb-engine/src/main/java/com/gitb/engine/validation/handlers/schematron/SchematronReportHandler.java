package com.gitb.engine.validation.handlers.schematron;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.validation.handlers.common.AbstractReportHandler;
import com.gitb.engine.validation.handlers.xml.DocumentNamespaceContext;
import com.gitb.tr.*;
import com.gitb.types.DataType;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.schematron.svrl.AbstractSVRLMessage;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBElement;
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
    private final boolean showTests;
    private final boolean showPaths;
    private Boolean hasDefaultNamespace;

    protected SchematronReportHandler(ObjectType xml, SchemaType sch, Document node, SchematronOutputType svrl, boolean convertXPathExpressions, boolean showTests, boolean showPaths) {
        super();

        this.node = node;
        this.svrlReport = svrl;
        this.convertXPathExpressions = convertXPathExpressions;
        this.showTests = showTests;
        this.showPaths = showPaths;

        report.setName("Schematron Validation");
        report.setReports(new TestAssertionGroupReportsType());

	    AnyContent attachment = new AnyContent();
	    attachment.setType(DataType.MAP_DATA_TYPE);

	    AnyContent xmlAttachment = new AnyContent();
	    xmlAttachment.setName(XML_ITEM_NAME);
        xmlAttachment.setMimeType(MediaType.APPLICATION_XML_VALUE);
	    xmlAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
	    xmlAttachment.setValue(new String(xml.serializeByDefaultEncoding()));
	    attachment.getItem().add(xmlAttachment);

        if (sch != null) {
            AnyContent schemaAttachment = new AnyContent();
            schemaAttachment.setName(SCH_ITEM_NAME);
            schemaAttachment.setType(DataType.SCHEMA_DATA_TYPE);
            schemaAttachment.setMimeType(MediaType.APPLICATION_XML_VALUE);
            schemaAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            schemaAttachment.setValue(new String(sch.serializeByDefaultEncoding()));
            attachment.getItem().add(schemaAttachment);
        }

	    report.setContext(attachment);
    }

    private <T extends AbstractSVRLMessage> TestResultType getErrorLevel(List<T> messages) {
        for (AbstractSVRLMessage item: messages) {
            if (item.getFlag().getNumericLevel() == EErrorLevel.ERROR.getNumericLevel()
                    || item.getFlag().getNumericLevel() == EErrorLevel.FATAL_ERROR.getNumericLevel()) {
                return TestResultType.FAILURE;
            }
        }
        return TestResultType.SUCCESS;
    }

    @Override
    public TAR createReport() {
        if (svrlReport != null) {
            var failedAssertions = SVRLHelper.getAllFailedAssertions(this.svrlReport);

            if (!failedAssertions.isEmpty()) {
                report.setResult(getErrorLevel(failedAssertions));
                var errorReports = traverseSVRLMessages(failedAssertions);
                report.getReports().getInfoOrWarningOrError().addAll(errorReports);
            }

            var successfulReports = SVRLHelper.getAllSuccessfulReports(this.svrlReport);
            if (!successfulReports.isEmpty()) {
                var successReports = traverseSVRLMessages(successfulReports);
                report.getReports().getInfoOrWarningOrError().addAll(successReports);
            }
        } else {
            // Occurs when validator fails to generate SVRL, so create a default error an add to the report
            report.setResult(TestResultType.FAILURE);

            var error = new BAR();
            error.setDescription("An error occurred when generating Schematron output due to a problem in given XML content.");
            error.setLocation(XML_ITEM_NAME + ":1:0");

            var element = objectFactory.createTestAssertionGroupReportsTypeError(error);
            report.getReports().getInfoOrWarningOrError().add(element);
        }

        return report;
    }

    private <T extends AbstractSVRLMessage> List<JAXBElement<TestAssertionReportType> > traverseSVRLMessages(List<T> svrlMessages){
        var reports = new ArrayList<JAXBElement<TestAssertionReportType>>();

        for (T message : svrlMessages) {
            var error = new BAR();
            error.setDescription(message.getText());
            LocationInfo locationInfo = getLocationInfo(message.getLocation());
            if (showPaths) {
                error.setLocation("%s:%s:0|%s".formatted(XML_ITEM_NAME, locationInfo.lineNumber(), locationInfo.path()));
            } else {
                error.setLocation("%s:%s:0".formatted(XML_ITEM_NAME, locationInfo.lineNumber()));
            }
            if (showTests) {
                error.setTest(message.getTest());
            }
            JAXBElement<TestAssertionReportType> element;
            int level = message.getFlag().getNumericLevel();
            if (level == EErrorLevel.SUCCESS.getNumericLevel()) {
                element = this.objectFactory.createTestAssertionGroupReportsTypeInfo(error);
            } else if (level == EErrorLevel.INFO.getNumericLevel()) {
                element = this.objectFactory.createTestAssertionGroupReportsTypeInfo(error);
            } else if (level == EErrorLevel.WARN.getNumericLevel()) {
                element = this.objectFactory.createTestAssertionGroupReportsTypeWarning(error);
            } else { // ERROR, FATAL_ERROR
                element = this.objectFactory.createTestAssertionGroupReportsTypeError(error);
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

    private LocationInfo getLocationInfo(String xpathExpression) {
        String xpathExpressionConverted = convertToXPathExpression(xpathExpression);
        XPath xPath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
        xPath.setNamespaceContext(getNamespaceContext());
        Node node;
        String lineNumber;
        try {
            node = (Node) xPath.evaluate(xpathExpressionConverted, this.node, XPathConstants.NODE);
            lineNumber = (String) node.getUserData("lineNumber");
        } catch (XPathExpressionException e) {
            logger.debug(e.getMessage());
            lineNumber = "0";
        }
        return new LocationInfo(toPathForPresentation(xpathExpression), lineNumber);
    }

    /**
     * Concert the provided XPath expression to one to be used for reporting.
     *
     * @param xpathExpression The XPath expression to process.
     * @return The location path to use.
     */
    private String toPathForPresentation(String xpathExpression) {
        if (xpathExpression != null) {
            return xpathExpression
                    .replaceAll("\\*:", "")
                    .replaceAll("\\[\\s*namespace-uri\\(\\)\\s*=\\s*(?:'[^\\[\\]]+'|\"[^\\[\\]]+\")\\s*]", "");
        } else {
            return null;
        }
    }

    private String convertToXPathExpression(String xpathExpression) {
        if (convertXPathExpressions) {
            try {
                if (documentHasDefaultNamespace(node)) {
                    StringBuilder s = new StringBuilder(xpathExpression);
                    Matcher m = DEFAULTNS_PATTERN.matcher(s.toString());
                    s.delete(0, s.length());
                    while (m.find()) {
                        String match = m.group(0);
                        if (match.indexOf(':') == -1) {
                            match = "/"+DocumentNamespaceContext.DEFAULT_NS+":"+match.substring(1);
                        }
                        m.appendReplacement(s, match);
                    }
                    m.appendTail(s);
                    xpathExpression = s.toString();
                }
            } catch (Exception e) {
                logger.warn("Failed to convert XPath expression.", e);
            }
        }
        return xpathExpression;
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
        return hasDefaultNamespace;
    }

    private record LocationInfo(String path, String lineNumber) {}
}
