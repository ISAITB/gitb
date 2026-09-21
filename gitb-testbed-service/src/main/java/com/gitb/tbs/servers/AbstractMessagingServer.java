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

package com.gitb.tbs.servers;

import com.gitb.engine.CallbackPayloadStore;
import com.gitb.engine.CallbackPayloadStore.PayloadRef;
import com.gitb.messaging.Message;
import com.gitb.types.BinaryType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.CONTENT_TYPE_HEADER;
import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.getUriExtension;

public abstract class AbstractMessagingServer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMessagingServer.class);

    /**
     * Executor used to build and send the response once a call has been matched (immediately, or after being
     * held awaiting a matching receive step). This keeps that work off the servlet container's own worker
     * threads, and - crucially - off the Pekko dispatcher thread that may complete the match from within the
     * receive step's own processing (see CallbackManager.registerCallbackData).
     */
    protected final Executor messagingCallbackExecutor;

    protected AbstractMessagingServer(Executor messagingCallbackExecutor) {
        this.messagingCallbackExecutor = messagingCallbackExecutor;
    }

    /**
     * A single captured multipart part, referencing its payload in the {@link CallbackPayloadStore} rather than
     * holding it directly - see {@link CapturedRequest}.
     */
    protected record CapturedPart(String name, Optional<String> submittedFileName, Optional<String> contentType, PayloadRef payload) {
    }

    /**
     * A snapshot of the parts of an incoming {@link HttpServletRequest} needed to build the step report and (for
     * SOAP) reparse the envelope, captured eagerly on the servlet container thread. This is required because the
     * request may end up being processed later, on a different thread, once a call that could not be immediately
     * matched to a parked receive step is held (see {@link com.gitb.engine.CallbackManager#lookupHandlingData}) -
     * and a {@link HttpServletRequest} (in particular its body) must not be read outside of that original
     * processing window. The body and each multipart part are referenced via {@link PayloadRef} rather than held
     * as {@code byte[]} directly, so that a held call's payload can be spilled to disk instead of pinning heap for
     * as long as it is held - see {@link #capture(HttpServletRequest, boolean)}.
     */
    protected record CapturedRequest(String method, String fullUri, Optional<String> queryString,
                                      Optional<MapType> headers, Optional<String> contentType,
                                      PayloadRef body, Optional<List<CapturedPart>> multipartParts) {

        /**
         * Releases the storage backing this request's payload(s). Safe to call once handling has fully finished
         * with them - i.e. after the response has been built - on every completion path (served, dropped after
         * the wait window, or an executor rejection).
         */
        protected void release() {
            var store = CallbackPayloadStore.getInstance();
            store.release(body);
            multipartParts.ifPresent(parts -> parts.forEach(part -> store.release(part.payload())));
        }
    }

    /**
     * Captures the parts of {@code request} needed once the call has been matched (immediately, or after being
     * held). {@code spill} indicates whether the payload is worth spilling to disk rather than being kept in
     * memory - only true when the call is actually about to be parked; one already matched is consumed
     * microseconds later regardless of size, so keeping it in memory is cheaper than a disk round-trip.
     */
    protected CapturedRequest capture(HttpServletRequest request, boolean spill) {
        var store = CallbackPayloadStore.getInstance();
        Optional<MapType> headers = getRequestHeaders(request);
        Optional<String> contentType = headers.flatMap(this::getContentTypeHeader);
        PayloadRef bodyRef;
        List<CapturedPart> capturedParts = null;
        if (contentType.isPresent() && contentType.get().contains("multipart/form-data")) {
            // Multipart request parts.
            capturedParts = new ArrayList<>();
            try {
                for (var part: request.getParts()) {
                    PayloadRef partRef;
                    try (var in = part.getInputStream()) {
                        partRef = store.store(in, spill);
                    }
                    if (partRef.size() > 0) {
                        capturedParts.add(new CapturedPart(part.getName(), Optional.ofNullable(part.getSubmittedFileName()), Optional.ofNullable(part.getContentType()), partRef));
                    }
                }
            } catch (IOException | ServletException e) {
                // Release anything already captured for this request before propagating the failure.
                capturedParts.forEach(part -> store.release(part.payload()));
                throw new IllegalStateException("Error processing request parts", e);
            }
            if (capturedParts.isEmpty()) {
                capturedParts = null;
            }
            // The multipart parts (above) carry the actual content - the request body itself is empty.
            bodyRef = emptyPayloadRef(store, spill);
        } else {
            // Non-multipart.
            try (var in = request.getInputStream()) {
                bodyRef = store.store(in, spill);
            } catch (IOException e) {
                throw new IllegalStateException("Error processing request body", e);
            }
        }
        return new CapturedRequest(
                request.getMethod(),
                getFullRequestURI(request),
                Optional.ofNullable(request.getQueryString()),
                headers,
                contentType,
                bodyRef,
                Optional.ofNullable(capturedParts)
        );
    }

    private PayloadRef emptyPayloadRef(CallbackPayloadStore store, boolean spill) {
        try {
            return store.store(InputStream.nullInputStream(), spill);
        } catch (IOException e) {
            // Never actually thrown for an empty in-memory/no-op store, but store() declares it.
            throw new IllegalStateException("Error initialising an empty payload reference", e);
        }
    }

    /**
     * Rebuilds the {@link MapType} of multipart parts (as previously produced inline during capture) from a list
     * of {@link CapturedPart}s, reading each part's payload back from the {@link CallbackPayloadStore}. Only
     * called once a call has been matched, so this is never done needlessly for a call that ends up rejected.
     */
    protected Optional<MapType> toMultipartMap(Optional<List<CapturedPart>> capturedParts) {
        if (capturedParts.isEmpty()) {
            return Optional.empty();
        }
        var store = CallbackPayloadStore.getInstance();
        MapType multipartBodyType = new MapType();
        for (var part: capturedParts.get()) {
            byte[] partBytes = store.read(part.payload());
            if (part.submittedFileName().isEmpty()) {
                multipartBodyType.addItem(part.name(), new StringType(new String(partBytes)));
            } else {
                var binaryPartType = new BinaryType(partBytes);
                binaryPartType.setContentType(part.contentType().orElse(null));
                multipartBodyType.addItem(part.name(), binaryPartType);
            }
        }
        return Optional.of(multipartBodyType);
    }

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

    private String getFullRequestURI(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (request.getQueryString() != null) {
            requestUri += "?" + request.getQueryString();
        }
        return requestUri;
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
