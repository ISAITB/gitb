package com.gitb.messaging.layer.transport.udp;

import com.gitb.messaging.layer.AbstractDatagramListener;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;

/**
 * Created by serbay.
 */
public class UDPListener extends AbstractDatagramListener {
    public UDPListener(SessionContext session, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        super(session, receiverTransactionContext, senderTransactionContext);
    }
}
