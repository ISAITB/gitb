package com.gitb.messaging.layer.application.http.proxy;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.layer.application.http.HttpSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.BinaryType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * HTTP proxy implementation that proxies (currently) GET and POST requests to a configured server.
 * The proxy also exposes all request and response information so that it can be subsequently
 * validated.
 *
 * Created by simatosc on 26/04/2016.
 */
public class HttpProxySender extends HttpSender {

    private Logger logger = LoggerFactory.getLogger(HttpProxySender.class);

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
        String receiverAddress = receiverConfiguration.getValue();

        MapType requestData = (MapType) message.getFragments().get(HttpProxyMessagingHandler.HTTP_REQUEST_DATA);
        String httpMethod = requestData.getItem(HttpProxyMessagingHandler.HTTP_METHOD_FIELD_NAME).getValue().toString();
        HttpRequestBase httpRequest = null;
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
        if ("POST".equals(httpMethod)) {
            httpRequest = new HttpPost(receiverAddress);
            BinaryType body = (BinaryType) requestData.getItem(HttpProxyMessagingHandler.HTTP_BODY_FIELD_NAME);
            if (body != null) {
                ByteArrayEntity contentEntity = new ByteArrayEntity((byte[])body.getValue());
                ((HttpPost)httpRequest).setEntity(contentEntity);
            }
        } else if ("GET".equals(httpMethod)) {
            httpRequest = new HttpGet(receiverAddress);
        }
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        byte[] responseContent = null;
        try {
            response = httpclient.execute(httpRequest);
            HttpEntity responseEntity = response.getEntity();
            responseContent = EntityUtils.toByteArray(responseEntity);
        } catch (Exception e) {
            logger.error(addMarker(), "Error sending message to proxied receiver", e);
            throw e;
        } finally {
            if (response != null) {
                response.close();
            }
        }
        MapType headers = new MapType();
        for (Header header: response.getAllHeaders()) {
            headers.addItem(header.getName(), new StringType(header.getValue()));
        }
        BinaryType responseMessageContent = new BinaryType();
        responseMessageContent.setValue(responseContent);
        configurations.add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_METHOD_CONFIG_NAME, httpMethod));
        message.getFragments().put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, responseMessageContent);
        message.getFragments().put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, headers);

        MapType responseData = new MapType();
        responseData.addItem("http_headers", headers);
        responseData.addItem("http_body", responseMessageContent);
        requestData.addItem("response_data", responseData);
    }

}
