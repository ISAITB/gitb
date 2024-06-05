package com.gitb.tbs.servers;

import com.gitb.engine.CallbackManager;
import com.gitb.engine.messaging.handlers.layer.application.soap.AttachmentInfo;
import com.gitb.engine.messaging.handlers.layer.application.soap.SoapVersion;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.Message;
import com.gitb.messaging.callback.CallbackType;
import com.gitb.messaging.callback.SessionCallbackData;
import com.gitb.types.MapType;
import com.gitb.types.ObjectType;
import com.gitb.types.StringType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.soap.SOAPMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.gitb.engine.messaging.handlers.layer.application.http.HttpMessagingHandlerV2.REPORT_ITEM_HEADERS;
import static com.gitb.engine.messaging.handlers.layer.application.soap.SoapMessagingHandlerV2.*;
import static com.gitb.engine.messaging.handlers.layer.application.soap.SoapVersion.VERSION_1_1;
import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.*;

@RestController
public class SoapMessagingServer extends AbstractMessagingServer {

    @PostMapping(path = "/"+ API_PATH+"/{system}/{*extension}")
    public ResponseEntity<byte[]> handleForSystem(@PathVariable String system, @PathVariable String extension, HttpServletRequest request) {
        if (system == null || system.isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            return handleInternal(CallbackManager.getInstance().lookupHandlingData(CallbackType.SOAP, system, (data) -> matchIncomingRequest(
                    HttpMethod.POST,
                    extension,
                    Optional.ofNullable(request.getQueryString()),
                    data,
                    () -> Optional.of(HttpMethod.POST),
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
                HttpStatus responseStatus = getStatus(data.get().data().inputs().getFragments(), STATUS_ARGUMENT_NAME, () -> HttpStatus.OK);
                Map<String, List<String>> responseHeaders = getMapOfValues(data.get().data().inputs().getFragments(), HEADERS_ARGUMENT_NAME);
                SoapVersion responseVersion = getSoapVersion(data.get().data().inputs().getFragments(), VERSION_ARGUMENT_NAME, () -> MessagingHandlerUtils.soapVersionFromHeaders(responseHeaders));
                byte[] responseEnvelope = getSoapEnvelope(data.get().data().inputs().getFragments(), ENVELOPE_ARGUMENT_NAME);
                Optional<List<AttachmentInfo>> responseAttachments = getSoapAttachments(data.get().data().inputs().getFragments(), ATTACHMENTS_ARGUMENT_NAME);
                SOAPMessage responseSoapMessage = createSoapMessage(responseVersion, responseEnvelope, responseAttachments);
                if (!responseHeaders.containsKey(CONTENT_TYPE_HEADER)) {
                    responseHeaders.put(CONTENT_TYPE_HEADER, List.of(createSoapContentTypeHeader(responseSoapMessage, responseVersion)));
                }
                // Build response.
                var builder = ResponseEntity.status(responseStatus); // Status.
                // Headers.
                responseHeaders.forEach((key, value) -> value.forEach(headerValue -> builder.header(key, headerValue)));
                // Body.
                ResponseEntity<byte[]> responseResult = builder.body(soapMessageToBytes(responseSoapMessage));
                /*
                 * Prepare report for test step.
                 */
                Message report = new Message();
                MapType requestMap = new MapType();
                MapType responseMap = new MapType();
                report.addInput(REPORT_ITEM_REQUEST, requestMap);
                report.addInput(REPORT_ITEM_RESPONSE, responseMap);
                // Request URI.
                requestMap.addItem(REPORT_ITEM_URI, getFullRequestURI(request));
                // Request headers.
                Optional<MapType> requestHeaders = getRequestHeaders(request);
                requestHeaders.ifPresent(headers -> requestMap.addItem(REPORT_ITEM_HEADERS, headers));
                Optional<String> requestContentTypeHeader = requestHeaders.flatMap(this::getContentTypeHeader);
                SoapVersion requestSoapVersion = requestContentTypeHeader
                        .map(SoapVersion::forContentTypeHeader)
                        .orElse(VERSION_1_1);
                SOAPMessage requestSoapMessage = deserialiseSoapMessage(requestSoapVersion, request.getInputStream(), requestContentTypeHeader);
                // Request envelope.
                ObjectType requestEnvelopeItem = getSoapEnvelope(requestSoapMessage);
                // Request body.
                Optional<ObjectType> requestBodyItem = getSoapBody(requestSoapMessage);
                // Request attachments.
                Optional<MapType> requestAttachmentsItem = getAttachments(requestSoapMessage);
                // Only if there wasn't any failure, add to report.
                requestMap.addItem(REPORT_ITEM_ENVELOPE, requestEnvelopeItem);
                requestBodyItem.ifPresent(body -> requestMap.getItems().put(REPORT_ITEM_BODY, body));
                requestAttachmentsItem.ifPresent(item -> requestMap.addItem(REPORT_ITEM_ATTACHMENTS, item));
                // Response status.
                responseMap.addItem(REPORT_ITEM_STATUS, new StringType(String.valueOf(responseStatus.value())));
                // Response headers.
                getHeadersForReport(responseHeaders).ifPresent(responseHeadersItem -> responseMap.getItems().put(REPORT_ITEM_HEADERS, responseHeadersItem));
                // Response envelope.
                ObjectType responseEnvelopeItem = getSoapEnvelope(responseSoapMessage);
                // Response body.
                Optional<ObjectType> responseBodyItem = getSoapBody(responseSoapMessage);
                // Only if there wasn't any failure, add to report.
                responseMap.getItems().put(REPORT_ITEM_ENVELOPE, responseEnvelopeItem);
                responseBodyItem.ifPresent(body -> responseMap.getItems().put(REPORT_ITEM_BODY, body));
                responseAttachments.ifPresent(parts -> {
                    var attachmentsItem = new MapType();
                    parts.forEach(part -> attachmentsItem.addItem(part.name(), part.content()));
                    responseMap.addItem(REPORT_ITEM_ATTACHMENTS, attachmentsItem);
                });
                // Make callback for step.
                CallbackManager.getInstance().callbackReceived(data.get().sessionId(), data.get().callId(), MessagingHandlerUtils.generateSuccessReport(report));
                /*
                 * Return response.
                 */
                return responseResult;
            } catch (Exception error) {
                // Pass the caught exception as part of the notification. This will get logged by the relevant session actor.
                CallbackManager.getInstance().callbackReceived(data.get().sessionId(), data.get().callId(), new GITBEngineInternalError("An unexpected error occurred while processing a SOAP request", error));
                return ResponseEntity.internalServerError().build();
            }
        }
    }

}
