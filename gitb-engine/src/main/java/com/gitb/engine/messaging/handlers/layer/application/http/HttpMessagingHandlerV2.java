package com.gitb.engine.messaging.handlers.layer.application.http;

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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.*;

@MessagingHandler(name="HttpMessagingV2")
public class HttpMessagingHandlerV2 extends AbstractNonWorkerMessagingHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpMessagingHandlerV2.class);

    public final static String METHOD_ARGUMENT_NAME = "method";
    public final static String STATUS_ARGUMENT_NAME = "status";
    public final static String HEADERS_ARGUMENT_NAME = "headers";
    public final static String BODY_ARGUMENT_NAME = "body";
    public final static String URI_EXTENSION_ARGUMENT_NAME = "uriExtension";
    private final static String URI_ARGUMENT_NAME = "uri";
    private final static String PARAMETERS_ARGUMENT_NAME = "parameters";
    private final static String QUERY_PARAMETERS_ARGUMENT_NAME = "queryParameters";
    private final static String PARTS_ARGUMENT_NAME = "parts";
    private final static String FOLLOW_REDIRECTS_ARGUMENT_NAME = "followRedirects";
    private final static String CONNECTION_TIMEOUT_ARGUMENT_NAME = "connectionTimeout";
    private final static String REQUEST_TIMEOUT_ARGUMENT_NAME = "requestTimeout";

    public static final String REPORT_ITEM_REQUEST = "request";
    public static final String REPORT_ITEM_RESPONSE = "response";
    public static final String REPORT_ITEM_URI = "uri";
    public static final String REPORT_ITEM_HEADERS = "headers";
    public static final String REPORT_ITEM_METHOD = "method";
    public static final String REPORT_ITEM_STATUS = "status";
    public static final String REPORT_ITEM_BODY = "body";

    public static final String API_PATH = "http";

    @Override
    public MessagingReport sendMessage(String sessionId, String transactionId, String stepId, List<Configuration> configurations, Message message) {
        // URI to send the request to.
        var uri = Objects.requireNonNull(getAndConvert(message.getFragments(), URI_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class), "Input [%s] must be provided".formatted(URI_ARGUMENT_NAME)).toString();
        // The map of parameters to use.
        var parameters = getMapOfValues(message.getFragments(), PARAMETERS_ARGUMENT_NAME);
        // The map of parameters to use explicitly on the query string.
        var queryParameters = getMapOfValues(message.getFragments(), QUERY_PARAMETERS_ARGUMENT_NAME);
        // The request body.
        var body = Optional.ofNullable(message.getFragments().get(BODY_ARGUMENT_NAME));
        // Follow redirects.
        var followRedirects = Optional.ofNullable(getAndConvert(message.getFragments(), FOLLOW_REDIRECTS_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class))
                .map(value -> (Boolean) value.getValue())
                .orElse(Boolean.TRUE);
        // Multipart form parts.
        var parts = Optional.ofNullable(getAndConvert(message.getFragments(), PARTS_ARGUMENT_NAME, DataType.LIST_DATA_TYPE, ListType.class));
        // The HTTP method to use (GET being the default).
        var method = getMethod(message.getFragments(), METHOD_ARGUMENT_NAME).orElseGet(() -> {
            if (body.isPresent() || parts.isPresent()) {
                return HttpMethod.POST;
            } else {
                return HttpMethod.GET;
            }
        });
        // The HTTP headers.
        var headers = getMapOfValues(message.getFragments(), HEADERS_ARGUMENT_NAME);
        // The connection timeout.
        var connectionTimeout = Optional.ofNullable(getAndConvert(message.getFragments(), CONNECTION_TIMEOUT_ARGUMENT_NAME, DataType.NUMBER_DATA_TYPE, NumberType.class))
                .map(NumberType::longValue)
                .filter(value -> value > 0)
                .orElse(10000L);
        // The request timeout.
        var requestTimeout = Optional.ofNullable(getAndConvert(message.getFragments(), REQUEST_TIMEOUT_ARGUMENT_NAME, DataType.NUMBER_DATA_TYPE, NumberType.class))
                .map(NumberType::longValue)
                .filter(value -> value > 0);
        // Create request.
        final var builder = HttpRequest.newBuilder();
        String uriToUse;
        HttpRequest.BodyPublisher bodyPublisher;
        DataType requestBodyItem = null;
        if (HttpMethod.GET.equals(method) || HttpMethod.DELETE.equals(method) || HttpMethod.HEAD.equals(method) || HttpMethod.OPTIONS.equals(method) || HttpMethod.TRACE.equals(method)) {
            uriToUse = getUriToUse(uri, mergeMapsOfValues(parameters, queryParameters));
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        } else if (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.PATCH.equals(method)) {
            if (body.isPresent() || parts.isPresent()) {
                if (parameters.isEmpty() && queryParameters.isEmpty()) {
                    uriToUse = uri;
                } else {
                    uriToUse = getUriToUse(uri, mergeMapsOfValues(parameters, queryParameters));
                }
                if (body.isPresent()) {
                    if (body.get() instanceof BinaryType binaryType) {
                        requestBodyItem = binaryType;
                        bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(binaryType.serializeByDefaultEncoding());
                    } else {
                        requestBodyItem = body.get().convertTo(DataType.STRING_DATA_TYPE);
                        bodyPublisher = HttpRequest.BodyPublishers.ofString(requestBodyItem.toString());
                    }
                } else {
                    if (parts.get().isEmpty() || !DataType.MAP_DATA_TYPE.equals(parts.get().getContainedType())) {
                        throw new IllegalArgumentException("The [%s] input for multipart requests must contain map elements.".formatted(PARTS_ARGUMENT_NAME));
                    }
                    MapType partsType = new MapType();
                    MultipartFormDataBodyPublisher partPublisher = new MultipartFormDataBodyPublisher();
                    parts.get().getElements()
                            .forEach(element -> {
                                if (element instanceof MapType mapElement) {
                                    var name = Objects.requireNonNull(mapElement.getItem("name"), "The [name] must be provided for each element of the [%s] input for multipart requests.".formatted(PARTS_ARGUMENT_NAME))
                                            .convertTo(DataType.STRING_DATA_TYPE).toString();
                                    var content = Objects.requireNonNull(mapElement.getItem("content"), "The [content] must be provided for each element of the [%s] input for multipart requests.".formatted(PARTS_ARGUMENT_NAME));
                                    var fileName = Optional.ofNullable(mapElement.getItem("fileName"))
                                            .map(input -> input.convertTo(DataType.STRING_DATA_TYPE).toString());
                                    if (fileName.isEmpty()) {
                                        // Text part.
                                        StringType partType = (StringType) content.convertTo(DataType.STRING_DATA_TYPE);
                                        Optional.ofNullable(mapElement.getItem("contentType"))
                                                .map(input -> input.convertTo(DataType.STRING_DATA_TYPE).toString())
                                                .ifPresent(partType::setContentType);
                                        partsType.addItem(name, partType);
                                        partPublisher.add(name, partType.toString());
                                    } else {
                                        // Binary/File part.
                                        var contentDataType = (BinaryType) content.convertTo(DataType.BINARY_DATA_TYPE);
                                        var contentBytes = contentDataType.serializeByDefaultEncoding();
                                        var contentType = Optional.ofNullable(mapElement.getItem("contentType"))
                                                .map(input -> input.convertTo(DataType.STRING_DATA_TYPE).toString())
                                                .orElse("application/octet-stream");
                                        contentDataType.setContentType(contentType);
                                        partPublisher.addStream(name, fileName.get(), () -> new ByteArrayInputStream(contentBytes), contentType);
                                        partsType.addItem(name, contentDataType);
                                    }
                                }
                            });
                    requestBodyItem = partsType;
                    bodyPublisher = partPublisher;
                    headers.putIfAbsent(CONTENT_TYPE_HEADER, List.of(partPublisher.contentType()));
                }
            } else {
                uriToUse = getUriToUse(uri, queryParameters);
                if (!parameters.isEmpty()) {
                    headers.putIfAbsent(CONTENT_TYPE_HEADER, List.of("application/x-www-form-urlencoded"));
                    String encodedParameters = encodeParameters(parameters);
                    bodyPublisher = HttpRequest.BodyPublishers.ofString(encodedParameters);
                    requestBodyItem = new StringType(encodedParameters);
                } else {
                    bodyPublisher = HttpRequest.BodyPublishers.noBody();
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method [%s].".formatted(method));
        }
        headers.forEach((key, value) -> value.forEach(headerValue -> builder.header(key, headerValue)));
        if (headers.containsKey(CONTENT_TYPE_HEADER) && requestBodyItem != null) {
            headers.get(CONTENT_TYPE_HEADER).stream()
                    .findFirst()
                    .flatMap(contentType -> Arrays.stream(StringUtils.split(contentType, ';')).findFirst())
                    .ifPresent(requestBodyItem::setContentType);
        }
        builder.uri(URI.create(uriToUse))
                .method(method.name(), bodyPublisher);
        // Request timeout.
        requestTimeout.ifPresent(value -> builder.timeout(Duration.ofMillis(value)));
        DataType finalRequestBodyItem = requestBodyItem;
        // Make request.
        CompletableFuture<HttpResponse<byte[]>> asyncResponse;
        try {
            asyncResponse = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(connectionTimeout))
                    .followRedirects(followRedirects?HttpClient.Redirect.ALWAYS:HttpClient.Redirect.NEVER)
                    .build()
                    .sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Error while contacting remote system", e);
        }
        CompletableFuture<MessagingReport> asyncReport = asyncResponse.thenApply((response) -> {
            // Create report - request
            var messageForReport = new Message();
            var requestItem = new MapType();
            // Method
            requestItem.getItems().put(REPORT_ITEM_METHOD, new StringType(method.name()));
            // URI
            requestItem.getItems().put(REPORT_ITEM_URI, new StringType(uriToUse));
            // Headers
            getHeadersForReport(headers).ifPresent(requestHeadersItem -> requestItem.getItems().put(REPORT_ITEM_HEADERS, requestHeadersItem));
            // Body
            if (finalRequestBodyItem != null) {
                requestItem.getItems().put(REPORT_ITEM_BODY, finalRequestBodyItem);
            }
            messageForReport.getFragments().put(REPORT_ITEM_REQUEST, requestItem);
            // Create report - response
            var responseItem = new MapType();
            // Status
            responseItem.getItems().put(REPORT_ITEM_STATUS, new StringType(String.valueOf(response.statusCode())));
            // Headers
            getHeadersForReport(response.headers().map()).ifPresent(responseHeadersItem -> responseItem.getItems().put(REPORT_ITEM_HEADERS, responseHeadersItem));
            // Body
            getResponseBody(response).ifPresent(item -> responseItem.addItem(REPORT_ITEM_BODY, item));
            messageForReport.getFragments().put(REPORT_ITEM_RESPONSE, responseItem);
            return MessagingHandlerUtils.generateSuccessReport(messageForReport);
        });
        return new DeferredMessagingReport(asyncReport);
    }

    @Override
    public MessagingReport receiveMessage(String sessionId, String transactionId, String callId, MessagingStep step, Message inputs, List<Thread> messagingThreads) {
        /*
         * Extract here the important inputs to have a chance of failing quick and reporting issues directly.
         */
        // Method.
        Optional<HttpMethod> method = getMethod(inputs.getFragments(), METHOD_ARGUMENT_NAME);
        // Status.
        getStatus(inputs.getFragments(), STATUS_ARGUMENT_NAME, () -> HttpStatus.OK);
        // Log expected endpoint call.
        LOG.info(MarkerFactory.getDetachedMarker(sessionId), "Waiting to receive {} at [{}]{}",
                method.map(HttpMethod::name).orElse("any call"),
                getReceptionEndpoint(sessionId, API_PATH, inputs, URI_EXTENSION_ARGUMENT_NAME),
                Optional.ofNullable(StringUtils.trimToNull(step.getDesc())).map(" for step [%s]"::formatted).orElse("")
        );
        return new DeferredMessagingReport(new CallbackData(inputs, CallbackType.HTTP));
    }

    @Override
    public void endSession(String sessionId) {
        CallbackManager.getInstance().sessionEnded(sessionId);
    }

    private Map<String, List<String>> mergeMapsOfValues(Map<String, List<String>> map1, Map<String, List<String>> map2) {
        Map<String, List<String>> result = new HashMap<>(map1);
        map2.forEach((key, values) -> result.merge(key, values, (currentValues, newValues) -> {
           var uniqueValues = new LinkedHashSet<>(currentValues);
           uniqueValues.addAll(newValues);
           return uniqueValues.stream().toList();
        }));
        return result;
    }

    private String getUriToUse(String uri, Map<String, List<String>> parameters) {
        if (parameters.isEmpty()) {
            return uri;
        } else {
            String queryStringParameters = encodeParameters(parameters);
            if (uri.endsWith("?")) {
                return uri + queryStringParameters;
            } else if (uri.indexOf('?') != -1) {
                return StringUtils.appendIfMissing(uri, "&") + queryStringParameters;
            } else {
                return uri + "?" + queryStringParameters;
            }
        }
    }

    private String encodeParameters(Map<String, List<String>> parameters) {
        var strBuilder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                var parameterKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
                for (String parameterValue: entry.getValue()) {
                    if (!strBuilder.isEmpty()) {
                        strBuilder.append("&");
                    }
                    strBuilder.append(parameterKey);
                    strBuilder.append("=");
                    strBuilder.append(URLEncoder.encode(parameterValue, StandardCharsets.UTF_8));
                }
            }
        }
        return strBuilder.toString();
    }

}
