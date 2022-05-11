package com.gitb.engine.messaging.handlers.layer.application.http;

import com.gitb.core.Configuration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.SecurityUtils;
import com.gitb.engine.messaging.handlers.layer.AbstractTransactionReceiver;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.types.BinaryType;
import com.gitb.types.ListType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.BHttpConnectionBase;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.message.BasicHeaderValueParser;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by serbay on 9/23/14.
 */
public class HttpReceiver extends AbstractTransactionReceiver {
	private Logger logger = LoggerFactory.getLogger(HttpReceiver.class);

	private static final int BUFFER_SIZE = 8*1024;
	private static HttpConnectionFactory<DefaultBHttpServerConnection> httpConnectionFactory;

	static {
		ConnectionConfig connectionConfig = ConnectionConfig
												.custom()
												.setBufferSize(BUFFER_SIZE)
												.setCharset(Charset.defaultCharset())
												.build();

		httpConnectionFactory = new DefaultBHttpServerConnectionFactory(connectionConfig);
	}

    /** Default HTTP Connection object */
    protected BHttpConnectionBase connection;

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

        if (transaction.getParameter(SSLContext.class) != null) {
            //secure this socket if it is not SSL secured
            if(!(socket instanceof SSLSocket)) {//no need to create if we already have one
                socket = SecurityUtils.secureSocket(transaction, socket);
                ((SSLSocket) socket).setUseClientMode(false); //do not use client mode for handshaking since it is a server socket
            }
        }

        //use the connection retrieved from the transaction
        connection = transaction.getParameter(BHttpConnectionBase.class);

        //if the connection is null, that means transaction has just begun, so create new
        if(connection == null) {
            connection = httpConnectionFactory.createConnection(socket);
            transaction.setParameter(BHttpConnectionBase.class, connection);
        }

        //connection is a server connection and will receive HTTP requests
        if(connection instanceof DefaultBHttpServerConnection) {
            return receiveHttpRequest(configurations);
        }

        //connection has sent an HTTP request and will receive HTTP response
        if(connection instanceof DefaultBHttpClientConnection) {
            return receiveHttpResponse(configurations);
        }

