package com.gitb.engine.remote.messaging;

import com.gitb.ms.MessagingClient;
import com.gitb.ms.NotifyForMessageRequest;
import com.gitb.ms.Void;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

/**
 * Created by simatosc on 25/11/2016.
 */
@WebService(name = "MessagingClient", serviceName = "MessagingClientService", targetNamespace = "http://www.gitb.com/ms/v1/")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class MessagingClientImpl implements MessagingClient {

    @Override
    public Void notifyForMessage(@WebParam(name = "NotifyForMessageRequest", targetNamespace = "http://www.gitb.com/ms/v1/", partName = "parameters") NotifyForMessageRequest parameters) {
        CallbackManager.getInstance().callbackReceived(parameters);
        return new Void();
    }

}
