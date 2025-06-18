/*
 * Copyright (C) 2025 European Union
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

package com.gitb.engine.validation.handlers.xmlunit;

import com.gitb.core.*;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.AbstractValidator;
import com.gitb.engine.validation.handlers.xml.DocumentNamespaceContext;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.*;
import com.gitb.types.*;
import com.gitb.utils.XMLDateTimeUtils;
import com.gitb.utils.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.DifferenceEvaluators;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gitb.engine.utils.HandlerUtils.NAMESPACE_MAP_INPUT;

@ValidationHandler(name="XmlMatchValidator")
public class XmlMatchValidator extends AbstractValidator {

    private static final Logger logger = LoggerFactory.getLogger(XmlMatchValidator.class);
    private static final String INPUT__XML = "xml";
    private static final String INPUT__TEMPLATE = "template";
    private static final String INPUT__IGNORED_PATHS = "ignoredPaths";

    @Override
    public ValidationModule getModuleDefinition() {
        ValidationModule module = new ValidationModule();
        module.setId("XmlMatchValidator");
        module.setOperation("V");
        module.setMetadata(new Metadata());
        module.getMetadata().setName("XmlMatchValidator");
        module.getMetadata().setVersion("1.0");
        module.setInputs(new TypedParameters());
        module.getInputs().getParam().add(createTypedParameter(INPUT__XML, "The XML document instance to validate", ConfigurationType.SIMPLE, UsageEnumeration.R, "object"));
        module.getInputs().getParam().add(createTypedParameter(INPUT__TEMPLATE, "The XML document template to consider for the match", ConfigurationType.SIMPLE, UsageEnumeration.R, "object"));
        module.getInputs().getParam().add(createTypedParameter(INPUT__IGNORED_PATHS, "A list of paths to ignore", ConfigurationType.SIMPLE, UsageEnumeration.O, "list[string]"));
        return module;
    }

    private TypedParameter createTypedParameter(String name, String desc, ConfigurationType kind, UsageEnumeration usage, String dataType) {
        TypedParameter param = new TypedParameter();
        param.setName(name);
        param.setUse(usage);
        param.setDesc(desc);
        param.setKind(kind);
        param.setType(dataType);
        return param;
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        if (!inputs.containsKey(INPUT__XML)) {
            throw new IllegalArgumentException("The ["+INPUT__XML+"] input is required.");
        }
        if (!inputs.containsKey(INPUT__TEMPLATE)) {
            throw new IllegalArgumentException("The ["+INPUT__TEMPLATE+"] input is required.");
        }
        ObjectType xml = getAsObjectType(inputs.get(INPUT__XML));
        ObjectType template = getAsObjectType(inputs.get(INPUT__TEMPLATE));
        MapType namespaces = (MapType) inputs.get(NAMESPACE_MAP_INPUT);

        List<String> pathsToIgnore = new ArrayList<>();
        if (inputs.containsKey(INPUT__IGNORED_PATHS)) {
            ListType listType;
            DataType type = inputs.get(INPUT__IGNORED_PATHS);
            if (type instanceof ListType) {
                listType = (ListType)type;
            } else {
                listType = (ListType)type.convertTo(DataType.LIST_DATA_TYPE);
            }
            for (Object path: ((List)listType.getValue())) {
                if (path instanceof StringType) {
                    pathsToIgnore.add((String)((StringType)path).getValue());
                } else {
                    throw new IllegalArgumentException("The items of input ["+INPUT__IGNORED_PATHS+"] must be of type string.");
                }
            }
        }

        Document document;
        try {
            document = XMLUtils.readXMLWithLineNumbers(new ByteArrayInputStream(xml.serializeByDefaultEncoding()));
        } catch (IOException|SAXException e) {
            throw new IllegalStateException(e);
        }
        var documentContext = new DocumentNamespaceContext(document, false);
        var documentNamespaces = documentContext.getPrefixToUriMap();
        Map<String, String> namespacesToUse;
        NamespaceContext contextToUse;
        if (namespaces != null) {
            namespacesToUse = new HashMap<>();
            for (var entry: ((Map<String, DataType>)namespaces.getValue()).entrySet()) {
                namespacesToUse.put(entry.getKey(), (String) entry.getValue().getValue());
            }
            for (var documentPrefix: documentNamespaces.entrySet()) {
                namespacesToUse.putIfAbsent(documentPrefix.getKey(), documentPrefix.getValue());
            }
            contextToUse = new com.gitb.utils.NamespaceContext(namespacesToUse);
        } else {
            contextToUse = documentContext;
            namespacesToUse = documentNamespaces;
        }
        DifferenceEvaluator chain = DifferenceEvaluators.chain(
                DifferenceEvaluators.Default,
                getDifferenceEvaluator(pathsToIgnore)
        );
        Diff diff = DiffBuilder
                .compare(template.getValue())
                .withTest(xml.getValue())
                .ignoreComments()
                .ignoreWhitespace()
                .normalizeWhitespace()
                .checkForSimilar()
                .withNamespaceContext(namespacesToUse)
                .withDifferenceEvaluator(chain)
                .build();

        return diffToTAR(diff, contextToUse, document, template);
    }

    private CustomDifferenceEvaluator getDifferenceEvaluator(List<String> xpathsToIgnore) {
        return new CustomDifferenceEvaluator(xpathsToIgnore);
    }

    private ObjectType getAsObjectType(DataType inputType) {
        ObjectType obj;
        if (inputType instanceof ObjectType) {
            obj = (ObjectType)inputType;
        } else {
            obj = (ObjectType)inputType.convertTo(DataType.OBJECT_DATA_TYPE);
        }
        return obj;
    }

    private TAR diffToTAR(Diff diff, NamespaceContext nsContext, Document xml, ObjectType template) {
        ObjectFactory objectFactory = new ObjectFactory();
        TAR report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        try {
            report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Exception while creating XMLGregorianCalendar", e);
        }
        if (diff.hasDifferences()) {
            XPath xPath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
            xPath.setNamespaceContext(nsContext);
            int counter = 0;
            for (Difference difference: diff.getDifferences()) {
                // Extract description
                String description = difference.getComparison().toString();
                // Extract location for error
                String lineNumber = getLineNumberFromXPath(xPath, difference.getComparison().getTestDetails().getXPath(), xml);
                if (lineNumber == null) {
                    lineNumber = getLineNumberFromXPath(xPath, difference.getComparison().getTestDetails().getParentXPath(), xml);
                    if (lineNumber == null) {
                        logger.warn("Could not extract line information for XPath {} or {}", difference.getComparison().getTestDetails().getXPath(), difference.getComparison().getTestDetails().getParentXPath());
                        lineNumber = "0";
                    }
                }
                String location = "xml:" + lineNumber + ":0";
                addError(objectFactory, report, description, null, location);
                counter += 1;
            }
            report.setCounters(new ValidationCounters());
            report.getCounters().setNrOfAssertions(BigInteger.ZERO);
            report.getCounters().setNrOfWarnings(BigInteger.ZERO);
            report.getCounters().setNrOfErrors(BigInteger.valueOf(counter));
            report.setResult(TestResultType.FAILURE);
        }
        AnyContent ctxContent = new AnyContent();
        ctxContent.setType("map");
        AnyContent xmlContent = new AnyContent();
        xmlContent.setName("xml");
        xmlContent.setType("string");
        xmlContent.setMimeType(MediaType.APPLICATION_XML_VALUE);
        xmlContent.setValue(new String(new ObjectType(xml).serializeByDefaultEncoding()));
        xmlContent.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        AnyContent templateContent = new AnyContent();
        templateContent.setName("template");
        templateContent.setType("string");
        templateContent.setValue(new String(template.serializeByDefaultEncoding()));
        templateContent.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        report.setContext(ctxContent);
        report.getContext().getItem().add(xmlContent);
        report.getContext().getItem().add(templateContent);
        return report;
    }

    private String getLineNumberFromXPath(XPath xPath, String xpathExpression, Document document) {
        if (xpathExpression != null) {
            Node node;
            try {
                node = (Node) xPath.evaluate(xpathExpression, document, XPathConstants.NODE);
                if (node != null) {
                    return (String) node.getUserData(XMLUtils.LINE_NUMBER_KEY_NAME);
                }
            } catch (XPathExpressionException e) {
                logger.debug(e.getMessage());
            }
        }
        return null;
    }

    private static void addError(ObjectFactory objectFactory, TAR report, String description, String test, String location) {
        BAR error = new BAR();
        error.setDescription(description);
        error.setTest(test);
        error.setLocation(location);
        if (report.getReports() == null) {
            report.setReports(new TestAssertionGroupReportsType());
        }
        report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeError(error));
        report.setResult(TestResultType.FAILURE);
    }

}
