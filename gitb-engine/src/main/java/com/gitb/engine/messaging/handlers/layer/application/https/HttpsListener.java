package com.gitb.engine.messaging.handlers.layer.application.https;

import com.gitb.engine.messaging.handlers.layer.application.http.HttpListener;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;

/**
 * Created by serbay.
 */
public class HttpsListener extends HttpListener {
    public HttpsListener(SessionContext session, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        super(session, receiverTransactionContext, senderTransactionContext);
    }
}
