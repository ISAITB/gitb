package com.gitb.engine.messaging.handlers.layer.transport.udp;

import com.gitb.engine.messaging.handlers.layer.AbstractDatagramListener;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;

/**
 * Created by serbay.
 */
public class UDPListener extends AbstractDatagramListener {
    public UDPListener(SessionContext session, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        super(session, receiverTransactionContext, senderTransactionContext);
    }
}
