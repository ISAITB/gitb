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

package com.gitb.engine.messaging.handlers.layer.application.http.proxy;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpMessagingHandler;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpSender;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.types.BinaryType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;

/**
 * HTTP proxy implementation that proxies (currently) GET and POST requests to a configured server.
 * The proxy also exposes all request and response information so that it can be subsequently
 * validated.
 * <p>
 * Created by simatosc on 26/04/2016.
 */
public class HttpProxySender extends HttpSender {

    private static final Logger logger = LoggerFactory.getLogger(HttpProxySender.class);

    public HttpProxySender(SessionContext session, TransactionContext transaction) {
        super(session, transaction);
    }

    @Override
    public Message send(List<Configuration> configurations, Message message) throws Exception {
        // Send request to proxied server.
        sendMessageToServer(configurations, message);
        // Return response.
        super.send(configurations, message);
        return message;
    }

    private void sendMessageToServer(List<Configuration> configurations, Message message) throws Exception {
        Configuration receiverConfiguration = ConfigurationUtils.getConfiguration(configurations, HttpProxyMessagingHandler.PROXY_ADDRESS_CONFIG_NAME);
        String receiverAddress = Objects.requireNonNull(receiverConfiguration).getValue();

        MapType requestData = (MapType) message.getFragments().get(HttpProxyMessagingHandler.HTTP_REQUEST_DATA);
        String httpMethod = requestData.getItem(HttpProxyMessagingHandler.HTTP_METHOD_FIELD_NAME).getValue().toString();
        String requestPath = requestData.getItem(HttpProxyMessagingHandler.HTTP_PATH_FIELD_NAME).getValue().toString();
        if (requestPath != null) {
            if (!receiverAddress.endsWith("/")) {
                receiverAddress += "/";
            }
            if (requestPath.startsWith("/")) {
                requestPath = requestPath.substring(1);
            }
            receiverAddress += requestPath;
        }
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(receiverAddress));
        if ("POST".equals(httpMethod)) {
            BinaryType body = (BinaryType) requestData.getItem(HttpProxyMessagingHandler.HTTP_BODY_FIELD_NAME);
            byte[] bodyBytes = body != null ? body.getValue() : new byte[0];
            requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
        } else if ("GET".equals(httpMethod)) {
            requestBuilder.GET();
        }
        HttpResponse<byte[]> response;
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (Exception e) {
            logger.error(addMarker(), "Error sending message to proxied receiver", e);
            throw e;
        }
        MapType headers = new MapType();
        response.headers().map().forEach((name, values) ->
                values.forEach(value -> headers.addItem(name, new StringType(value)))
        );
        BinaryType responseMessageContent = new BinaryType();
        responseMessageContent.setValue(response.body());
        configurations.add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_METHOD_CONFIG_NAME, httpMethod));
        message.getFragments().put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, responseMessageContent);
        message.getFragments().put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, headers);

        MapType responseData = new MapType();
        responseData.addItem("http_headers", headers);
        responseData.addItem("http_body", responseMessageContent);
        requestData.addItem("response_data", responseData);
    }

}
