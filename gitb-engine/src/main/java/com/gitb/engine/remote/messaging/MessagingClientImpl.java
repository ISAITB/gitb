package com.gitb.engine.remote.messaging;

import com.gitb.messaging.CallbackManager;
import com.gitb.ms.MessagingClient;
import com.gitb.ms.NotifyForMessageRequest;
import com.gitb.ms.Void;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Action;

/**
 * Created by simatosc on 25/11/2016.
 */
@WebService(name = "MessagingClient", serviceName = "MessagingClientService", targetNamespace = "http://www.gitb.com/ms/v1/")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class MessagingClientImpl implements MessagingClient {

    @WebMethod
    @WebResult(
            name = "NotifyForMessageResponse",
            targetNamespace = "http://www.gitb.com/ms/v1/",
            partName = "parameters"
    )
    @Action(
            input = "http://gitb.com/MessagingClient/notifyForMessage",
            output = "http://gitb.com/MessagingClient/notifyForMessageResponse"
    )
    @Override
    public Void notifyForMessage(@WebParam(name = "NotifyForMessageRequest", targetNamespace = "http://www.gitb.com/ms/v1/", partName = "parameters") NotifyForMessageRequest parameters) {
        CallbackManager.getInstance().callbackReceived(parameters);
        return new Void();
    }

}
