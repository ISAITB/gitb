package com.gitb.engine.messaging.handlers.layer.transport.tcp;

import com.gitb.engine.messaging.handlers.layer.AbstractTransactionListener;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;

/**
 * Created by serbay.
 */
public class TCPListener extends AbstractTransactionListener {

    public TCPListener(SessionContext session, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        super(session, receiverTransactionContext, senderTransactionContext);
    }
}
