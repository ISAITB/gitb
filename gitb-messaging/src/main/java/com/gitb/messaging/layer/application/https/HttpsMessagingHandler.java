package com.gitb.messaging.layer.application.https;

import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.SecurityUtils;
import com.gitb.messaging.SessionManager;
import com.gitb.messaging.layer.AbstractMessagingHandler;
import com.gitb.messaging.model.tcp.ITransactionListener;
import com.gitb.messaging.model.tcp.ITransactionReceiver;
import com.gitb.messaging.model.tcp.ITransactionSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.util.List;

/**
 * Created by senan on 07.11.2014.
 */
@MetaInfServices(IMessagingHandler.class)
public class HttpsMessagingHandler extends AbstractMessagingHandler{
    private static Logger logger = LoggerFactory.getLogger(HttpsMessagingHandler.class);

    private static final String PKI_ALGORITHM     = "SunX509";
    private static final String SECURE_ALGORITHM  = "TLSv1.2";

    public  static final String MODULE_DEFINITION_XML = "/https-messaging-definition.xml";

    private static MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

    @Override
    public void beginTransaction(String sessionId, String transactionId, String from, String to, List<Configuration> configurations) {
        super.beginTransaction(sessionId, transactionId, from, to, configurations);

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
