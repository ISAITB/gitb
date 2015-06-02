package com.gitb.messaging.layer.application.https;

import com.gitb.messaging.Message;
import com.gitb.messaging.layer.application.http.HttpListener;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by serbay.
 */
public class HttpsListener extends HttpListener {
    public HttpsListener(SessionContext session, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        super(session, receiverTransactionContext, senderTransactionContext);
    }
}
