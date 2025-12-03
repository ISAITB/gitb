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

package com.gitb.engine.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.gitb.core.AnyContent;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.*;
import com.gitb.types.*;
import com.gitb.utils.TestSessionNamespaceContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

import static com.gitb.engine.utils.TestCaseUtils.getInputFor;

public class HandlerUtils {

    private static final Logger LOG = LoggerFactory.getLogger(HandlerUtils.class);
    public static final String NAMESPACE_MAP_INPUT = "_com.gitb.Namespaces";
    public static final String SESSION_INPUT = "_com.gitb.Session";

    private static final ObjectReader YAML_READER;
    private static final ObjectWriter YAML_WRITER;
    private static final ObjectReader JSON_READER;
    private static final ObjectWriter JSON_WRITER;

    static {
        // Construct immutable (thread-safe) readers and writers for JSON and YAML.
        var jsonMapper = new ObjectMapper();
        JSON_READER = jsonMapper.reader();
        JSON_WRITER = jsonMapper.writerWithDefaultPrettyPrinter();
        var yamlMapper = new YAMLMapper();
        YAML_READER = yamlMapper.reader();
        YAML_WRITER = yamlMapper.writerWithDefaultPrettyPrinter();
    }

    public static XPathExpression compileXPathExpression(MapType namespaces, StringType expression, VariableResolver variableResolver) {
        // Compile xpath expression
        XPath xPath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
        if (namespaces != null) {
            var nsMap = new HashMap<String, String>();
            for (var entry: ((Map<String, DataType>)namespaces.getValue()).entrySet()) {
                nsMap.put(entry.getKey(), (String) entry.getValue().getValue());
            }
            xPath.setNamespaceContext(new TestSessionNamespaceContext(nsMap));
            xPath.setXPathVariableResolver(variableResolver);
        }
        try {
            return xPath.compile(expression.getValue());
        } catch (XPathExpressionException e) {
            throw new GITBEngineInternalError(e);
        }
    }

    public static String prettyPrintJson(String input) {
        try (StringReader in = new StringReader(input)) {
            JsonElement json = com.google.gson.JsonParser.parseReader(in);
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .serializeNulls()
                    .create();
            return gson.toJson(json);
        } catch (JsonSyntaxException e) {
            throw new IllegalStateException("Unable to parse provided input as a JSON document", e);
        }
    }

    public static JsonNode readAsJson(String jsonContent) {
        try {
            return JSON_READER.readTree(jsonContent);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unexpected error while parsing JSON", e);
        }
    }

    public static JsonNode readAsYaml(String yamlContent) {
        try {
            return YAML_READER.readTree(yamlContent);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unexpected error while parsing YAML", e);
        }
    }

    public static String writeAsJson(JsonNode node) {
        var out = new StringWriter();
        try {
            JSON_WRITER.writeValue(out, node);
            return out.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected error while writing JSON", e);
        }
    }

    public static String writeAsYaml(JsonNode node) {
        var out = new StringWriter();
        try {
            YAML_WRITER.writeValue(out, node);
            return out.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected error while writing YAML", e);
        }
    }

    public static String convertYamlToJson(String yamlContent) {
        var out = new StringWriter();
        try {
            JSON_WRITER.writeValue(out, readAsYaml(yamlContent));
            return out.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected error while converting YAML to JSON", e);
        }
    }

    public static String convertJsonToYaml(String jsonContent) {
        var out = new StringWriter();
        try {
            YAML_WRITER.writeValue(out, readAsJson(jsonContent));
            return out.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected error while converting JSON to YAML", e);
        }
    }

    public static Long getHandlerTimeout(String timeout, VariableResolver resolver) {
        Long handlerTimeout = null;
        if (timeout != null) {
            if (VariableResolver.isVariableReference(timeout)) {
                handlerTimeout = resolver.resolveVariableAsNumber(timeout).longValue();
            } else {
                handlerTimeout = Double.valueOf(timeout).longValue();
            }
        }
        return handlerTimeout;
    }

    public static void recordHandlerTimeout(String timeoutFlag, TestCaseScope scope, boolean timeoutOccurred) {
        recordHandlerTimeout(timeoutFlag, new VariableResolver(scope), scope, timeoutOccurred);
    }

