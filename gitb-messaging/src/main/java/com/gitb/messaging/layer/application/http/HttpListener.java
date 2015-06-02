package com.gitb.messaging.layer.application.http;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.AbstractTransactionListener;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by serbay.
 */
public class HttpListener extends AbstractTransactionListener {

    public HttpListener(SessionContext session, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        super(session, receiverTransactionContext, senderTransactionContext);
    }

    @Override
    public List<Configuration> transformConfigurations(Message incomingMessage, List<Configuration> configurations) {
        List<Configuration> transformed = new ArrayList<>(configurations);

        StringType method = (StringType) incomingMessage.getFragments().get(HttpMessagingHandler.HTTP_METHOD_FIELD_NAME);
        StringType path = (StringType) incomingMessage.getFragments().get(HttpMessagingHandler.HTTP_PATH_FIELD_NAME);

        transformed.add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_METHOD_CONFIG_NAME, (String) method.getValue()));
        transformed.add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_URI_CONFIG_NAME, (String) path.getValue()));

        return transformed;
    }
}
