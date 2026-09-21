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

import com.gitb.core.Configuration;
import com.gitb.engine.messaging.handlers.layer.AbstractTransactionReceiver;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.Message;
import com.gitb.types.BinaryType;
import com.gitb.types.ListType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by serbay on 9/23/14.
 */
public class HttpReceiver extends AbstractTransactionReceiver {

	private static final Logger logger = LoggerFactory.getLogger(HttpReceiver.class);

	private static final Pattern PART_NAME_PATTERN = Pattern.compile("(?i)\\bname\\s*=\\s*\"([^\"]*)\"");

    /** Default HTTP Connection object, shared with a peer {@link HttpSender} acting on the same transaction. */
    private SimpleHttpConnection connection;

	public HttpReceiver(SessionContext session, TransactionContext transaction) {
		super(session, transaction);
	}

	@Override
	public Message receive(List<Configuration> configurations, Message inputs) throws Exception {
        //use the socket retrieved from the transaction
        socket = getSocket();

        //if the socket is null, that means transaction has just begun, so create new
        //below code blocks until a socket is created
        if(socket == null){
            waitUntilMessageReceived();
        }

        //use the connection retrieved from the transaction
        connection = transaction.getParameter(SimpleHttpConnection.class);

        //if the connection is null, that means transaction has just begun, so create new (as the server side, since we are receiving first)
        if(connection == null) {
            connection = new SimpleHttpConnection(socket, SimpleHttpConnection.Role.SERVER);
            transaction.setParameter(SimpleHttpConnection.class, connection);
        }

        //connection was created as a server connection (by this class) and will receive HTTP requests
        if(connection.role() == SimpleHttpConnection.Role.SERVER) {
            return receiveHttpRequest(configurations);
        }

        //connection was created as a client connection (by the peer HttpSender) and will receive an HTTP response
        if(connection.role() == SimpleHttpConnection.Role.CLIENT) {
            return receiveHttpResponse(configurations);
        }

        //not likely to happen
        throw new GITBEngineInternalError("Unexpected HTTP connection type");
	}

    /**
     * Receives HTTP requests from clients
     * @param configurations Receiver configurations
     * @return Received HTTP request
     */
    private Message receiveHttpRequest(List<Configuration> configurations) throws Exception{
        logger.debug(addMarker(), "Message received: {}", socket);

        logger.debug(addMarker(), "Connection created: {}", connection);

        String requestLine = Http11Wire.readLine(connection.inputStream());
        if (requestLine == null) {
            throw new GITBEngineInternalError("Connection closed while waiting for an HTTP request");
        }
        String[] requestLineParts = parseRequestLine(requestLine);
        String method = requestLineParts[0];
        String path = requestLineParts[1];
        String protocolVersion = requestLineParts[2];
        LinkedHashMap<String, String> headers = Http11Wire.readHeaders(connection.inputStream());
        logger.debug(addMarker(), "Received request header: {} {} {}", method, path, protocolVersion);

        Message message = new Message();
        message.getFragments()
                .put(HttpMessagingHandler.HTTP_METHOD_FIELD_NAME, new StringType(method));
        message.getFragments()
                .put(HttpMessagingHandler.HTTP_PATH_FIELD_NAME, new StringType(path));
        message.getFragments()
                .put(HttpMessagingHandler.HTTP_PROTOCOL_VERSION_FIELD_NAME, new StringType(protocolVersion));
        MapType headersFragment = constructHttpHeadersFragment(headers);
        message.getFragments()
                .put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, headersFragment);

        // The sender always provides a Content-Length header (defaulting to 0), so the request always encloses a body.
        int contentLength = parseContentLength(headers);
        byte[] bodyBytes = Http11Wire.readBody(connection.inputStream(), contentLength);
        logger.debug(addMarker(), "Received request entity: {} bytes", bodyBytes.length);

