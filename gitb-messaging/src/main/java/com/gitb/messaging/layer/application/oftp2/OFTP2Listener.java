package com.gitb.messaging.layer.application.oftp2;

import com.gitb.messaging.layer.AbstractTransactionListener;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;

public class OFTP2Listener extends AbstractTransactionListener{

    public OFTP2Listener(SessionContext session, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        super(session, receiverTransactionContext, senderTransactionContext);
    }
}
