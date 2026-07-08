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

package com.gitb.engine.messaging.handlers.layer.application.http;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.engine.messaging.handlers.ServerUtils;
import com.gitb.engine.messaging.handlers.layer.AbstractTransactionSender;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.messaging.Message;
import com.gitb.types.*;
import com.gitb.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by serbay on 9/23/14.
 */
public class HttpSender extends AbstractTransactionSender {
    private static final Logger logger = LoggerFactory.getLogger(HttpSender.class);

    private static final int DEFAULT_STATUS_CODE = 200;

    /** Default HTTP Connection object, shared with a peer {@link HttpReceiver} acting on the same transaction. */
    private SimpleHttpConnection connection;

    public HttpSender(SessionContext session, TransactionContext transaction) {
        super(session, transaction);
    }

    @Override
    public Message send(List<Configuration> configurations, Message message) throws Exception {
        //use the socket retrieved from the transaction
        Socket socket = getSocket();

        //secure this socket if it is not SSL secured
        if (transaction.getParameter(SSLContext.class) != null) {
            if(!(socket instanceof SSLSocket)) { //no need to create if we already have one
                SSLContext sslContext = transaction.getParameter(SSLContext.class);

                ActorConfiguration actorConfiguration = transaction.getWith();
                Configuration ipAddressConfig = Objects.requireNonNull(ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.IP_ADDRESS_CONFIG_NAME));
                Configuration portConfig = Objects.requireNonNull(ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.PORT_CONFIG_NAME));

                SocketFactory sf = sslContext.getSocketFactory();
                socket = sf.createSocket(InetAddress.getByName(Objects.requireNonNull(ipAddressConfig).getValue()),
                        Integer.parseInt(Objects.requireNonNull(portConfig).getValue()));

                transaction.setParameter(Socket.class, socket);
            }
        }

        //this ensures that a socket is created and saved into the transaction
        super.send(configurations, message);

        //use the connection retrieved from the transaction
        connection = transaction.getParameter(SimpleHttpConnection.class);

        //if the connection is null, that means transaction has just begun, so create new (as the client side, since we are sending first)
        if (connection == null) {
            connection = new SimpleHttpConnection(getSocket(), SimpleHttpConnection.Role.CLIENT);
            transaction.setParameter(SimpleHttpConnection.class, connection);
        }

        //connection was created as a client connection (by this class) and will send HTTP requests
        if (connection.role() == SimpleHttpConnection.Role.CLIENT) {
            sendHttpRequest(configurations, message);
        }

        //connection was created as a server connection (by the peer HttpReceiver) and will send an HTTP response
        if (connection.role() == SimpleHttpConnection.Role.SERVER) {
            sendHttpResponse(configurations, message);
        }

        return message;
    }

    private void sendHttpRequest(List<Configuration> configurations, Message message) throws Exception {
        logger.debug(addMarker(), "Connection created: {}", connection);

        Http11Request request = createHttpRequest(configurations, message);

        Http11Wire.writeRequestLine(connection.outputStream(), request.method(), request.path());
        Http11Wire.writeHeaders(connection.outputStream(), request.headers());
        connection.flush();
        logger.debug(addMarker(), "Sent request: {} {}", request.method(), request.path());

        Http11Wire.writeBody(connection.outputStream(), request.body());
        connection.flush();
        logger.debug(addMarker(), "Sent entity: {} bytes", request.body() == null ? 0 : request.body().length);
    }

    private void sendHttpResponse(List<Configuration> configurations, Message message) throws Exception {
        Http11Response response = createHttpResponse(configurations, message);

        Http11Wire.writeStatusLine(connection.outputStream(), response.statusCode(), response.reasonPhrase());
        Http11Wire.writeHeaders(connection.outputStream(), response.headers());
        connection.flush();
        logger.debug(addMarker(), "Sent response: {}", response.statusCode());

        Http11Wire.writeBody(connection.outputStream(), response.body());
        connection.flush();
        logger.debug(addMarker(), "Sent response entity: {} bytes", response.body() == null ? 0 : response.body().length);
    }

    protected Http11Request createHttpRequest(List<Configuration> configurations, Message message) {
        String method = getHttpMethod(configurations, message);
        String path = getHttpPath(configurations, message);
        Map<String, String> customHeaders = getHttpHeaders(message);

        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        byte[] body = buildBody(message, headers);

        headers.putAll(customHeaders);

        return new Http11Request(method, path, headers, body);
    }

    protected Http11Response createHttpResponse(List<Configuration> configurations, Message message) {
        Http11Request request = createHttpRequest(configurations, message);

        int statusCode;
        Configuration statusCodeConfig = ConfigurationUtils.getConfiguration(configurations, HttpMessagingHandler.HTTP_STATUS_CODE_CONFIG_NAME);
        if (statusCodeConfig == null) { //send default response status code
            statusCode = DEFAULT_STATUS_CODE;
        } else { //send status code provided as configuration
            statusCode = Integer.parseInt(statusCodeConfig.getValue());
        }

        return new Http11Response(statusCode, Http11Wire.reasonPhrase(statusCode), request.headers(), request.body());
    }

    /**
     * Builds the request/response body, populating the {@code Content-Length} and {@code Host} headers to match
     * (both for a raw body and for a multipart body). Mirrors the previous entity-construction logic, including
     * the fact that no headers or body are set at all when the {@code http_parts} input is present but malformed.
     */
    private byte[] buildBody(Message message, LinkedHashMap<String, String> headers) {
        byte[] messageContent = getHttpBody(message);
        if (messageContent != null) {
            headers.put("Content-Length", String.valueOf(messageContent.length));
            headers.put("Host", getHost() + ":" + getPort());
            return messageContent;
        }

        ListType partInput = (ListType) message.getFragments().get(HttpMessagingHandler.HTTP_PARTS_FIELD_NAME);
        if (partInput != null) {
            // Send the request as a multipart request.
            if (!partInput.isEmpty() && "map".equals(partInput.getContainedType())) {
                @SuppressWarnings("unchecked")
                List<MapType> parts = (List<MapType>) partInput.getValue();
                MultipartBody multipart = buildMultipartBody(parts);
                headers.put("Content-Type", multipart.contentType());
                headers.put("Content-Length", String.valueOf(multipart.body().length));
                headers.put("Host", getHost() + ":" + getPort());
                return multipart.body();
            } else {
                logger.warn(addMarker(), "Input for " + HttpMessagingHandler.HTTP_PARTS_FIELD_NAME + " must contain map items");
                return null;
            }
        } else {
            headers.put("Content-Length", "0");
            headers.put("Host", getHost() + ":" + getPort());
            return new byte[0];
        }
    }

    private MultipartBody buildMultipartBody(List<MapType> parts) {
        String boundary = "----------------------------" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (MapType partInfo: parts) {
            String name = (String)(partInfo.getItem("name")).getValue();
            StringType fileName = (StringType)partInfo.getItem("file_name");
            DataType content = partInfo.getItem("content");
            writeAscii(out, "--" + boundary + Http11Wire.CRLF);
            if (fileName == null) {
                // Text part.
                if (!(content instanceof StringType)) {
                    content = content.convertTo(DataType.STRING_DATA_TYPE);
                }
                writeAscii(out, "Content-Disposition: form-data; name=\"" + name + "\"" + Http11Wire.CRLF);
                writeAscii(out, "Content-Type: text/plain; charset=ISO-8859-1" + Http11Wire.CRLF);
                writeAscii(out, Http11Wire.CRLF);
                writeAscii(out, (String) content.getValue());
            } else {
                // Binary/File part.
                String fileNameValue = fileName.getValue();
                String contentType = (String)(partInfo.getItem("content_type")).getValue();
                if (!(content instanceof BinaryType)) {
                    content = content.convertTo(DataType.BINARY_DATA_TYPE);
                }
                writeAscii(out, "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileNameValue + "\"" + Http11Wire.CRLF);
                writeAscii(out, "Content-Type: " + contentType + Http11Wire.CRLF);
                writeAscii(out, Http11Wire.CRLF);
                out.writeBytes((byte[]) content.getValue());
            }
            writeAscii(out, Http11Wire.CRLF);
        }
        writeAscii(out, "--" + boundary + "--" + Http11Wire.CRLF);
        return new MultipartBody("multipart/form-data; boundary=" + boundary, out.toByteArray());
    }

    private static void writeAscii(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    protected byte[] getHttpBody(Message message) {
        BinaryType data = (BinaryType) message.getFragments().get(HttpMessagingHandler.HTTP_BODY_FIELD_NAME);

        if (data != null) {
            return data.getValue();
        }

        return null;
    }

    protected Map<String, String> getHttpHeaders(Message message) {
        Map<String, String> headers = new HashMap<>();

        MapType data = (MapType) message.getFragments().get(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME);

        if (data != null) {
            Map<String, DataType> elements = (Map<String, DataType>) data.getValue();

            for (Map.Entry<String, DataType> entry : elements.entrySet()) {
                String name = entry.getKey();
                StringType value = (StringType) entry.getValue();
                headers.put(name, value.getValue());
            }
        }

        return headers;
    }

    protected String getHttpMethod(List<Configuration> configurations, Message message) {
        Configuration methodConfig = ConfigurationUtils.getConfiguration(configurations, HttpMessagingHandler.HTTP_METHOD_CONFIG_NAME);

        if (methodConfig != null) {
            return methodConfig.getValue();
        }

        return null;
    }

    protected String getHttpPath(List<Configuration> configurations, Message message) {
        Configuration httpPathConfig;

        httpPathConfig = ConfigurationUtils.getConfiguration(configurations, HttpMessagingHandler.HTTP_URI_CONFIG_NAME);
        if (httpPathConfig == null) {
            httpPathConfig = ConfigurationUtils.getConfiguration(transaction.getWith().getConfig(), HttpMessagingHandler.HTTP_URI_CONFIG_NAME);
        }
        Configuration httpPathExtensionConfig = ConfigurationUtils.getConfiguration(configurations, HttpMessagingHandler.HTTP_URI_EXTENSION_CONFIG_NAME);

        return getPath(httpPathConfig, httpPathExtensionConfig);
    }

    private static String getPath(Configuration httpPathConfig, Configuration httpPathExtensionConfig) {
        String servicePath = "";
        if (httpPathConfig != null) {
            servicePath = httpPathConfig.getValue();
        }
        String uriExtension = "";
        if (httpPathExtensionConfig != null) {
            uriExtension = httpPathExtensionConfig.getValue();
        }

        if(!servicePath.startsWith("/") && !servicePath.contentEquals("")) {
            servicePath = "/" + servicePath;
        }

        if (servicePath.endsWith("/")) {
            servicePath = servicePath.substring(0, servicePath.length() - 1);
        }

        String path = servicePath;

        if(!uriExtension.contentEquals("")) {
            path = path + "/" + uriExtension;
        }
        return path;
    }

    protected String getHost() {
        ActorConfiguration actorConfiguration = transaction.getWith();
        Configuration host = Objects.requireNonNull(ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.IP_ADDRESS_CONFIG_NAME));
        return Objects.requireNonNull(host).getValue();
    }

    protected String getPort() {
        ActorConfiguration actorConfiguration = transaction.getWith();
        Configuration port = Objects.requireNonNull(ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.PORT_CONFIG_NAME));
        return Objects.requireNonNull(port).getValue();
    }

    /** Minimal request representation, replacing the previous {@code BasicHttpEntityEnclosingRequest}. */
    protected record Http11Request(String method, String path, LinkedHashMap<String, String> headers, byte[] body) {
    }

    /** Minimal response representation, replacing the previous {@code BasicHttpResponse}. */
    protected record Http11Response(int statusCode, String reasonPhrase, LinkedHashMap<String, String> headers, byte[] body) {
    }

    private record MultipartBody(String contentType, byte[] body) {
    }
}
