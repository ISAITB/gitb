package com.gitb.engine.messaging.handlers.layer.application.http;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.messaging.DeferredMessagingReport;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.ms.InitiateResponse;
import com.gitb.types.*;
import org.apache.commons.lang3.StringUtils;

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

@MessagingHandler(name="HttpMessagingV2")
public class HttpMessagingHandlerV2 extends AbstractMessagingHandler {

    private final static String URI_ARGUMENT_NAME = "uri";
    private final static String METHOD_ARGUMENT_NAME = "method";
    private final static String PARAMETERS_ARGUMENT_NAME = "parameters";
    private final static String QUERY_PARAMETERS_ARGUMENT_NAME = "queryParameters";
    private final static String HEADERS_ARGUMENT_NAME = "headers";
    private final static String BODY_ARGUMENT_NAME = "body";
    private final static String PARTS_ARGUMENT_NAME = "parts";
    private final static String FOLLOW_REDIRECTS_ARGUMENT_NAME = "followRedirects";

    private final static String CONTENT_TYPE = "Content-Type";

    @Override
    public MessagingModule getModuleDefinition() {
        return new MessagingModule();
    }

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
        var method = Optional.ofNullable(getAndConvert(message.getFragments(), METHOD_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class))
                .map(StringType::toString)
                .orElseGet(() -> {
                    if (body.isPresent() || parts.isPresent()) {
                        return "POST";
                    } else {
                        return "GET";
                    }
                })
                .toUpperCase();
        // The HTTP headers.
        var headers = getMapOfValues(message.getFragments(), HEADERS_ARGUMENT_NAME);
        // Create request.
        final var builder = HttpRequest.newBuilder();
        String uriToUse;
        HttpRequest.BodyPublisher bodyPublisher;
        DataType requestBodyItem = null;
        if ("GET".equals(method) || "DELETE".equals(method)) {
            uriToUse = getUriToUse(uri, mergeMapsOfValues(parameters, queryParameters));
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        } else if ("POST".equals(method) || "PUT".equals(method)) {
            if (body.isPresent() || parts.isPresent()) {
                if (parameters.isEmpty()) {
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
                    headers.putIfAbsent(CONTENT_TYPE, List.of(partPublisher.contentType()));
                }
            } else {
                uriToUse = getUriToUse(uri, queryParameters);
                if (!parameters.isEmpty()) {
                    headers.putIfAbsent(CONTENT_TYPE, List.of("application/x-www-form-urlencoded"));
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
        var requestHeadersItem = new MapType();
        headers.forEach((key, value) -> {
            value.forEach(headerValue -> builder.header(key, headerValue));
            requestHeadersItem.addItem(key, new StringType(String.join(", ", value)));
        });
        if (headers.containsKey(CONTENT_TYPE) && requestBodyItem != null) {
            Optional<String> contentTypeToSet = headers.get(CONTENT_TYPE).stream()
                    .findFirst()
                    .flatMap(contentType -> Arrays.stream(StringUtils.split(contentType, ';')).findFirst());
            if (contentTypeToSet.isPresent()) {
                requestBodyItem.setContentType(contentTypeToSet.get());
            }
        }
        builder.uri(URI.create(uriToUse))
                .method(method, bodyPublisher);
        DataType finalRequestBodyItem = requestBodyItem;
        // Make request.
        CompletableFuture<HttpResponse<byte[]>> asyncResponse;
        try {
            asyncResponse = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
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
            requestItem.getItems().put("method", new StringType(method));
            // URI
            requestItem.getItems().put("uri", new StringType(uriToUse));
            // Headers
            if (!headers.isEmpty()) {
                requestItem.getItems().put("headers", requestHeadersItem);
            }
            // Body
            if (finalRequestBodyItem != null) {
                requestItem.getItems().put("body", finalRequestBodyItem);
            }
            messageForReport.getFragments().put("request", requestItem);
            // Create report - response
            var responseItem = new MapType();
            // Status
            responseItem.getItems().put("status", new StringType(String.valueOf(response.statusCode())));
            // Headers
            if (!response.headers().map().isEmpty()) {
                MapType responseHeadersItem = new MapType();
                response.headers().map().forEach((headerName, headerValues) -> {
                    responseHeadersItem.addItem(headerName, new StringType(String.join(", ", headerValues)));
                });
                responseItem.getItems().put("headers", responseHeadersItem);
            }
            // Body
            byte[] responseBody = response.body();
            if (responseBody != null && responseBody.length > 0) {
                var dataItem = new BinaryType(responseBody);
                response.headers()
                        .firstValue(CONTENT_TYPE)
                        .flatMap(contentType -> Arrays.stream(StringUtils.split(contentType, ';')).findFirst())
                        .ifPresent(contentType -> dataItem.setContentType(contentType.trim()));
                responseItem.addItem("body", dataItem);
            }
            messageForReport.getFragments().put("response", responseItem);
            return MessagingHandlerUtils.generateSuccessReport(messageForReport);
        });
        return new DeferredMessagingReport(asyncReport);
    }

    @Override
    public MessagingReport receiveMessage(String sessionId, String transactionId, String callId, String stepId, List<Configuration> configurations, Message inputs, List<Thread> messagingThreads) {
        throw new IllegalStateException("The 'receive' step is not currently supported for the HttpMessagingV2 handler");
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

    private Map<String, List<String>> getMapOfValues(Map<String, DataType> inputs, String argumentName) {
        return Optional.ofNullable(getAndConvert(inputs, argumentName, DataType.MAP_DATA_TYPE, MapType.class))
                .map(inputMap -> {
                    Map<String, List<String>> headerMap = new HashMap<>();
                    inputMap.getItems().forEach((key, value) -> {
                        Objects.requireNonNull(key, "A value was provided in the [%s] map that did not have a name.".formatted(argumentName));
                        var headersForKey = headerMap.computeIfAbsent(key, k -> new ArrayList<>());
                        if (value instanceof MapType mapType) {
                            headersForKey.addAll(mapType.getItems().values().stream().map(x -> x.convertTo(DataType.STRING_DATA_TYPE).toString()).toList());
                        } else if (value instanceof ListType listType) {
                            headersForKey.addAll(listType.getElements().stream().map(x -> x.convertTo(DataType.STRING_DATA_TYPE).toString()).toList());
                        } else {
                            headersForKey.add(value.convertTo(DataType.STRING_DATA_TYPE).toString());
                        }
                    });
                    return headerMap;
                }).orElse(new HashMap<>(0));
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

    @Override
    public void beginTransaction(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations) {
        // Do nothing.
    }

    @Override
    public void endTransaction(String sessionId, String transactionId, String stepId) {
        // Do nothing.
    }

    @Override
    public void endSession(String sessionId) {
        // Do nothing.
    }

    @Override
    public MessagingReport listenMessage(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations, Message inputs) {
        throw new IllegalStateException("The HttpMessagingV2 handler can only be used for send and receive operations");
    }

    @Override
    public boolean needsMessagingServerWorker() {
        return false;
    }

    @Override
    public InitiateResponse initiate(List<ActorConfiguration> actorConfigurations) {
        return new InitiateResponse();
    }

}
