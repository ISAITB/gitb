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

package com.gitb.engine.validation.handlers.rdf;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.system.PrefixLib;
import org.topbraid.jenax.util.RDFLabels;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Label function used by the SHACL engine to render nodes within the messages of validation results.
 * <p>
 * The engine's default behaviour abbreviates a URI only if its local part is a valid XML name (a limitation of
 * Jena's {@code qnameFor}), and does so using the prefixes of the node's own model. This leads to inconsistent
 * messages, with some URIs prefixed and others (e.g. those with a local name starting with a digit) given in full.
 * <p>
 * This implementation consistently abbreviates URIs using the prefixes of the validated data model (which are
 * also the ones declared in the resulting report), as long as the local part is valid under Turtle rules. Otherwise
 * the URI is rendered in full within angle brackets. All other nodes are rendered as per the engine's default.
 */
public class ResultMessageLabelFunction implements Function<RDFNode, String> {

    private final Map<String, String> prefixes;

    /**
     * Constructor.
     *
     * @param dataModel The model whose prefixes are to be used for abbreviating URIs.
     */
    public ResultMessageLabelFunction(Model dataModel) {
        this.prefixes = new HashMap<>(dataModel.getNsPrefixMap());
    }

    @Override
    public String apply(RDFNode node) {
        if (node.isURIResource()) {
            return label(node.asResource().getURI());
        }
        return RDFLabels.get().getNodeLabel(node);
    }

    private String label(String uri) {
        String bestPrefix = null;
        String bestLocal = null;
        int bestNamespaceLength = -1;
        for (var entry : prefixes.entrySet()) {
            String namespace = entry.getValue();
            if (namespace.length() > bestNamespaceLength && uri.startsWith(namespace)) {
                String local = uri.substring(namespace.length());
                if (PrefixLib.isSafeLocalPart(local)) {
                    bestPrefix = entry.getKey();
                    bestLocal = local;
                    bestNamespaceLength = namespace.length();
                }
            }
        }
        if (bestPrefix == null) {
            return "<" + uri + ">";
        }
        return bestPrefix + ":" + bestLocal;
    }

}
