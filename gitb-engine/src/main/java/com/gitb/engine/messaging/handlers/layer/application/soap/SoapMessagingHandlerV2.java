package com.gitb.engine.messaging.handlers.layer.application.soap;

import com.gitb.core.Configuration;
import com.gitb.engine.CallbackManager;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.layer.AbstractNonWorkerMessagingHandler;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.messaging.DeferredMessagingReport;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.messaging.callback.CallbackData;
import com.gitb.messaging.callback.CallbackType;
import com.gitb.tdl.MessagingStep;
import com.gitb.types.*;
import jakarta.xml.soap.SOAPMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.*;

@MessagingHandler(name="SoapMessagingV2")
public class SoapMessagingHandlerV2 extends AbstractNonWorkerMessagingHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SoapMessagingHandlerV2.class);

    public static final String HEADERS_ARGUMENT_NAME = "headers";
    public static final String URI_ARGUMENT_NAME = "uri";
    public static final String STATUS_ARGUMENT_NAME = "status";
    public static final String VERSION_ARGUMENT_NAME = "version";
    public static final String ACTION_ARGUMENT_NAME = "action";
    public static final String ENVELOPE_ARGUMENT_NAME = "envelope";
    public static final String ATTACHMENTS_ARGUMENT_NAME = "attachments";
    public static final String TOLERATE_NON_SOAP_ARGUMENT_NAME = "tolerateNonSoapResponse";
    public static final String URI_EXTENSION_ARGUMENT_NAME = "uriExtension";

    public static final String REPORT_ITEM_REQUEST = "request";
    public static final String REPORT_ITEM_RESPONSE = "response";
    public static final String REPORT_ITEM_URI = "uri";
    public static final String REPORT_ITEM_ENVELOPE = "envelope";
    public static final String REPORT_ITEM_BODY = "body";
    public static final String REPORT_ITEM_ERROR = "error";
    public static final String REPORT_ITEM_ATTACHMENTS = "attachments";
    public static final String REPORT_ITEM_STATUS = "status";
    public static final String REPORT_ITEM_HEADERS = "headers";

    public static final String SOAP_ACTION_HEADER = "SOAPAction";

    public static final String API_PATH = "soap";

    @Override
    public MessagingReport sendMessage(String sessionId, String transactionId, String stepId, List<Configuration> configurations, Message message) {
        /*
         * Manage inputs.
         */
        // URI of the target SOAP service.
        String uri = Objects.requireNonNull(getAndConvert(message.getFragments(), URI_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class), "Input [%s] must be provided".formatted(URI_ARGUMENT_NAME)).toString();
        // HTTP headers.
        var headers = getMapOfValues(message.getFragments(), HEADERS_ARGUMENT_NAME);
        // SOAP version.
        SoapVersion soapVersion = getSoapVersion(message.getFragments(), VERSION_ARGUMENT_NAME, () -> MessagingHandlerUtils.soapVersionFromHeaders(headers));
        // SOAP message.
        byte[] soapEnvelopeBytes = getSoapEnvelope(message.getFragments(), ENVELOPE_ARGUMENT_NAME);
        // SOAPAction.
        Optional<String> soapAction = Optional.ofNullable(getAndConvert(message.getFragments(), ACTION_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class)).map(StringType::toString);
        // Attachments.
        Optional<List<AttachmentInfo>> attachments = getSoapAttachments(message.getFragments(), ATTACHMENTS_ARGUMENT_NAME);
        // Tolerate non-SOAP responses.
        boolean tolerateNonSoapResponse = Optional.ofNullable(getAndConvert(message.getFragments(), TOLERATE_NON_SOAP_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(value -> (Boolean) value.getValue())
                .orElse(false);
        /*
         * Create SOAP message.
         */
        SOAPMessage soapMessage = createSoapMessage(soapVersion, soapEnvelopeBytes, attachments);
        headers.put(CONTENT_TYPE_HEADER, List.of(createSoapContentTypeHeader(soapMessage, soapVersion)));
        soapAction.ifPresent(action -> headers.put(SOAP_ACTION_HEADER, List.of(action)));
        /*
         * Construct HTTP message.
         */
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .POST(HttpRequest.BodyPublishers.ofByteArray(soapMessageToBytes(soapMessage)));
        headers.forEach((key, value) -> value.forEach(headerValue -> builder.header(key, headerValue)));
        // Make request.
        CompletableFuture<HttpResponse<byte[]>> asyncResponse;
        try {
            asyncResponse = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build()
                    .sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Error while contacting remote system", e);
        }
        CompletableFuture<MessagingReport> asyncReport = asyncResponse.thenApply((response) -> {
            var messageForReport = new Message();
            /*
             * Create report - request
             */
            var requestItem = new MapType();
            messageForReport.addInput("request", requestItem);
            // URI.
            requestItem.getItems().put(REPORT_ITEM_URI, new StringType(uri));
            // Headers.
            getHeadersForReport(headers).ifPresent(requestHeadersItem -> requestItem.getItems().put(REPORT_ITEM_HEADERS, requestHeadersItem));
            // Message.
            requestItem.getItems().put(REPORT_ITEM_ENVELOPE, getSoapEnvelope(soapMessage));
            // Body
            getSoapBody(soapMessage).ifPresent(body -> requestItem.getItems().put(REPORT_ITEM_BODY, body));
            // Attachments.
            attachments.ifPresent(parts -> {
                var attachmentsItem = new MapType();
                parts.forEach(part -> attachmentsItem.addItem(part.name(), part.content()));
                requestItem.addItem(REPORT_ITEM_ATTACHMENTS, attachmentsItem);
            });
            /*
             * Create report - response
             */
            var responseItem = new MapType();
            messageForReport.addInput("response", responseItem);
            // Response status.
            responseItem.getItems().put(REPORT_ITEM_STATUS, new StringType(String.valueOf(response.statusCode())));
            // Response headers.
            getHeadersForReport(response.headers().map()).ifPresent(responseHeadersItem -> responseItem.getItems().put(REPORT_ITEM_HEADERS, responseHeadersItem));
            // Message
            boolean errorRaised = false;
            try {
                // First extract all SOAP-related data.
                String contentTypeHeader = String.join(", ", response.headers().allValues(CONTENT_TYPE_HEADER));
                SOAPMessage soapResponse = MessagingHandlerUtils.deserialiseSoapMessage(
                        SoapVersion.forContentTypeHeader(contentTypeHeader),
                        response.body(),
                        Optional.of(contentTypeHeader)
                );
                // Response envelope.
                ObjectType envelopeItem = getSoapEnvelope(soapResponse);
                // Response body.
                Optional<ObjectType> bodyItem = getSoapBody(soapResponse);
                // Response attachments.
                Optional<MapType> attachmentsItem = getAttachments(soapResponse);
                // Only if there wasn't any failure, add to report.
                responseItem.addItem(REPORT_ITEM_ENVELOPE, envelopeItem);
                bodyItem.ifPresent(body -> responseItem.addItem(REPORT_ITEM_BODY, body));
                attachmentsItem.ifPresent(item -> responseItem.addItem(REPORT_ITEM_ATTACHMENTS, item));
            } catch (Exception e) {
                if (tolerateNonSoapResponse) {
                    LOG.info(MarkerFactory.getDetachedMarker(sessionId), "Ignored error processing SOAP response: {}", e.getMessage());
                } else {
                    LOG.error(MarkerFactory.getDetachedMarker(sessionId), "Error while processing SOAP response: {}", e.getMessage(), e);
                }
                // Error extracting SOAP. Extract simply the body.
                getResponseBody(response).ifPresent(body -> responseItem.addItem(REPORT_ITEM_ERROR, body));
                errorRaised = true;
            }
            if (!errorRaised || tolerateNonSoapResponse) {
                return MessagingHandlerUtils.generateSuccessReport(messageForReport);
            } else {
                return MessagingHandlerUtils.generateErrorReport(messageForReport);
            }
        });
        return new DeferredMessagingReport(asyncReport);
    }

    @Override
    public MessagingReport receiveMessage(String sessionId, String transactionId, String callId, MessagingStep step, Message inputs, List<Thread> messagingThreads) {
        // Validate status.
        getStatus(inputs.getFragments(), STATUS_ARGUMENT_NAME, () -> HttpStatus.OK);
        // Validate SOAP version.
        getSoapVersion(inputs.getFragments(), VERSION_ARGUMENT_NAME, () -> SoapVersion.VERSION_1_1);
        Optional.ofNullable(getAndConvert(inputs.getFragments(), VERSION_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class))
                .ifPresent(data -> SoapVersion.forInput(data.toString()));
        // Validate envelope.
        getSoapEnvelope(inputs.getFragments(), ENVELOPE_ARGUMENT_NAME);
        // Log expected endpoint call.
        LOG.info(MarkerFactory.getDetachedMarker(sessionId), "Waiting to receive SOAP message at [{}]{}",
                getReceptionEndpoint(sessionId, API_PATH, inputs, URI_EXTENSION_ARGUMENT_NAME),
                Optional.ofNullable(StringUtils.trimToNull(step.getDesc())).map(" for step [%s]"::formatted).orElse("")
        );
        return new DeferredMessagingReport(new CallbackData(inputs, CallbackType.SOAP));
    }

    @Override
    public void endSession(String sessionId) {
        CallbackManager.getInstance().sessionEnded(sessionId);
    }

}
