package com.gitb.messaging.layer.application.smp;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.messaging.layer.application.http.HttpReceiver;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.types.MapType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by senan on 06.01.2015.
 */
public class SMPReceiver extends HttpReceiver {
    private Logger logger = LoggerFactory.getLogger(SMPReceiver.class);

    public SMPReceiver(SessionContext session, TransactionContext transaction) {
        super(session, transaction);
    }

    @Override
    public Message receive(List<Configuration> configurations, Message inputs) throws Exception {
        Message received =  super.receive(configurations, inputs); //ignore received message

        //construct response message
        MapType headers = (MapType) received.getFragments().get(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME);

        Message message = new Message();
        message.getFragments().put(HttpMessagingHandler.HTTP_HEADERS_FIELD_NAME, headers);

        return received;
    }

}