        //not likely to happen
        throw new GITBEngineInternalError("Unexpected HTTP connection type");
	}

    /**
     * Receives HTTP requests from clients
     * @param configurations Receiver configurations
     * @return Received HTTP request
     * @throws Exception
     */
    private Message receiveHttpRequest(List<Configuration> configurations) throws Exception{
        logger.debug(addMarker(), "Message received: " + socket);

        logger.debug(addMarker(), "Connection created: " + connection);

        HttpRequest request = ((DefaultBHttpServerConnection) connection).receiveRequestHeader();
        logger.debug(addMarker(), "Received request header: " + request);

        Message message = new Message();
        message.getFragments()
                .put(HttpMessagingHandler.HTTP_METHOD_FIELD_NAME, constructHttpMethodFragment(request));
        message.getFragments()
                .put(HttpMessagingHandler.HTTP_PATH_FIELD_NAME, constructHttpPathFragment(request));
        message.getFragments()
                .put(HttpMessagingHandler.HTTP_PROTOCOL_VERSION_FIELD_NAME, constructHttpVersionFragment(request));
        MapType headers = constructHttpHeadersFragment(request);
        message.getFragments()
                .put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, headers);

        if(request instanceof HttpEntityEnclosingRequest) {
            ((DefaultBHttpServerConnection) connection).receiveRequestEntity((HttpEntityEnclosingRequest) request);

            logger.debug(addMarker(), "Received request entity: " + request);

            HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();

            BinaryType httpBody = constructHttpBodyFragment(request, entity);
            message.getFragments().put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, httpBody);
            message.getFragments().put(HttpMessagingHandler.HTTP_PARTS_FIELD_NAME, getMultipartData(httpBody, request));

        }

        return message;
    }

    private MapType getMultipartData(BinaryType httpBody, HttpRequest request) {
        String boundary = null;
        Header[] contentTypeHeaders = request.getHeaders("Content-Type");
        if (contentTypeHeaders != null && contentTypeHeaders.length == 1 && contentTypeHeaders[0] != null) {
            HeaderElement[] contentTypeHeaderElements = contentTypeHeaders[0].getElements();
            if (contentTypeHeaderElements != null) {
                for (HeaderElement contentTypeHeaderElement: contentTypeHeaderElements) {
                    NameValuePair nameValue = contentTypeHeaderElement.getParameterByName("boundary");
                    if (nameValue != null) {
                        boundary = nameValue.getValue();
                    }
                }
            }
        }
        MapType info = new MapType();
        int partCounter = 0;
        if (!StringUtils.isBlank(boundary)) {
            boundary = boundary.trim();
            ListType parts = new ListType("map");
            MapType partsByName = new MapType();
            try (ByteArrayInputStream bis = new ByteArrayInputStream((byte[])httpBody.getValue())) {
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

    /**
     * Receives HTTP responses from servers
     * @param configurations Receiver configurations
     * @return Received HTTP response
     * @throws Exception
     */
    private Message receiveHttpResponse(List<Configuration> configurations) throws Exception {
        logger.debug(addMarker(), "Message received: " + socket);

        HttpResponse response = ((DefaultBHttpClientConnection) connection).receiveResponseHeader();

        logger.debug(addMarker(), "Received response header: " + response);

        ((DefaultBHttpClientConnection) connection).receiveResponseEntity(response);

        logger.debug(addMarker(), "Received response entity: " + response);

        //check retrieved status code
        checkStatusCode(configurations, response);

        HttpEntity entity = response.getEntity();

        Message message = new Message();
        message
                .getFragments()
                .put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, constructHttpHeadersFragment(response));
        message
                .getFragments()
                .put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, constructHttpBodyFragment(response, entity));

        return message;
    }

	private StringType constructHttpPathFragment(HttpRequest request) {
		return new StringType(request.getRequestLine().getUri());
	}

	private StringType constructHttpMethodFragment(HttpRequest request) {
		return new StringType(request.getRequestLine().getMethod());
	}

    private StringType constructHttpVersionFragment(HttpRequest request) {
        return new StringType(request.getRequestLine().getProtocolVersion().toString());
    }

	private MapType constructHttpHeadersFragment(HttpMessage request) {
		MapType headerMap = new MapType();
		for(Header h : request.getAllHeaders()) {
			StringType value = new StringType(h.getValue());

			headerMap.addItem(h.getName(), value);
		}
		return headerMap;
	}

	private String getPartName(String headers) {
        HeaderElement[] headerElements = BasicHeaderValueParser.parseElements(headers, null);
        for(HeaderElement h : headerElements) {
            for (NameValuePair pair: h.getParameters()) {
                String name = pair.getName().trim().toLowerCase();
                if ("name".equals(name)) {
                    return pair.getValue().trim();
                }
            }
        }
        return null;
    }

	private BinaryType constructHttpBodyFragment(HttpMessage request, HttpEntity httpEntity) throws IOException {
		byte[] content = EntityUtils.toByteArray(httpEntity);

		BinaryType messageContent = new BinaryType();
		messageContent.setValue(content);

		return messageContent;
	}

    private void checkStatusCode(List<Configuration> configurations, HttpResponse response) throws Exception {
        Configuration expectedStatusCode = ConfigurationUtils.getConfiguration(configurations, HttpMessagingHandler.HTTP_STATUS_CODE_CONFIG_NAME);
        if(expectedStatusCode != null) { //here we expect received response status code to match provided status code configuration
            int statusCode = Integer.parseInt(expectedStatusCode.getValue());
            if(response.getStatusLine().getStatusCode() != statusCode){
                throw new Exception("Expected status code: " + expectedStatusCode + ", but received: " + response.getStatusLine().getStatusCode());
            }
        }
    }
}
