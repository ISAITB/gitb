package com.gitb.engine.messaging.handlers.layer.application.domibus;

import com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ebms.Messaging;
import com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ebms.PartInfo;
import com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ecodex.*;
import com.gitb.utils.XMLUtils;
import jakarta.activation.DataHandler;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.ws.Holder;
import org.apache.cxf.attachment.ByteDataSource;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.springframework.util.StreamUtils;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Created by simatosc on 01/12/2016.
 */
public class DomibusClient implements AutoCloseable {

    private BackendInterface domibus;
    private final BackendInfo backend;

    public DomibusClient(BackendInfo backend) {
        this.backend = backend;
    }

    @Override
    public void close() {
        if (domibus != null) {
            try {
                Client client = ClientProxy.getClient(domibus);
                client.destroy();
                domibus = null;
            } catch (Exception e) {
                // Ignore.
            }
        }
    }

    public List<String> getPendingMessageIds() {
        return getBackend().listPendingMessages("").getMessageID();
    }

    public MessageData downloadMessage(String messageId) {
        Holder<RetrieveMessageResponse> responseHolder = new Holder<>();
        Holder<Messaging> headerHolder = new Holder<>();
        RetrieveMessageRequest request = new RetrieveMessageRequest();
        request.setMessageID(messageId);
        try {
            getBackend().retrieveMessage(request, responseHolder, headerHolder);
        } catch (RetrieveMessageFault retrieveMessageFault) {
            throw new IllegalStateException("Error from backend", retrieveMessageFault);
        }
        RetrieveMessageResponse retrieveResponse = responseHolder.value;
        Messaging headerResponse = headerHolder.value;
        return getDataForDomibusMessage(headerResponse, retrieveResponse);
    }

    public List<String> sendToDomibus(Messaging headerInfo, List<byte[]> payloads) {
        SubmitRequest submitRequest = new SubmitRequest();
        int counter = 0;
        for (PartInfo partInfo: headerInfo.getUserMessage().getPayloadInfo().getPartInfo()) {
            LargePayloadType payloadType = new LargePayloadType();
            payloadType.setPayloadId(partInfo.getHref());
            payloadType.setValue(new DataHandler(new ByteDataSource(payloads.get(counter))));
            submitRequest.getPayload().add(payloadType);
            counter += 1;
        }
        SubmitResponse response;
        try {
            response = getBackend().submitMessage(submitRequest, headerInfo);
        } catch (SubmitMessageFault submitMessageFault) {
            throw new IllegalStateException("Error from backend: " + submitMessageFault.getMessage());
        }
        return response.getMessageID();
    }

    public String checkStatus(String messageId) {
        try {
            StatusRequest request = new StatusRequest();
            request.setMessageID(messageId);
            MessageStatus status = getBackend().getStatus(request);
            return status.value();
        } catch (StatusFault statusFault) {
            throw new IllegalStateException("Error from backend", statusFault);
        }
    }

    private MessageData getDataForDomibusMessage(Messaging headerResponse, RetrieveMessageResponse retrieveResponse) {
        MessageData data = new MessageData();
        var objFactory = new com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ebms.ObjectFactory();
        objFactory.createMessaging(headerResponse);
        try {
            data.setHeaderContent(XMLUtils.marshalToString(objFactory.createMessaging(headerResponse)));
        } catch (JAXBException e) {
            throw new IllegalStateException("Error serialising messaging header", e);
        }
        for (LargePayloadType payload: retrieveResponse.getPayload()) {
            Payload payloadData = new Payload();
            payloadData.setId(payload.getPayloadId());
            payloadData.setContentType(payload.getContentType());
            try {
                payloadData.setValue(StreamUtils.copyToByteArray(payload.getValue().getInputStream()));
            } catch (IOException e) {
                throw new IllegalStateException("Error serialising message content", e);
            }
            data.getPayloads().add(payloadData);
        }
        return data;
    }

    private BackendInterface getBackend() {
        if (domibus == null) {
            try {
                var proxyFactoryBean = new JaxWsProxyFactoryBean();
                proxyFactoryBean.setWsdlLocation("/messaging/wsdl/domibus.wsdl");
                proxyFactoryBean.setServiceClass(BackendInterface.class);
                proxyFactoryBean.setServiceName(new QName("http://org.ecodex.backend/1_1/", "BackendService_1_1"));
                proxyFactoryBean.setEndpointName(new QName("http://org.ecodex.backend/1_1/", "BACKEND_PORT"));
                proxyFactoryBean.setAddress(backend.address());
                var serviceProxy = (BackendInterface)proxyFactoryBean.create();
                var client = ClientProxy.getClient(serviceProxy);
                var conduit = (HTTPConduit)client.getConduit();
                conduit.getClient().setAutoRedirect(true);
                // Apply credentials.
                if (backend.username() != null && !backend.username().isBlank()) {
                    Objects.requireNonNull(backend.password(), "When a username is provided for backend authentication a password must be provided as well.");
                    var authType = Objects.requireNonNullElse(backend.authenticationType(), AuthType.BASIC);
                    String authTypeToSet;
                    if (authType == AuthType.BASIC) {
                        authTypeToSet = "Basic";
                    } else {
                        authTypeToSet = "Digest";
                    }
                    var policy = new AuthorizationPolicy();
                    policy.setUserName(backend.username().trim());
                    policy.setPassword(backend.password().trim());
                    policy.setAuthorizationType(authTypeToSet);
                    conduit.setAuthorization(policy);
                }
                domibus = serviceProxy;
            } catch (Exception e) {
                throw new IllegalStateException("Unable to create Domibus client", e);
            }
        }
        return domibus;
    }

}
