package com.gitb.engine.messaging.handlers.layer.application.soap;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.engine.messaging.handlers.layer.AbstractTransactionListener;
import com.gitb.engine.messaging.handlers.layer.application.http.HttpMessagingHandler;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by serbay.
 */
public class SoapListener extends AbstractTransactionListener {
    public SoapListener(SessionContext session, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        super(session, receiverTransactionContext, senderTransactionContext);
    }

    @Override
    public List<Configuration> transformConfigurations(Message incomingMessage, List<Configuration> configurations) {
        List<Configuration> transformed = new ArrayList<>(configurations);

        StringType httpPath = (StringType) incomingMessage.getFragments().get(SoapMessagingHandler.HTTP_PATH_FIELD_NAME);

        transformed.add(ConfigurationUtils.constructConfiguration(SoapMessagingHandler.HTTP_URI_CONFIG_NAME, (String) httpPath.getValue()));

        return transformed;
    }

    @Override
    public Message transformMessage(Message incomingMessage) throws Exception{
        Message message = new Message();

        message.getFragments()
                .put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME,
                        incomingMessage.getFragments().get(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME));

        message.getFragments()
                .put(SoapMessagingHandler.SOAP_MESSAGE_FIELD_NAME,
                        incomingMessage.getFragments().get(SoapMessagingHandler.SOAP_MESSAGE_FIELD_NAME));

        return message;
    }
}
