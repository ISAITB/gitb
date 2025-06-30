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

package com.gitb.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AliasManager {

    private final Map<String, String> processingHandlerAliases;
    private final Map<String, Map<String, String>> processingHandlerInputs;
    private final Map<String, String> validationHandlerAliases;
    private final Map<String, Map<String, String>> validationHandlerInputs;

    static {
        _instance = new AliasManager();
    }

    private static final AliasManager _instance;

    private AliasManager() {
        // Create maps.
        processingHandlerAliases = new ConcurrentHashMap<>();
        processingHandlerInputs = new ConcurrentHashMap<>();
        validationHandlerAliases = new ConcurrentHashMap<>();
        validationHandlerInputs = new ConcurrentHashMap<>();
        // Processing handler aliases.
        processingHandlerAliases.put("JSONPointerProcessor", "JsonPointerProcessor");
        processingHandlerAliases.put("XSLTProcessor", "XsltProcessor");
        // Validation handler aliases.
        validationHandlerInputs.put("NumberValidator", new ConcurrentHashMap<>());
        validationHandlerInputs.get("NumberValidator").put("actualnumber", "actual");
        validationHandlerInputs.get("NumberValidator").put("expectednumber", "expected");
        validationHandlerInputs.put("StringValidator", new ConcurrentHashMap<>());
        validationHandlerInputs.get("StringValidator").put("actualstring", "actual");
        validationHandlerInputs.get("StringValidator").put("expectedstring", "expected");
        validationHandlerInputs.put("XPathValidator", new ConcurrentHashMap<>());
        validationHandlerInputs.get("XPathValidator").put("xmldocument", "xml");
        validationHandlerInputs.get("XPathValidator").put("xpathexpression", "expression");
        validationHandlerAliases.put("XSDValidator", "XsdValidator");
        validationHandlerInputs.put("XsdValidator", new ConcurrentHashMap<>());
        validationHandlerInputs.get("XsdValidator").put("xmldocument", "xml");
        validationHandlerInputs.get("XsdValidator").put("xsddocument", "xsd");
        validationHandlerInputs.put("SchematronValidator", new ConcurrentHashMap<>());
        validationHandlerInputs.get("SchematronValidator").put("xmldocument", "xml");
    }

    public static AliasManager getInstance() {
        return _instance;
    }

    private String resolveHandler(Map<String, String> map, String alias) {
        if (alias != null) {
            return map.getOrDefault(alias, alias);
        } else {
            return null;
        }
    }

    private String resolveHandlerInput(Map<String, Map<String, String>> map, String handler, String alias) {
        if (alias != null && map != null && handler != null && map.containsKey(handler)) {
            return map.get(handler).getOrDefault(alias, alias);
        } else {
            return alias;
        }
    }

    public String resolveProcessingHandler(String alias) {
        return resolveHandler(processingHandlerAliases, alias);
    }

    public String resolveValidationHandler(String alias) {
        return resolveHandler(validationHandlerAliases, alias);
    }

    public String resolveProcessingHandlerInput(String handler, String alias) {
        return resolveHandlerInput(processingHandlerInputs, handler, alias);
    }

    public String resolveValidationHandlerInput(String handler, String alias) {
        return resolveHandlerInput(validationHandlerInputs, handler, alias);
    }

}
