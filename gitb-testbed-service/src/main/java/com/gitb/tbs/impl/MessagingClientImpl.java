package com.gitb.tbs.impl;

import com.gitb.messaging.CallbackManager;
import com.gitb.ms.MessagingClient;
import com.gitb.ms.NotifyForMessageRequest;
import com.gitb.ms.Void;
import org.springframework.stereotype.Component;

/**
 * Created by simatosc on 25/11/2016.
 */
//@WebService(name = "MessagingClient", serviceName = "MessagingClientService", targetNamespace = "http://www.gitb.com/ms/v1/")
//@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@Component
public class MessagingClientImpl implements MessagingClient {

    @Override
    public Void notifyForMessage(NotifyForMessageRequest parameters) {
        CallbackManager.getInstance().callbackReceived(parameters);
        return new Void();
    }

}
