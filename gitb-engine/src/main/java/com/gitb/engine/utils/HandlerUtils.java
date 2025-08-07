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
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.types.DataType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.TestSessionNamespaceContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class HandlerUtils {

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

}
