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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;

import static com.gitb.engine.messaging.handlers.layer.application.http.HttpMessagingHandlerV2.*;
import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.*;

@RestController
public class HttpMessagingServer extends AbstractMessagingServer {

    private static final Logger LOG = LoggerFactory.getLogger(HttpMessagingServer.class);

    @RequestMapping(path = "/"+ API_PATH+"/{system}/{*extension}")
    public ResponseEntity<byte[]> handleForSystem(@PathVariable String system, @PathVariable String extension, HttpServletRequest request) {
        if (system == null || system.isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            return handleInternal(CallbackManager.getInstance().lookupHandlingData(CallbackType.HTTP, system, (data) -> matchIncomingRequest(
                    HttpMethod.valueOf(request.getMethod()),
                    extension,
                    Optional.ofNullable(request.getQueryString()),
                    data,
                    () -> getMethod(data.getFragments(), METHOD_ARGUMENT_NAME),
                    URI_EXTENSION_ARGUMENT_NAME
            )), request);
        }
    }

    private ResponseEntity<byte[]> handleInternal(Optional<SessionCallbackData> data, HttpServletRequest request) {
        if (data.isEmpty()) {
            // No test session found to be waiting for this call.
            return ResponseEntity.notFound().build();
        } else {
            try {
                /*
                 * Prepare response for SUT.
                 */
                var responseBody = Optional.ofNullable(getAndConvert(data.get().data().inputs().getFragments(), HttpMessagingHandlerV2.BODY_ARGUMENT_NAME, BinaryType.BINARY_DATA_TYPE, BinaryType.class)).map(BinaryType::serializeByDefaultEncoding);
                var responseHeaders = getMapOfValues(data.get().data().inputs().getFragments(), HttpMessagingHandlerV2.HEADERS_ARGUMENT_NAME);
                var responseStatus = getStatus(data.get().data().inputs().getFragments(), STATUS_ARGUMENT_NAME, () -> HttpStatus.OK);
                // Build response.
                var builder = ResponseEntity.status(responseStatus); // Status
                // Headers.
                responseHeaders.forEach((key, value) -> value.forEach(headerValue -> builder.header(key, headerValue)));
                // Body.
                ResponseEntity<byte[]> responseResult = responseBody.map(builder::body).orElse(builder.build());
                /*
                 * Prepare report for test step.
                 */
                Message report = new Message();
                MapType requestMap = new MapType();
                MapType responseMap = new MapType();
                report.addInput(REPORT_ITEM_REQUEST, requestMap);
                report.addInput(REPORT_ITEM_RESPONSE, responseMap);
                // Request method.
                requestMap.addItem(REPORT_ITEM_METHOD, new StringType(request.getMethod()));
                // Request URI.
                requestMap.addItem(REPORT_ITEM_URI, getFullRequestURI(request));
                // Request headers.
                Optional<MapType> requestHeaders = getRequestHeaders(request);
                requestHeaders.ifPresent(headers -> requestMap.addItem(REPORT_ITEM_HEADERS, headers));
                Optional<String> requestContentTypeHeader = requestHeaders.flatMap(this::getContentTypeHeader);
                // Request body.
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
                    // Non-multipart.
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
                // Response status.
                responseMap.addItem(REPORT_ITEM_STATUS, new StringType(String.valueOf(responseStatus.value())));
                // Response headers.
                getHeadersForReport(responseHeaders).ifPresent(headers -> responseMap.getItems().put(REPORT_ITEM_HEADERS, headers));
                // Response body.
                responseBody.flatMap(body -> getResponseBody(body, responseHeaders)).ifPresent(item -> responseMap.addItem(REPORT_ITEM_BODY, item));
                // Make callback for step.
                CallbackManager.getInstance().callbackReceived(data.get().sessionId(), data.get().callId(), MessagingHandlerUtils.generateSuccessReport(report));
                /*
                 * Return response.
                 */
                return responseResult;
            } catch (Exception error) {
                // Pass the caught exception as part of the notification. This will get logged by the relevant session actor.
                CallbackManager.getInstance().callbackReceived(data.get().sessionId(), data.get().callId(), new GITBEngineInternalError("An unexpected error occurred while processing a HTTP request", error));
                return ResponseEntity.internalServerError().build();
            }
        }
    }

}
