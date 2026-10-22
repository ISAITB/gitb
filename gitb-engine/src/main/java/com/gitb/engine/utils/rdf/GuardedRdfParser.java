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

package com.gitb.engine.utils.rdf;

import com.gitb.engine.TestEngineConfiguration;
import com.gitb.utils.UriAuthorizer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.lang.LangJSONLD11;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapStd;
import org.apache.jena.sparql.util.Context;

import java.util.Map;

/**
 * Parser of RDF content that may refer to other resources (e.g. a JSON-LD {@literal @context}), that never loads
 * resources that are not permitted (files, or HTTP(S) resources that are not authorized).
 */
public final class GuardedRdfParser {

    private GuardedRdfParser() {}

    /**
     * Parse the provided content into a new model.
     *
     * @param content The content.
     * @param language The syntax of the content.
     * @return The parsed model.
     */
    public static Model readModel(String content, Lang language) {
        return readModel(content, language, null);
    }

    /**
     * Parse the provided content into a new model.
     *
     * @param content The content.
     * @param language The syntax of the content (must be accepted by the configured {@link RdfContentRestrictions}).
     * @param nsPrefixes The namespace prefixes to set before parsing, to avoid mismatches (may be null).
     * @return The parsed model.
     */
    public static Model readModel(String content, Lang language, Map<String, String> nsPrefixes) {
        RdfSecurity.install();
        TestEngineConfiguration.RDF_CONTENT_RESTRICTIONS.check(language);
        var builder = RDFParserBuilder
                .create()
                .lang(language)
                .fromString(content);
        if (nsPrefixes != null) {
            PrefixMap prefixes = new PrefixMapStd();
            prefixes.putAll(nsPrefixes);
            builder = builder.prefixes(prefixes);
        }
        GuardedJsonLdDocumentLoader jsonLdLoader = null;
        if (Lang.JSONLD11.equals(language) || Lang.JSONLD.equals(language)) {
            // Never load files or unauthorized remote contexts (the options must not be shared between parsing operations).
            jsonLdLoader = new GuardedJsonLdDocumentLoader(UriAuthorizer.global());
            builder = builder.context(Context.create().set(LangJSONLD11.JSONLD_OPTIONS, GuardedJsonLdDocumentLoader.newOptions(jsonLdLoader)));
        }
        try {
            return builder.build().toModel();
        } catch (RiotException e) {
            if (jsonLdLoader != null) {
                // Report the actual reason if the parsing failed because of a resource that is not permitted.
                throw jsonLdLoader.toReportedError(e);
            }
            throw e;
        }
    }

}
