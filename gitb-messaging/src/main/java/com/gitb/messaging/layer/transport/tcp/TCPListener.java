package com.gitb.messaging.layer.transport.tcp;

import com.gitb.messaging.Message;
import com.gitb.messaging.layer.AbstractTransactionListener;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;

/**
 * Created by serbay.
 */
public class TCPListener extends AbstractTransactionListener {

    public TCPListener(SessionContext session, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        super(session, receiverTransactionContext, senderTransactionContext);
    }
}
