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
