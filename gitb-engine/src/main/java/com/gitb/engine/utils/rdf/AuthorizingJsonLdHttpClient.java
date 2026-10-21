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
import com.apicatalog.jsonld.http.DefaultHttpClient;
import com.apicatalog.jsonld.http.HttpClient;
import com.apicatalog.jsonld.http.HttpResponse;
import com.gitb.utils.BudgetedHttpClient;
import com.gitb.utils.UriAuthorizer;

import java.net.URI;
import java.time.Duration;

/**
 * HTTP client used to load remote JSON-LD documents (e.g. contexts) that ensures that every requested URI is authorized
 * before a request is made.
 * <p>
 * The JSON-LD processor follows redirects and link headers by issuing a new request for each hop. By never following
 * redirects at the HTTP level and checking each requested URI here, every hop is subject to authorization.
 */
public class AuthorizingJsonLdHttpClient implements HttpClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final UriAuthorizer uriAuthorizer;
    private final HttpClient delegate;

    /**
     * Constructor.
     *
     * @param uriAuthorizer The authorizer for requested URIs.
     */
    public AuthorizingJsonLdHttpClient(UriAuthorizer uriAuthorizer) {
        this.uriAuthorizer = uriAuthorizer;
        // Requests count towards the limits of the test step doing the lookup (each redirect or link followed by the processor is a new request).
        this.delegate = new DefaultHttpClient(BudgetedHttpClient.newClient(CONNECT_TIMEOUT)).timeout(REQUEST_TIMEOUT);
    }

    @Override
    public HttpResponse send(URI targetUri, String requestProfile) throws JsonLdError {
        uriAuthorizer.check(targetUri);
        return delegate.send(targetUri, requestProfile);
    }

    @Override
    public HttpClient timeout(Duration timeout) {
        delegate.timeout(timeout);
        return this;
    }

}
