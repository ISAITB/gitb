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

package com.gitb.tbs.servers;

import com.gitb.messaging.Message;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.CONTENT_TYPE_HEADER;
import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.getUriExtension;

public abstract class AbstractMessagingServer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMessagingServer.class);

    Boolean matchIncomingRequest(HttpMethod detectedMethod, String detectedUriExtension, Optional<String> detectedQueryString, Message data, Supplier<Optional<HttpMethod>> expectedMethodSupplier, String expectedUriExtensionInputName) {
        try {
            var expectedMethod = expectedMethodSupplier.get();
            var expectedUriExtension = getUriExtension(data.getFragments(), expectedUriExtensionInputName);
            Function<String, Boolean> uriMatcher = (expectedExtension) -> {
                String expectedBeforeQueryString;
                String detectedBeforeQueryString;
                String expectedAfterQueryString = null;
                String detectedAfterQueryString = null;
                if (expectedExtension.indexOf('?') != -1) {
                    var parts = StringUtils.split(expectedExtension, '?');
                    expectedBeforeQueryString = Strings.CS.appendIfMissing(Strings.CS.prependIfMissing(parts[0].toLowerCase(), "/"), "/");
                    expectedAfterQueryString = parts[1];
                } else {
                    expectedBeforeQueryString = Strings.CS.appendIfMissing(Strings.CS.prependIfMissing(expectedExtension.toLowerCase(), "/"), "/");
                }
                detectedBeforeQueryString = Strings.CS.appendIfMissing(Strings.CS.prependIfMissing(detectedUriExtension.toLowerCase(), "/"), "/");
                if (detectedQueryString.isPresent()) {
                    detectedAfterQueryString = detectedQueryString.get();
                }
                return Objects.equals(expectedBeforeQueryString, detectedBeforeQueryString) &&
                        (expectedAfterQueryString == null || Objects.equals(expectedAfterQueryString, detectedAfterQueryString));
            };
            if (expectedMethod.isPresent() && expectedUriExtension.isPresent()) {
                return expectedMethod.get().equals(detectedMethod) && uriMatcher.apply(expectedUriExtension.get());
            } else if (expectedMethod.isPresent()) {
                return expectedMethod.get().equals(detectedMethod);
            } else if (expectedUriExtension.isPresent()) {
                return uriMatcher.apply(expectedUriExtension.get());
            } else {
                // Matching only on the basis of the system key used.
                return true;
            }
        } catch (Exception e) {
            // Nothing we can do here but log a possible error (one should never be raised however).
            LOG.error("Unexpected error while performing HTTP request matching", e);
            return false;
        }
    }


    StringType getFullRequestURI(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (request.getQueryString() != null) {
            requestUri += "?" + request.getQueryString();
        }
        return new StringType(requestUri);
    }

    Optional<MapType> getRequestHeaders(HttpServletRequest request) {
        if (request.getHeaderNames().hasMoreElements()) {
            MapType requestHeaders = new MapType();
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                var headerValues = new ArrayList<String>();
                request.getHeaders(headerName).asIterator().forEachRemaining(headerValues::add);
                requestHeaders.addItem(headerName, new StringType(String.join(", ", headerValues)));
            });
            return Optional.of(requestHeaders);
        }
        return Optional.empty();
    }

    Optional<String> getContentTypeHeader(MapType headers) {
        for (var headerEntry: headers.getItems().entrySet()) {
            if (headerEntry.getKey().equalsIgnoreCase(CONTENT_TYPE_HEADER)) {
                return Optional.of(headerEntry.getValue().toString());
            }
        }
        return Optional.empty();
    }



}
