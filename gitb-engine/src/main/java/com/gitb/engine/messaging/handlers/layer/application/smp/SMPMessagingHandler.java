package com.gitb.engine.messaging.handlers.layer.application.smp;

import com.gitb.core.MessagingModule;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionReceiver;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionSender;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;

import java.io.IOException;

/**
 * Created by senan on 06.01.2015.
 */
@MessagingHandler(name="SMPMessaging")
public class SMPMessagingHandler extends AbstractMessagingHandler{

    public static final String SMP_METADATA_FIELD_NAME = "smp_metadata";

    public static final String MODULE_DEFINITION_XML = "/messaging/smp-messaging-definition.xml";

    private static MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

    @Override
    public ITransactionReceiver getReceiver(SessionContext sessionContext, TransactionContext transactionContext) throws IOException {
        return new SMPReceiver(sessionContext, transactionContext);
    }

    @Override
    public ITransactionSender getSender(SessionContext sessionContext, TransactionContext transactionContext) {
        return new SMPSender(sessionContext, transactionContext);
    }

    @Override
    public MessagingModule getModuleDefinition() {
        return module;
    }
}
