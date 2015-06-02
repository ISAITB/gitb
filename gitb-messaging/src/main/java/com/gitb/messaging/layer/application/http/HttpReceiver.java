package com.gitb.messaging.layer.application.http;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.AbstractTransactionReceiver;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.BinaryType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;
import org.apache.http.*;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.BHttpConnectionBase;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public Message receive(List<Configuration> configurations) throws Exception {
        //use the socket retrieved from the transaction
        socket = getSocket();

        //if the socket is null, that means transaction has just begun, so create new
        //below code blocks until a socket is created
        if(socket == null){
            waitUntilMessageReceived();
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
        logger.debug("Message received: " + socket);

        logger.debug("Connection created: " + connection);

        HttpRequest request = ((DefaultBHttpServerConnection) connection).receiveRequestHeader();

        logger.debug("Received request header: " + request);

        Message message = new Message();
        message.getFragments()
                .put(HttpMessagingHandler.HTTP_METHOD_FIELD_NAME, constructHttpMethodFragment(request));
        message.getFragments()
                .put(HttpMessagingHandler.HTTP_PATH_FIELD_NAME, constructHttpPathFragment(request));
        message.getFragments()
                .put(HttpMessagingHandler.HTTP_PROTOCOL_VERSION_FIELD_NAME, constructHttpVersionFragment(request));
        message.getFragments()
                .put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, constructHttpHeadersFragment(request));

        if(request instanceof HttpEntityEnclosingRequest) {
            ((DefaultBHttpServerConnection) connection).receiveRequestEntity((HttpEntityEnclosingRequest) request);

            logger.debug("Received request entity: " + request);

            HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();
            message
                    .getFragments()
                    .put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, constructHttpBodyFragment(request, entity));
        }

        return message;
    }

    /**
     * Receives HTTP responses from servers
     * @param configurations Receiver configurations
     * @return Received HTTP response
     * @throws Exception
     */
    private Message receiveHttpResponse(List<Configuration> configurations) throws Exception {
        logger.debug("Message received: " + socket);

        HttpResponse response = ((DefaultBHttpClientConnection) connection).receiveResponseHeader();

        logger.debug("Received response header: " + response);

        ((DefaultBHttpClientConnection) connection).receiveResponseEntity(response);

        logger.debug("Received response entity: " + response);

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
