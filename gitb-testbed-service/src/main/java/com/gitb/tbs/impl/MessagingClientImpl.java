package com.gitb.tbs.impl;

import com.gitb.engine.messaging.CallbackManager;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import com.gitb.ms.MessagingClient;
import com.gitb.ms.NotifyForMessageRequest;
import com.gitb.ms.Void;
import org.springframework.stereotype.Component;

/**
 * Created by simatosc on 25/11/2016.
 */
@Component
public class MessagingClientImpl implements MessagingClient {

    @Override
    public Void notifyForMessage(NotifyForMessageRequest parameters) {
        CallbackManager.getInstance().callbackReceived(
                parameters.getSessionId(),
                parameters.getCallId(),
                MessagingHandlerUtils.getMessagingReport(parameters.getReport()));
        return new Void();
    }

}
