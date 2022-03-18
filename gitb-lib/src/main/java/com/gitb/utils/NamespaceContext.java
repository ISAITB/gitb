package com.gitb.utils;

import java.util.*;

public class NamespaceContext implements javax.xml.namespace.NamespaceContext{

    //<prefix, namespaceURI>
    private final Map<String, String> namespaceURIs;

    //<namespaceURI, prefix>
    private final Map<String, String> prefixes;

    public NamespaceContext(Map<String, String> namespaceDefinitions){
        namespaceURIs = new HashMap<>();
        prefixes = new HashMap<>();
        if (namespaceDefinitions != null) {
        for (var nsEntry: namespaceDefinitions.entrySet()) {
                namespaceURIs.put(nsEntry.getKey(), nsEntry.getValue());
                prefixes.put(nsEntry.getValue(), nsEntry.getKey());
            }
        }
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return namespaceURIs.get(prefix);
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return prefixes.get(namespaceURI);
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        List<String> result = new ArrayList<>();
        if (namespaceURIs.containsValue(namespaceURI)) {
            for (String key: namespaceURIs.keySet()) {
                if (namespaceURIs.get(key).equals(namespaceURI)) {
                    result.add(key);
                }
            }
        }
        return result.iterator();
    }
}
