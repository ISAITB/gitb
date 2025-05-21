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
        return map.getOrDefault(alias, alias);
    }

    private String resolveHandlerInput(Map<String, Map<String, String>> map, String handler, String alias) {
        if (map.containsKey(handler)) {
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
