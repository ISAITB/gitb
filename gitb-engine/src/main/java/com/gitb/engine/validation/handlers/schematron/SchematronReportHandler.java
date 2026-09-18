/*
 * Copyright (C) 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.engine.validation.handlers.schematron;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.validation.handlers.common.AbstractReportHandler;
import com.gitb.engine.validation.handlers.xml.DocumentNamespaceContext;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.*;
import com.gitb.types.DataType;
import com.gitb.types.SchemaType;
import com.gitb.utils.XMLUtils;
import com.helger.diagnostics.error.level.EErrorLevel;
import com.helger.schematron.svrl.AbstractSVRLMessage;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by senan on 31.10.2014.
 */
public class SchematronReportHandler extends AbstractReportHandler {

    private static final Logger logger = LoggerFactory.getLogger(SchematronReportHandler.class);

    public static final String XML_ITEM_NAME  = "xml";
    public static final String SCH_ITEM_NAME  = "sch";
    private static final Pattern DEFAULTNS_PATTERN = Pattern.compile("\\/[\\w]+:?");

    private final Supplier<Document> documentSupplier;
    private Document document;
    private XPathFactory xpathFactory;
    private XPath xPath;
    private SchematronLocationResolver locationResolver;
    private final SchematronOutputType svrlReport;
    private NamespaceContext namespaceContext;
    private final boolean convertXPathExpressions;
    private final boolean showTests;
    private final boolean showPaths;
    private Boolean hasDefaultNamespace;

    /**
     * Constructor.
     *
     * @param xmlContent The input XML content, already serialised (by the caller's shared
     *   {@code XmlInputProvider}) rather than serialised again here.
     * @param sch The Schematron schema to attach to the report, or {@code null} if it should not be shown.
     * @param documentSupplier Supplier for the line-numbered document, resolved lazily and at most once, since it is
     *   only needed to localise report item locations.
     * @param svrl The SVRL validation output.
     * @param convertXPathExpressions Whether locations come from the pure Schematron engine (and so need path
     *   conversion) rather than the XSLT-based one.
     * @param showTests Whether to include the failed assertion's test expression in the report.
     * @param showPaths Whether to include the resolved location path in the report.
     */
    protected SchematronReportHandler(String xmlContent, SchemaType sch, Supplier<Document> documentSupplier, SchematronOutputType svrl, boolean convertXPathExpressions, boolean showTests, boolean showPaths) {
        super();
        this.documentSupplier = documentSupplier;
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
	    xmlAttachment.setValue(xmlContent);
	    attachment.getItem().add(xmlAttachment);

        if (sch != null) {
            AnyContent schemaAttachment = new AnyContent();
            schemaAttachment.setName(SCH_ITEM_NAME);
            schemaAttachment.setType(DataType.SCHEMA_DATA_TYPE);
            schemaAttachment.setMimeType(MediaType.APPLICATION_XML_VALUE);
            schemaAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            schemaAttachment.setValue(new String(sch.serializeByDefaultEncoding(), StandardCharsets.UTF_8));
            attachment.getItem().add(schemaAttachment);
        }

	    report.setContext(attachment);
    }

