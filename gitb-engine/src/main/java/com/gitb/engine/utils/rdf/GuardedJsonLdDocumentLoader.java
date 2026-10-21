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

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.apicatalog.jsonld.loader.HttpLoader;
import com.gitb.utils.UriAuthorizer;
import com.gitb.utils.UriNotAllowedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Loader of remote JSON-LD documents (e.g. contexts) that only loads resources that are permitted. Only HTTP(S) URIs
 * are supported, each subject to authorization before any access is made. Files are never loaded.
 * <p>
 * The default loader of the JSON-LD processor loads any HTTP(S) or file URI, making it possible for a crafted
 * {@literal @context} to trigger requests to arbitrary internal resources, or read local files.
 */
public class GuardedJsonLdDocumentLoader implements DocumentLoader {

    private static final Logger LOG = LoggerFactory.getLogger(GuardedJsonLdDocumentLoader.class);

    private final DocumentLoader httpLoader;
    private volatile UriNotAllowedException blocked;

    /**
     * Constructor.
     *
     * @param uriAuthorizer The authorizer for HTTP(S) URIs.
     */
    public GuardedJsonLdDocumentLoader(UriAuthorizer uriAuthorizer) {
        var loader = new HttpLoader(new AuthorizingJsonLdHttpClient(uriAuthorizer));
        /*
         * Set fallback type for remote contexts to avoid errors for non JSON/JSON-LD Content Types.
         * This allows us to proceed if e.g. the Content Type originally returned is "text/plain".
         */
        loader.fallbackContentType(MediaType.JSON);
        this.httpLoader = loader;
    }

    /**
     * Create the JSON-LD processing options to use for a single parsing operation.
     *
     * @param loader The loader to use.
     * @return The options (never share these between parsing operations as the processor modifies them).
     */
    public static JsonLdOptions newOptions(GuardedJsonLdDocumentLoader loader) {
        var options = new JsonLdOptions();
        options.setDocumentLoader(loader);
        return options;
    }

    /**
     * The JSON-LD parser reports a failure to load a resource as a generic parsing error. If the cause was a resource
     * that is not permitted, this is the original exception that should be reported instead.
     *
     * @param parsingError The error reported by the parser.
     * @return The exception to throw in place of the parsing error.
     */
    public RuntimeException toReportedError(RuntimeException parsingError) {
        return blocked == null ? parsingError : blocked;
    }

    @Override
    public Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {
        try {
            String scheme = url == null ? null : url.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                return httpLoader.loadDocument(url, options);
            } else {
                throw new UriNotAllowedException("The JSON-LD document [%s] is not allowed to be loaded because only http and https URIs can be looked up.".formatted(url));
            }
        } catch (UriNotAllowedException e) {
            blocked = e;
            throw e;
        } catch (JsonLdError e) {
            // The parser only reports a generic failure so we record the actual reason.
            LOG.warn("Unable to load JSON-LD document [{}]: {}", url, e.getCause() == null ? e.getMessage() : e.getCause().toString());
            throw e;
        }
    }

}