    public static void recordHandlerTimeout(String timeoutFlag, VariableResolver resolver, TestCaseScope scope, boolean timeoutOccurred) {
        if (timeoutFlag != null) {
            if (VariableResolver.isVariableReference(timeoutFlag)) {
                timeoutFlag = resolver.resolveVariableAsString(timeoutFlag).getValue();
            }
            scope.createVariable(timeoutFlag).setValue(new BooleanType(timeoutOccurred));
        }
    }

    public static void addReportStepMapToReport(DataType reportSteps, TAR report, boolean setResult, TestCaseScope scope, String session) {
        if (report != null && reportSteps != null) {
            // Converting to a list allows us to accept a simple string as well as a list of strings.
            ListType reportStepList = (ListType) reportSteps.convertTo(DataType.LIST_DATA_TYPE);
            List<JAXBElement<TestAssertionReportType>> errorList = new ArrayList<>();
            List<JAXBElement<TestAssertionReportType>> warningList = new ArrayList<>();
            List<JAXBElement<TestAssertionReportType>> infoList = new ArrayList<>();
            reportStepList.getElements().forEach(stepIdType -> {
                String stepIdValue = (String) stepIdType.convertTo(DataType.STRING_DATA_TYPE).getValue();
                Arrays.stream(StringUtils.split(stepIdValue, ',')).map(String::trim).forEach((stepId) -> {
                    // Each step ID value is also considered as potentially comma-separated.
                    TAR referencedReport = TestCaseUtils.retrieveStoredStepReport(stepId, scope);
                    if (referencedReport != null) {
                        if (referencedReport.getContext() != null) {
                            // Merge the context data.
                            if (report.getContext() == null) {
                                report.setContext(referencedReport.getContext());
                            } else {
                                if (referencedReport.getContext().getItem() != null) {
                                    for (AnyContent item: referencedReport.getContext().getItem()) {
                                        if (item.getName() != null) {
                                            List<AnyContent> matchedInputs = getInputFor(report.getContext().getItem(), item.getName());
                                            if (matchedInputs.isEmpty()) {
                                                report.getContext().getItem().add(item);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (referencedReport.getReports() != null) {
                            // Process the report items.
                            referencedReport.getReports().getInfoOrWarningOrError().forEach(item -> {
                                if (item.getName().getLocalPart().equals("error")) {
                                    errorList.add(item);
                                } else if (item.getName().getLocalPart().equals("warning")) {
                                    warningList.add(item);
                                } else {
                                    infoList.add(item);
                                }
                            });
                        }
                    } else {
                        LOG.warn(MarkerFactory.getDetachedMarker(session), "No validation report found for step ID [{}]", stepId);
                    }
                });
            });
            int errorCount = errorList.size();
            int warningCount = warningList.size();
            int infoCount = infoList.size();
            // Merge report items.
            if (errorCount > 0 || warningCount > 0 || infoCount > 0) {
                if (report.getReports() == null) {
                    report.setReports(new TestAssertionGroupReportsType());
                }
                report.getReports().getInfoOrWarningOrError().addAll(errorList);
                report.getReports().getInfoOrWarningOrError().addAll(warningList);
                report.getReports().getInfoOrWarningOrError().addAll(infoList);
            }
            // Adapt counters.
            setReportCounters(report, errorCount, warningCount, infoCount, setResult);
        }
    }

    public static void addReportItemMapToReport(DataType reportItems, TAR report, boolean setResult, Optional<ObjectFactory> objectFactory) {
        if (report != null && reportItems instanceof MapType reportItemMap) {
            DataType informationItemData = reportItemMap.getItem("info");
            DataType warningItemData = reportItemMap.getItem("warning");
            DataType errorItemData = reportItemMap.getItem("error");
            ObjectFactory factoryToUse = objectFactory.orElseGet(ObjectFactory::new);
            if (informationItemData != null || warningItemData != null || errorItemData != null) {
                if (report.getReports() == null) {
                    report.setReports(new TestAssertionGroupReportsType());
                }
            }
            int errorCount = addReportItemsToReport(errorItemData, report, factoryToUse::createTestAssertionGroupReportsTypeError);
            int warningCount = addReportItemsToReport(warningItemData, report, factoryToUse::createTestAssertionGroupReportsTypeWarning);
            int infoCount = addReportItemsToReport(informationItemData, report, factoryToUse::createTestAssertionGroupReportsTypeInfo);
            setReportCounters(report, errorCount, warningCount, infoCount, setResult);
        }
    }

    private static void setReportCounters(TAR report, int errorCount, int warningCount, int infoCount, boolean setResult) {
        ValidationCounters counters = report.getCounters();
        if (report.getCounters() == null) {
            counters = new ValidationCounters();
            report.setCounters(counters);
        }
        int totalErrorCount = errorCount + ((counters.getNrOfErrors() == null)?0:counters.getNrOfErrors().intValue());
        int totalWarningCount = warningCount + ((counters.getNrOfWarnings() == null)?0:counters.getNrOfWarnings().intValue());
        int totalInfoCount = infoCount + ((counters.getNrOfAssertions() == null)?0:counters.getNrOfAssertions().intValue());
        counters.setNrOfErrors(BigInteger.valueOf(totalErrorCount));
        counters.setNrOfWarnings(BigInteger.valueOf(totalWarningCount));
        counters.setNrOfAssertions(BigInteger.valueOf(totalInfoCount));
        if (setResult) {
            // The result is fully based on the items in the report.
            if (totalErrorCount > 0) {
                report.setResult(TestResultType.FAILURE);
            } else if (totalWarningCount > 0) {
                report.setResult(TestResultType.WARNING);
            } else {
                report.setResult(TestResultType.SUCCESS);
            }
        } else {
            // The result is determined regardless of the report items, but we check the items and override the result to ensure consistency.
            if (totalErrorCount > 0 && report.getResult() != TestResultType.FAILURE) {
                report.setResult(TestResultType.FAILURE);
            } else if (totalWarningCount > 0 && report.getResult() != TestResultType.FAILURE && report.getResult() != TestResultType.WARNING) {
                report.setResult(TestResultType.WARNING);
            } else if (report.getResult() == null) {
                report.setResult(TestResultType.SUCCESS);
            }
        }
    }

    private static int addReportItemsToReport(DataType itemList, TAR targetReport, Function<BAR, JAXBElement<TestAssertionReportType>> wrapper) {
        int itemCount = 0;
        if (itemList != null) {
            ListType items = (ListType) itemList.convertTo(DataType.LIST_DATA_TYPE);
            items.getElements().forEach(item -> targetReport.getReports().getInfoOrWarningOrError().add(reportItemFromDataType(item, wrapper)));
            itemCount = items.getElements().size();
        }
        return itemCount;
    }

    private static JAXBElement<TestAssertionReportType> reportItemFromDataType(DataType itemData, Function<BAR, JAXBElement<TestAssertionReportType>> wrapper) {
        var item = new BAR();
        if (itemData instanceof MapType mapData) {
            item.setDescription(mapData.getItemOptional("description").map(v -> (String) v.convertTo(DataType.STRING_DATA_TYPE).getValue()).orElse(null));
            item.setLocation(mapData.getItemOptional("location").map(v -> (String) v.convertTo(DataType.STRING_DATA_TYPE).getValue()).orElse(null));
            item.setAssertionID(mapData.getItemOptional("assertionId").map(v -> (String) v.convertTo(DataType.STRING_DATA_TYPE).getValue()).orElse(null));
            item.setTest(mapData.getItemOptional("test").map(v -> (String) v.convertTo(DataType.STRING_DATA_TYPE).getValue()).orElse(null));
            item.setType(mapData.getItemOptional("type").map(v -> (String) v.convertTo(DataType.STRING_DATA_TYPE).getValue()).orElse(null));
            item.setValue(mapData.getItemOptional("value").map(v -> (String) v.convertTo(DataType.STRING_DATA_TYPE).getValue()).orElse(null));
        } else if (itemData != null) {
            item.setDescription((String) itemData.convertTo(DataType.STRING_DATA_TYPE).getValue());
        } else {
            throw new IllegalArgumentException("Report item data was found to be null");
        }
        return wrapper.apply(item);
    }

}
