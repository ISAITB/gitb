package com.gitb.engine.remote.messaging;

import com.gitb.ms.MessagingServiceService;

import jakarta.jws.HandlerChain;
import jakarta.xml.ws.WebServiceClient;
import java.net.URL;

/**
 * Created by simatosc on 28/11/2016.
 */
@WebServiceClient(name = "MessagingServiceService", targetNamespace = "http://www.gitb.com/ms/v1/", wsdlLocation = "http://www.gitb.com/services")
@HandlerChain(file="handler-chain-messaging.xml")
public class MessagingServiceClient extends MessagingServiceService {

    public MessagingServiceClient(URL serviceURL) {
        super(serviceURL);
    }
}
