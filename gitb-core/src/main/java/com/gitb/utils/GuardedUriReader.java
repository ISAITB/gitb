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

package com.gitb.utils;

import com.gitb.exceptions.GITBEngineInternalError;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Reads remote resources over HTTP(S), authorizing every request made (including those that follow redirects), so
 * that an allowed URI cannot be used to reach one that is not allowed.
 * <p>
 * Redirects are never followed by the HTTP client itself. They are followed here (up to a maximum number) after
 * checking the target with the provided {@link UriAuthorizer}.
 */
public final class GuardedUriReader {

    /**
     * The maximum number of redirects followed.
     */
    public static final int MAX_REDIRECTS = 10;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private GuardedUriReader() {}

    /**
     * The client is created lazily and shared (a client holds on to resources until it is garbage collected).
     */
    private static final class ClientHolder {
        private static final HttpClient CLIENT = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(CONNECT_TIMEOUT)
                .proxy(ProxySelector.getDefault())
                .build();
    }

    /**
     * The response of a successful request.
     * <p>
     * The response owns its stream. Either close the response (e.g. with try-with-resources) once the content has been read,
     * or hand the stream over to a consumer that takes care of closing it - in which case the response must not be closed,
     * apart from when something fails before the hand-over (see {@link #closeQuietly()}).
     *
     * @param stream The stream of the response's body.
     * @param contentType The content type of the response, without parameters (if provided).
     * @param uri The URI that was finally read (after redirects).
     */
    public record Response(InputStream stream, Optional<String> contentType, URI uri) implements Closeable {

        @Override
        public void close() throws IOException {
            stream.close();
        }

        /**
         * Close the response, ignoring any failure to do so.
         */
        public void closeQuietly() {
            try {
                stream.close();
            } catch (IOException e) {
                // Ignore.
            }
        }

    }

    /**
     * Open a stream to the provided URI.
     *
     * @param uri The URI.
     * @param acceptHeader The value for the Accept header (may be null).
     * @param authorizer The authorizer for the URI and any redirects.
     * @return The response.
     * @throws UriNotAllowedException If the URI, or a redirect target, is not allowed.
     * @throws GITBEngineInternalError If the resource cannot be read.
     */
    public static Response open(URI uri, String acceptHeader, UriAuthorizer authorizer) {
        Set<URI> visited = new HashSet<>();
        URI current = uri;
        try {
            for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
                if (!visited.add(current)) {
                    throw new GITBEngineInternalError("Redirect loop detected at [%s]".formatted(current));
                }
                authorizer.check(current);
                var requestBuilder = HttpRequest.newBuilder().uri(current).timeout(REQUEST_TIMEOUT).GET();
                if (acceptHeader != null && !acceptHeader.isBlank()) {
                    requestBuilder.header("Accept", acceptHeader);
                }
                HttpResponse<InputStream> response = ClientHolder.CLIENT.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();
                if (isRedirect(status)) {
                    response.body().close();
                    String location = response.headers().firstValue("Location").map(String::trim).orElse("");
                    if (location.isEmpty()) {
                        throw new GITBEngineInternalError("Redirect without location from [%s]".formatted(current));
                    }
                    current = current.resolve(location);
                } else if (status >= 400) {
                    response.body().close();
                    throw new GITBEngineInternalError("Unable to read [%s]: the server returned status %s".formatted(current, status));
                } else {
                    return new Response(response.body(), response.headers().firstValue("Content-Type").map(GuardedUriReader::withoutParameters), current);
                }
            }
            throw new GITBEngineInternalError("Too many redirects from [%s]".formatted(uri));
        } catch (IOException | IllegalArgumentException e) {
            throw new GITBEngineInternalError("Unable to read [%s]".formatted(current), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GITBEngineInternalError("Interrupted while reading [%s]".formatted(current), e);
        }
    }

    private static String withoutParameters(String contentType) {
        int index = contentType.indexOf(';');
        return (index < 0 ? contentType : contentType.substring(0, index)).trim();
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

}
