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

import com.apicatalog.jsonld.JsonLdOptions;
import com.gitb.utils.UriAuthorizer;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParserRegistry;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.ReaderRIOTFactory;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.lang.LangJSONLD11;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sys.JenaSystem;

import java.io.InputStream;
import java.io.Reader;

/**
 * Factory for JSON-LD readers that makes sure that no remote resources are loaded without authorization.
 * <p>
 * Parsers we construct ourselves are explicitly configured with a guarded document loader. This factory is a safety net
 * for all other parsing that goes through Jena's parser registry (e.g. {@code Model.read} calls, or the loading of
 * JSON-LD documents referred to by {@code owl:imports}), for which Jena would otherwise use a default document loader
 * that loads any HTTP(S) or file URI.
 */
public class GuardedJsonLdReaderFactory implements ReaderRIOTFactory {

    private final ReaderRIOTFactory delegate;

    GuardedJsonLdReaderFactory(ReaderRIOTFactory delegate) {
        this.delegate = delegate;
    }

    /**
     * Replace the registered JSON-LD parsers with ones that guard the loading of remote resources. Calling this
     * multiple times is safe.
     */
    public static synchronized void install() {
        JenaSystem.init();
        for (Lang lang : new Lang[] { Lang.JSONLD, Lang.JSONLD11 }) {
            boolean triples = RDFParserRegistry.isTriples(lang);
            boolean quads = RDFParserRegistry.isQuads(lang);
            ReaderRIOTFactory current = RDFParserRegistry.getFactory(lang);
            if (current instanceof GuardedJsonLdReaderFactory) {
                // Already installed.
                continue;
            }
            var factory = new GuardedJsonLdReaderFactory(current);
            if (triples) {
                RDFParserRegistry.registerLangTriples(lang, factory);
            }
            if (quads) {
                RDFParserRegistry.registerLangQuads(lang, factory);
            }
        }
    }

    @Override
    public ReaderRIOT create(Lang language, ParserProfile profile) {
        return new GuardedReader(delegate.create(language, profile));
    }

    /**
     * Reader that ensures guarded options are present before delegating.
     */
    private static class GuardedReader implements ReaderRIOT {

        private final ReaderRIOT reader;

        private GuardedReader(ReaderRIOT reader) {
            this.reader = reader;
        }

        @Override
        public void read(InputStream in, String baseURI, ContentType ct, StreamRDF output, Context context) {
            var loader = new GuardedJsonLdDocumentLoader[1];
            var guardedContext = guard(context, loader);
            try {
                reader.read(in, baseURI, ct, output, guardedContext);
            } catch (RiotException e) {
                throw loader[0] == null ? e : loader[0].toReportedError(e);
            }
        }

        @Override
        public void read(Reader in, String baseURI, ContentType ct, StreamRDF output, Context context) {
            var loader = new GuardedJsonLdDocumentLoader[1];
            var guardedContext = guard(context, loader);
            try {
                reader.read(in, baseURI, ct, output, guardedContext);
            } catch (RiotException e) {
                throw loader[0] == null ? e : loader[0].toReportedError(e);
            }
        }

        private Context guard(Context context, GuardedJsonLdDocumentLoader[] loaderHolder) {
            if (context != null) {
                Object options = context.get(LangJSONLD11.JSONLD_OPTIONS);
                if (options instanceof JsonLdOptions jsonLdOptions && jsonLdOptions.getDocumentLoader() instanceof GuardedJsonLdDocumentLoader) {
                    // Already configured explicitly.
                    return context;
                }
            }
            Context guarded = context == null ? Context.create() : context.copy();
            loaderHolder[0] = new GuardedJsonLdDocumentLoader(UriAuthorizer.global());
            guarded.set(LangJSONLD11.JSONLD_OPTIONS, GuardedJsonLdDocumentLoader.newOptions(loaderHolder[0]));
            return guarded;
        }
    }

}
