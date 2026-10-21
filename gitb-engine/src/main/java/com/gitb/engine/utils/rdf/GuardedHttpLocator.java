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

import com.gitb.utils.GuardedUriReader;
import com.gitb.utils.UriAuthorizer;
import com.gitb.utils.UriNotAllowedException;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.system.streammgr.Locator;

import java.net.URI;

/**
 * Jena locator used to retrieve the resources referred to by an ontology (e.g. via {@code owl:imports}). Only HTTP(S)
 * resources can be looked up, subject to authorization of every request made (including redirects).
 */
public class GuardedHttpLocator implements Locator {

    @Override
    public TypedInputStream open(String uri) {
        if (uri.regionMatches(true, 0, "http://", 0, 7) || uri.regionMatches(true, 0, "https://", 0, 8)) {
            var response = GuardedUriReader.open(URI.create(uri), WebContent.defaultRDFAcceptHeader, UriAuthorizer.global());
            try {
                ContentType contentType = response.contentType()
                        .map(ContentType::create)
                        .orElseGet(() -> RDFLanguages.guessContentType(uri));
                // The stream is handed over to Jena, that closes it once read.
                return new TypedInputStream(response.stream(), contentType);
            } catch (RuntimeException e) {
                response.closeQuietly();
                throw e;
            }
        }
        int colon = uri.indexOf(':');
        int slash = uri.indexOf('/');
        if (colon >= 0 && (slash < 0 || colon < slash)) {
            // A reference with another scheme (e.g. file:, jar: or ftp:) that must not be looked up.
            throw new UriNotAllowedException("The ontology reference [%s] is not allowed because only http and https URIs can be looked up.".formatted(uri));
        }
        // Not something we can look up.
        return null;
    }

    @Override
    public String getName() {
        return "GuardedHttpLocator";
    }

}