    /**
     * Get or resolve (parse) the line-numbered input document. Resolved lazily and only once, since parsing is
     * only needed to localise report item locations.
     *
     * @return The document.
     */
    private Document getDocument() {
        if (document == null) {
            document = documentSupplier.get();
        }
        return document;
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
            namespaceContext = new DocumentNamespaceContext(getDocument(), false);
        }
        return namespaceContext;
    }

    /**
     * Get or initialise the resolver used to quickly locate the line number for a report item's location, without
     * needing a full XPath evaluation for the (very common) case of a canonical, XSLT-generated location path.
     *
     * @return The resolver.
     */
    private SchematronLocationResolver getLocationResolver() {
        if (locationResolver == null) {
            locationResolver = new SchematronLocationResolver(getDocument());
        }
        return locationResolver;
    }

    /**
     * Construct the specific XPath factory to use (force it to be a Saxon implementation).
     *
     * @return The factory.
     */
    private XPathFactory getXPathFactory() {
        if (xpathFactory == null) {
            xpathFactory = new net.sf.saxon.xpath.XPathFactoryImpl();
        }
        return xpathFactory;
    }

    /**
     * Get or initialise the (single, reused) XPath instance used to resolve report item locations.
     *
     * @return The XPath instance.
     */
    private XPath getXPath() {
        if (xPath == null) {
            xPath = getXPathFactory().newXPath();
            xPath.setNamespaceContext(getNamespaceContext());
        }
        return xPath;
    }

    /**
     * Adapt the provided XPath expression to change its path elements that don't have a prefix, to use a wildcard prefix.
     * <p/>
     * Splitting naively on {@code '/'} is not fully safe for canonical, XSLT-generated locations - a namespace URI
     * predicate can itself contain slashes - but those never reach this method in practice: they are always
     * prefixed (see {@link SchematronLocationResolver}), so no path part is ever missing a {@code ':'} and
     * {@code changed} stays {@code false}, returning empty. A genuinely malformed split simply fails to evaluate,
     * degrading to line {@code "0"} - the same outcome as before this method existed.
     *
     * @param xpathExpression The XPath expression to process.
     * @return The adapted XPath expression or empty if no change was made to the original expression.
     */
    private Optional<String> convertToWildCardXPathExpression(String xpathExpression) {
        boolean changed = false;
        String expressionToReturn = xpathExpression;
        if (xpathExpression != null) {
            String[] pathParts = StringUtils.split(xpathExpression, '/');
            var builder = new StringBuilder();
            for (var pathPart: pathParts) {
                if (!builder.isEmpty()) {
                    builder.append('/');
                }
                if (pathPart.indexOf(':') == -1) {
                    changed = true;
                    builder.append("*:");
                }
                builder.append(pathPart);
            }
            expressionToReturn = builder.toString();
        }
        if (changed) {
            return Optional.of(expressionToReturn);
        } else {
            return Optional.empty();
        }
    }

    private LocationInfo getLocationInfo(String xpathExpression) {
        String xpathExpressionConverted = convertToXPathExpression(xpathExpression);
        String lineNumber;
        try {
            Node locatedNode = null;
            if (!convertXPathExpressions) {
                /*
                 * The fast resolver understands the canonical, XSLT-generated location syntax produced for
                 * XSLT-based Schematron ("*:local[namespace-uri()='uri'][n]"). It is not attempted for the "pure"
                 * Schematron path (convertXPathExpressions=true), whose locations use a different XPath dialect
                 * that the resolver does not parse - it would simply never match and fall through below
                 * regardless, but skipping the attempt avoids the wasted parsing effort.
                 */
                locatedNode = getLocationResolver().resolve(xpathExpressionConverted).orElse(null);
            }
            if (locatedNode == null) {
                XPath xPath = getXPath();
                locatedNode = (Node) xPath.evaluate(xpathExpressionConverted, getDocument(), XPathConstants.NODE);
                if (locatedNode == null) {
                    var expressionWithWildcards = convertToWildCardXPathExpression(xpathExpression);
                    if (expressionWithWildcards.isPresent()) {
                        locatedNode = (Node) xPath.evaluate(expressionWithWildcards.get(), getDocument(), XPathConstants.NODE);
                    }
                }
            }
            lineNumber = resolveLineNumber(locatedNode);
        } catch (GITBEngineInternalError e) {
            // A genuine failure to resolve the input document itself (see getDocument()) - not a location
            // resolution problem, so this must not be silently swallowed into a "0" line number.
            throw e;
        } catch (Exception e) {
            // Either the fast resolver or the XPath evaluation failed to make sense of this specific location
            // expression - the pre-existing behaviour for such cases, i.e. the finding is still reported, just
            // without a specific line number.
            logger.debug(e.getMessage());
            lineNumber = "0";
        }
        return new LocationInfo(toPathForPresentation(xpathExpression), lineNumber);
    }

    /**
     * Resolve the line number to report for the given located node.
     * <p/>
     * The line-numbered document ({@code XMLUtils#readXMLWithLineNumbers}) only records the line number user data
     * on elements, not attributes - so a location that resolves to an attribute (e.g. {@code @id}) falls back to
     * its owning element's line, rather than reporting a literal {@code "null"} line number.
     *
     * @param locatedNode The located node, or {@code null} if none was found.
     * @return The line number to report, or {@code "0"} if none could be determined.
     */
    private String resolveLineNumber(Node locatedNode) {
        if (locatedNode == null) {
            return "0";
        }
        Node lineNumberSource = locatedNode.getNodeType() == Node.ATTRIBUTE_NODE
                ? ((org.w3c.dom.Attr) locatedNode).getOwnerElement()
                : locatedNode;
        String lineNumber = lineNumberSource == null ? null : (String) lineNumberSource.getUserData(XMLUtils.LINE_NUMBER_KEY_NAME);
        return lineNumber != null ? lineNumber : "0";
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
                if (documentHasDefaultNamespace(getDocument())) {
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