        BinaryType httpBody = new BinaryType();
        httpBody.setValue(bodyBytes);
        message.getFragments().put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, httpBody);
        message.getFragments().put(HttpMessagingHandler.HTTP_PARTS_FIELD_NAME, getMultipartData(httpBody, headers));

        return message;
    }

    private MapType getMultipartData(BinaryType httpBody, Map<String, String> headers) {
        String boundary = extractBoundary(Http11Wire.getHeaderIgnoreCase(headers, "Content-Type"));
        MapType info = new MapType();
        int partCounter = 0;
        if (!StringUtils.isBlank(boundary)) {
            boundary = boundary.trim();
            ListType parts = new ListType("map");
            MapType partsByName = new MapType();
            try (ByteArrayInputStream bis = new ByteArrayInputStream(httpBody.getValue())) {
                MultipartStream multipartStream = new MultipartStream(bis, boundary.getBytes(), 4096, null);
                boolean nextPart = multipartStream.skipPreamble();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                while (nextPart) {
                    partCounter += 1;
                    String header = multipartStream.readHeaders();
                    multipartStream.readBodyData(bos);
                    // Record the part.
                    MapType partInfo = new MapType();
                    partInfo.addItem("header", new StringType(header));
                    BinaryType partContent = new BinaryType();
                    partContent.setValue(bos.toByteArray());
                    partInfo.addItem("content", partContent);
                    parts.append(partInfo);
                    // Map also based on the part's name.
                    String partName = getPartName(header);
                    ListType collectedParts = (ListType)partsByName.getItem(partName);
                    if (collectedParts == null) {
                        collectedParts = new ListType("map");
                        partsByName.addItem(partName, collectedParts);
                    }
                    collectedParts.append(partInfo);
                    // Read next part.
                    bos.reset();
                    nextPart = multipartStream.readBoundary();
                }
            } catch(Exception e) {
                logger.warn(addMarker(), "Error while parsing multipart contents", e);
            }
            info.addItem("parts", parts);
            info.addItem("partsByName", partsByName);
        }
        info.addItem("count", new StringType(String.valueOf(partCounter)));
        return info;
    }

    /** Extracts the {@code boundary} parameter from a {@code Content-Type} header value, e.g. {@code multipart/form-data; boundary=XYZ}. */
    private String extractBoundary(String contentTypeHeaderValue) {
        if (contentTypeHeaderValue == null) {
            return null;
        }
        for (String parameter : contentTypeHeaderValue.split(";")) {
            String trimmed = parameter.trim();
            if (trimmed.regionMatches(true, 0, "boundary=", 0, "boundary=".length())) {
                String value = trimmed.substring("boundary=".length()).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return null;
    }

    /**
     * Receives HTTP responses from servers
     * @param configurations Receiver configurations
     * @return Received HTTP response
     */
    private Message receiveHttpResponse(List<Configuration> configurations) throws Exception {
        logger.debug(addMarker(), "Message received: {}", socket);

        String statusLine = Http11Wire.readLine(connection.inputStream());
        if (statusLine == null) {
            throw new GITBEngineInternalError("Connection closed while waiting for an HTTP response");
        }
        int statusCode = parseStatusLine(statusLine);

        logger.debug(addMarker(), "Received response header: {}", statusCode);

        LinkedHashMap<String, String> headers = Http11Wire.readHeaders(connection.inputStream());

        int contentLength = parseContentLength(headers);
        byte[] bodyBytes = Http11Wire.readBody(connection.inputStream(), contentLength);
        logger.debug(addMarker(), "Received response entity: {} bytes", bodyBytes.length);

        //check retrieved status code
        checkStatusCode(configurations, statusCode);

        Message message = new Message();
        message
                .getFragments()
                .put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, constructHttpHeadersFragment(headers));
        message
                .getFragments()
                .put(HttpMessagingHandler.HTTP_STATUS_FIELD_NAME, new StringType(String.valueOf(statusCode)));
        if (statusCode != 204) { // SC_NO_CONTENT
            BinaryType httpBody = new BinaryType();
            httpBody.setValue(bodyBytes);
            message
                    .getFragments()
                    .put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, httpBody);
        }

        return message;
    }

	private MapType constructHttpHeadersFragment(Map<String, String> headers) {
		MapType headerMap = new MapType();
		for(Map.Entry<String, String> h : headers.entrySet()) {
			headerMap.addItem(h.getKey(), new StringType(h.getValue()));
		}
		return headerMap;
	}

	private String getPartName(String headers) {
        if (headers == null) {
            return null;
        }
        Matcher matcher = PART_NAME_PATTERN.matcher(headers);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String[] parseRequestLine(String line) throws GITBEngineInternalError {
        int firstSpace = line.indexOf(' ');
        int secondSpace = firstSpace < 0 ? -1 : line.indexOf(' ', firstSpace + 1);
        if (firstSpace < 0 || secondSpace < 0) {
            throw new GITBEngineInternalError("Malformed HTTP request line: " + line);
        }
        return new String[] {
                line.substring(0, firstSpace),
                line.substring(firstSpace + 1, secondSpace),
                line.substring(secondSpace + 1)
        };
    }

    private int parseStatusLine(String line) throws GITBEngineInternalError {
        int firstSpace = line.indexOf(' ');
        if (firstSpace < 0) {
            throw new GITBEngineInternalError("Malformed HTTP status line: " + line);
        }
        int secondSpace = line.indexOf(' ', firstSpace + 1);
        String codeString = secondSpace < 0 ? line.substring(firstSpace + 1) : line.substring(firstSpace + 1, secondSpace);
        try {
            return Integer.parseInt(codeString.trim());
        } catch (NumberFormatException e) {
            throw new GITBEngineInternalError("Malformed HTTP status line: " + line);
        }
    }

    private int parseContentLength(Map<String, String> headers) {
        String value = Http11Wire.getHeaderIgnoreCase(headers, "Content-Length");
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void checkStatusCode(List<Configuration> configurations, int statusCode) throws Exception {
        Configuration expectedStatusCode = ConfigurationUtils.getConfiguration(configurations, HttpMessagingHandler.HTTP_STATUS_CODE_CONFIG_NAME);
        if(expectedStatusCode != null) { //here we expect received response status code to match provided status code configuration
            int expected = Integer.parseInt(expectedStatusCode.getValue());
            if(statusCode != expected){
                throw new Exception("Expected status code: " + expectedStatusCode.getValue() + ", but received: " + statusCode);
            }
        }
    }
}
