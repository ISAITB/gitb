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

import com.gitb.utils.BudgetedHttpClient;
import com.gitb.utils.UriNotAllowedException;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.query.ARQ;
import org.apache.jena.riot.adapters.AdapterFileManager;
import org.apache.jena.riot.system.streammgr.StreamManager;
import org.apache.jena.sys.JenaSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Entry point for the restrictions applied when working with RDF content, which may refer to external resources (JSON-LD
 * contexts, ontology imports, SPARQL services) that must not be looked up freely.
 */
public final class RdfSecurity {

    private static final Logger LOG = LoggerFactory.getLogger(RdfSecurity.class);
    private static boolean installed = false;

    private RdfSecurity() {}

    /**
     * Install the global safeguards. Calling this multiple times is safe.
     * <ul>
     *     <li>JSON-LD parsing never loads files or unauthorized remote contexts.</li>
     *     <li>SPARQL SERVICE calls are authorized and never follow redirects.</li>
     * </ul>
     */
    public static synchronized void install() {
        if (!installed) {
            JenaSystem.init();
            GuardedJsonLdReaderFactory.install();
            AuthorizingServiceExecutor.install();
            // Redirects are never followed for SERVICE calls (otherwise an authorized endpoint could redirect to one that is not).
            // The calls also count towards the limits of the test step making them (see RemoteResourceBudget).
            ARQ.getContext().set(ARQ.httpQueryClient, BudgetedHttpClient.newClient(Duration.ofSeconds(10)));
            installed = true;
        }
    }

    /**
     * Create a document manager to use for loading the imports of an ontology. It only looks up HTTP(S) resources
     * (subject to authorization), and never reads local files, the classpath or any other kind of source. It is not
     * shared, avoiding any caching of imports between uses.
     *
     * @return The document manager.
     */
    @SuppressWarnings("removal")
    public static OntDocumentManager newOntDocumentManager() {
        install();
        var streamManager = new StreamManager();
        streamManager.addLocator(new GuardedHttpLocator());
        var documentManager = new OntDocumentManager(new AdapterFileManager(streamManager), (String) null);
        documentManager.setReadFailureHandler((url, model, e) -> {
            // Imports that cannot be read are ignored, unless they were not permitted.
            Throwable current = e;
            while (current != null) {
                if (current instanceof UriNotAllowedException blocked) {
                    throw blocked;
                }
                current = current.getCause();
            }
            LOG.warn("Unable to read ontology import [{}]: {}", url, e != null ? e.getMessage() : "");
        });
        return documentManager;
    }

}
