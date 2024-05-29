package com.gitb.tbs.servers;

import com.gitb.engine.CallbackManager;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpMessagingHandlerV2;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.Message;
import com.gitb.messaging.callback.CallbackType;
import com.gitb.messaging.callback.SessionCallbackData;
import com.gitb.types.BinaryType;
import com.gitb.types.DataType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static com.gitb.engine.messaging.handlers.layer.application.http.HttpMessagingHandlerV2.*;
import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.getAndConvert;
import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.getMapOfValues;

@RestController
public class HttpMessagingServer {

    private static final Logger LOG = LoggerFactory.getLogger(HttpMessagingServer.class);

    @RequestMapping(path = "/"+ API_PATH+"/{system}/{*extension}")
    public ResponseEntity<byte[]> handleForSystem(@PathVariable String system, @PathVariable String extension, HttpServletRequest request) {
        if (system == null || system.isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            return handleInternal(CallbackManager.getInstance().lookupHandlingData(CallbackType.HTTP, system, (data) -> match(HttpMethod.valueOf(request.getMethod()), extension, Optional.ofNullable(request.getQueryString()), data)), request);
        }
    }

    private Boolean match(HttpMethod detectedMethod, String detectedUriExtension, Optional<String> detectedQueryString, Message data) {
        try {
            var expectedMethod = HttpMessagingHandlerV2.getMethod(data.getFragments());
            var expectedUriExtension = getUriExtension(data.getFragments());
            Function<String, Boolean> uriMatcher = (expectedExtension) -> {
                String expectedBeforeQueryString;
                String detectedBeforeQueryString;
                String expectedAfterQueryString = null;
                String detectedAfterQueryString = null;
                if (expectedExtension.indexOf('?') != -1) {
                    var parts = StringUtils.split(expectedExtension, '?');
                    expectedBeforeQueryString = StringUtils.appendIfMissing(StringUtils.prependIfMissing(parts[0].toLowerCase(), "/"), "/");
                    expectedAfterQueryString = parts[1];
                } else {
                    expectedBeforeQueryString = StringUtils.appendIfMissing(StringUtils.prependIfMissing(expectedExtension.toLowerCase(), "/"), "/");
                }
                detectedBeforeQueryString = StringUtils.appendIfMissing(StringUtils.prependIfMissing(detectedUriExtension.toLowerCase(), "/"), "/");
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

    private ResponseEntity<byte[]> handleInternal(Optional<SessionCallbackData> data, HttpServletRequest request) {
        if (data.isEmpty()) {
            // No test session found to be waiting for this call.
            return ResponseEntity.notFound().build();
        } else {
            try {
                var inputs = data.get().data().inputs().getFragments();
                // Test session found.
                var responseBody = Optional.ofNullable(getAndConvert(inputs, HttpMessagingHandlerV2.BODY_ARGUMENT_NAME, BinaryType.BINARY_DATA_TYPE, BinaryType.class)).map(BinaryType::serializeByDefaultEncoding);
                var responseHeaders = getMapOfValues(inputs, HttpMessagingHandlerV2.HEADERS_ARGUMENT_NAME);
                var responseStatus = HttpMessagingHandlerV2.getStatus(inputs, () -> HttpStatus.OK);
                /*
                 * Prepare response for SUT.
                 */
                var builder = ResponseEntity.status(responseStatus); // Status
                // Headers.
                responseHeaders.forEach((key, value) -> value.forEach(headerValue -> builder.header(key, headerValue)));
                // Body.
                responseBody.ifPresent(builder::body);
                /*
                 * Prepare report for test step.
                 */
                Message report = new Message();
                MapType requestMap = new MapType();
                report.addInput(REPORT_ITEM_REQUEST, requestMap);
                requestMap.addItem(REPORT_ITEM_METHOD, new StringType(request.getMethod()));
                String requestUri = request.getRequestURI();
                if (request.getQueryString() != null) {
                    requestUri += "?" + request.getQueryString();
                }
                requestMap.addItem(REPORT_ITEM_URI, new StringType(requestUri));
                // Request headers.
                Optional<String> requestContentTypeHeader = Optional.empty();
                if (request.getHeaderNames().hasMoreElements()) {
                    MapType requestHeaders = new MapType();
                    request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                        var headerValues = new ArrayList<String>();
                        request.getHeaders(headerName).asIterator().forEachRemaining(headerValues::add);
                        requestHeaders.addItem(headerName, new StringType(String.join(", ", headerValues)));
                    });
                    for (var headerEntry: requestHeaders.getItems().entrySet()) {
                        if (headerEntry.getKey().equalsIgnoreCase(CONTENT_TYPE)) {
                            requestContentTypeHeader = Optional.of(headerEntry.getValue().toString());
                        }
                    }
                    requestMap.addItem(REPORT_ITEM_HEADERS, requestHeaders);
                }
                DataType requestBodyType = null;
                if (requestContentTypeHeader.isPresent() && requestContentTypeHeader.get().contains("multipart/form-data")) {
                    // Multipart request parts
                    MapType multipartBodyType = new MapType();
                    try {
                        for (var part: request.getParts()) {
                            var partBytes = IOUtils.toByteArray(part.getInputStream());
                            if (partBytes != null && partBytes.length > 0) {
                                if (part.getSubmittedFileName() == null) {
                                    multipartBodyType.addItem(part.getName(), new StringType(new String(partBytes)));
                                } else {
                                    var binaryPartType = new BinaryType(partBytes);
                                    binaryPartType.setContentType(part.getContentType());
                                    multipartBodyType.addItem(part.getName(), binaryPartType);
                                }
                            }
                        }
                    } catch (IOException | ServletException e) {
                        throw new IllegalStateException("Error processing request parts", e);
                    }
                    if (!multipartBodyType.isEmpty()) {
                        requestBodyType = multipartBodyType;
                    }
                } else {
                    // Request body.
                    byte[] requestBodyBytes;
                    try (var in = request.getInputStream()) {
                        requestBodyBytes = IOUtils.toByteArray(in);
                    } catch (IOException e) {
                        throw new IllegalStateException("Error processing request body", e);
                    }
                    if (requestBodyBytes.length > 0) {
                        requestBodyType = new BinaryType(requestBodyBytes);
                    }
                }
                if (requestBodyType != null) {
                    requestMap.addItem(REPORT_ITEM_BODY, requestBodyType);
                }
                MapType responseMap = new MapType();
                // Response status.
                responseMap.addItem(REPORT_ITEM_STATUS, new StringType(String.valueOf(responseStatus.value())));
                // Response headers.
                if (!responseHeaders.isEmpty()) {
                    MapType responseHeadersItem = new MapType();
                    responseHeaders.forEach((headerName, headerValues) -> {
                        responseHeadersItem.addItem(headerName, new StringType(String.join(", ", headerValues)));
                    });
                    responseMap.getItems().put(REPORT_ITEM_HEADERS, responseHeadersItem);
                }
                // Response body.
                responseBody.ifPresent(value -> {
                    var bodyItem = new BinaryType(responseBody.get());
                    if (responseHeaders.containsKey(CONTENT_TYPE)) {
                        responseHeaders.get(CONTENT_TYPE).stream()
                                .findFirst()
                                .flatMap(contentType -> Arrays.stream(StringUtils.split(contentType, ';')).findFirst())
                                .ifPresent(bodyItem::setContentType);
                    }
                    responseMap.addItem(REPORT_ITEM_BODY, bodyItem);
                });
                report.addInput(REPORT_ITEM_RESPONSE, responseMap);
                CallbackManager.getInstance().callbackReceived(data.get().sessionId(), data.get().callId(), MessagingHandlerUtils.generateSuccessReport(report));
                /*
                 * Return response.
                 */
                return builder.build();
            } catch (Exception error) {
                // Pass the caught exception as part of the notification. This will get logged by the relevant session actor.
                CallbackManager.getInstance().callbackReceived(data.get().sessionId(), data.get().callId(), new GITBEngineInternalError("An unexpected error occurred while processing a HTTP request", error));
                return ResponseEntity.internalServerError().build();
            }
        }
    }

}
