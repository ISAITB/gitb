package com.gitb.engine.messaging.handlers.layer.application.https;

import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.SecurityUtils;
import com.gitb.engine.messaging.handlers.SessionManager;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionListener;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionReceiver;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionSender;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.List;

/**
 * Created by senan on 07.11.2014.
 */
@MessagingHandler(name="HttpsMessaging")
public class HttpsMessagingHandler extends AbstractMessagingHandler{

    public  static final String MODULE_DEFINITION_XML = "/messaging/https-messaging-definition.xml";

    private static final MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

    @Override
    public void beginTransaction(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations) {
        super.beginTransaction(sessionId, transactionId, stepId, from, to, configurations);

        SessionContext sessionContext = SessionManager.getInstance().getSession(sessionId);
        List<TransactionContext> transactions = sessionContext.getTransactions(transactionId);

        //create an SSLContext and save it to the transaction context
        SSLContext sslContext = SecurityUtils.createSSLContext();

        for(TransactionContext transactionContext : transactions) {
            transactionContext.setParameter(SSLContext.class, sslContext);
        }
    }

    @Override
    public ITransactionReceiver getReceiver(SessionContext sessionContext, TransactionContext transactionContext) throws IOException {
        return new HttpsReceiver(sessionContext, transactionContext);
    }

    @Override
    public ITransactionSender getSender(SessionContext sessionContext, TransactionContext transactionContext) {
        return new HttpsSender(sessionContext, transactionContext);
    }

    @Override
    public ITransactionListener getListener(SessionContext sessionContext, TransactionContext receiverTransactionContext, TransactionContext senderTransactionContext) {
        return new HttpsListener(sessionContext, receiverTransactionContext, senderTransactionContext);
    }

    @Override
    public MessagingModule getModuleDefinition() {
        return module;
    }
}
