package com.gitb.messaging.layer.application.http;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.ServerUtils;
import com.gitb.messaging.layer.AbstractTransactionSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.*;
import com.gitb.utils.ConfigurationUtils;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.BHttpConnectionBase;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpClientConnectionFactory;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by serbay on 9/23/14.
 */
public class HttpSender extends AbstractTransactionSender {
    private Logger logger = LoggerFactory.getLogger(HttpSender.class);

    private static final int BUFFER_SIZE = 8 * 1024;
    private static HttpConnectionFactory<DefaultBHttpClientConnection> httpConnectionFactory;

    static {
        ConnectionConfig connectionConfig = ConnectionConfig
                .custom()
                .setBufferSize(BUFFER_SIZE)
                .setCharset(Charset.defaultCharset())
                .build();

        httpConnectionFactory = new DefaultBHttpClientConnectionFactory(connectionConfig);
    }

    /**
     * Default HTTP Connection object
     */
    protected BHttpConnectionBase connection;

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
                Configuration ipAddressConfig = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.IP_ADDRESS_CONFIG_NAME);
                Configuration portConfig = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.PORT_CONFIG_NAME);

                SocketFactory sf = sslContext.getSocketFactory();
                socket = sf.createSocket(InetAddress.getByName(ipAddressConfig.getValue()),
                        Integer.parseInt(portConfig.getValue()));

                transaction.setParameter(Socket.class, socket);
            }
        }

        //this ensures that a socket is created and saved into the transaction
        super.send(configurations, message);

        //use the connection retrieved from the transaction
        connection = transaction.getParameter(BHttpConnectionBase.class);

        //if the connection is null, that means transaction has just begun, so create new
        if (connection == null) {
            connection = httpConnectionFactory.createConnection(getSocket());
            transaction.setParameter(BHttpConnectionBase.class, connection);
        }

        //connection is a client connection and will send HTTP requests
        if (connection instanceof DefaultBHttpClientConnection) {
            sendHttpRequest(configurations, message);
        }

        //connection has received an HTTP request and will send a response
        if (connection instanceof DefaultBHttpServerConnection) {
            sendHttpResponse(configurations, message);
        }

        return message;
    }

    private void sendHttpRequest(List<Configuration> configurations, Message message) throws Exception {
        logger.debug("Connection created: " + connection);

        BasicHttpEntityEnclosingRequest request = createHttpRequest(configurations, message);

        ((DefaultBHttpClientConnection) connection).sendRequestHeader(request);
        logger.debug("Sent request: " + request);
        ((DefaultBHttpClientConnection) connection).flush();

        ((DefaultBHttpClientConnection) connection).sendRequestEntity(request);
        logger.debug("Sent entity: " + request + " - " + request.getEntity());

        ((DefaultBHttpClientConnection) connection).flush();
        logger.debug("Flushed connection: " + connection);
    }

    private void sendHttpResponse(List<Configuration> configurations, Message message) throws Exception {
        BasicHttpResponse response = createHttpResponse(configurations, message);

        ((DefaultBHttpServerConnection) connection).sendResponseHeader(response);
        logger.debug("Sent response: " + response);

        ((DefaultBHttpServerConnection) connection).sendResponseEntity(response);
        logger.debug("Sent response entity: " + response + " - " + response.getEntity());

        ((DefaultBHttpServerConnection) connection).flush();
        logger.debug("Flushed connection: " + connection);
    }

    protected BasicHttpEntityEnclosingRequest createHttpRequest(List<Configuration> configurations, Message message) {
        String method = getHttpMethod(configurations, message);
        String path = getHttpPath(configurations, message);
        Map<String, String> headers = getHttpHeaders(message);

        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(method, path);

        byte[] messageContent = getHttpBody(message);
        if (messageContent != null) {
            ByteArrayEntity entity = new ByteArrayEntity(messageContent);
            request.setEntity(entity);
            request.addHeader(entity.getContentEncoding());
            request.addHeader(entity.getContentType());
            request.addHeader(HTTP.CONTENT_LEN, String.valueOf(entity.getContentLength()));
            request.addHeader(HTTP.TARGET_HOST, getHost() + ":" + getPort());
        } else {
            ListType partInput = (ListType) message.getFragments().get(HttpMessagingHandler.HTTP_PARTS_FIELD_NAME);
            if (partInput != null) {
                // Send the request as a multipart request.
                if (!partInput.isEmpty() && "map".equals(partInput.getContainedType())) {
                    List<MapType> parts = (List<MapType>) partInput.getValue();
                    MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
                    for (MapType partInfo: parts) {
                        String name = (String)(partInfo.getItem("name")).getValue();
                        StringType fileName = (StringType)partInfo.getItem("file_name");
                        DataType content = partInfo.getItem("content");
                        if (fileName == null) {
                            // Text part.
                            if (!(content instanceof StringType)) {
                                content = content.convertTo(DataType.STRING_DATA_TYPE);
                            }
                            entityBuilder.addTextBody(name, (String)content.getValue());
                        } else {
                            // Binary/File part.
                            String fileNameValue = (String)(fileName).getValue();
                            String contentType = (String)(partInfo.getItem("content_type")).getValue();
                            if (!(content instanceof BinaryType)) {
                                content = content.convertTo(DataType.BINARY_DATA_TYPE);
                            }
                            entityBuilder.addBinaryBody(name, (byte[])content.getValue(), ContentType.getByMimeType(contentType), fileNameValue);
                        }
                    }
                    HttpEntity entity = entityBuilder.build();
                    request.setEntity(entity);
                    request.addHeader(entity.getContentEncoding());
                    request.addHeader(entity.getContentType());
                    request.addHeader(HTTP.CONTENT_LEN, String.valueOf(entity.getContentLength()));
                    request.addHeader(HTTP.TARGET_HOST, getHost() + ":" + getPort());
                } else {
                    logger.warn("Input for " + HttpMessagingHandler.HTTP_PARTS_FIELD_NAME + " must contain map items");
                }
            }
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request.setHeader(entry.getKey(), entry.getValue());
        }

        return request;
    }

    protected BasicHttpResponse createHttpResponse(List<Configuration> configurations, Message message) {
        BasicHttpEntityEnclosingRequest request = createHttpRequest(configurations, message);
        BasicHttpResponse response = null;

        Configuration statusCode = ConfigurationUtils.getConfiguration(configurations, HttpMessagingHandler.HTTP_STATUS_CODE_CONFIG_NAME);
        if (statusCode == null) { //send default response status code
            response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null);
        } else { //send status code provided as configuration
            int status = Integer.parseInt(statusCode.getValue());
            response = new BasicHttpResponse(HttpVersion.HTTP_1_1, status, null);
        }

        response.setHeaders(request.getAllHeaders());
        response.setEntity(request.getEntity());
        return response;
    }

    protected byte[] getHttpBody(Message message) {
        BinaryType data = (BinaryType) message.getFragments().get(HttpMessagingHandler.HTTP_BODY_FIELD_NAME);

        if (data != null) {
            return (byte[]) data.getValue();
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
                headers.put(name, (String) value.getValue());
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
        Configuration host = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.IP_ADDRESS_CONFIG_NAME);
        return host.getValue();
    }

    protected String getPort() {
        ActorConfiguration actorConfiguration = transaction.getWith();
        Configuration port = ConfigurationUtils.getConfiguration(actorConfiguration.getConfig(), ServerUtils.PORT_CONFIG_NAME);
        return port.getValue();
    }
}
