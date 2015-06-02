package com.gitb.messaging.layer.application.smp;

import com.gitb.core.MessagingModule;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.layer.AbstractMessagingHandler;
import com.gitb.messaging.model.tcp.ITransactionReceiver;
import com.gitb.messaging.model.tcp.ITransactionSender;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import org.kohsuke.MetaInfServices;

import java.io.IOException;

/**
 * Created by senan on 06.01.2015.
 */
@MetaInfServices(IMessagingHandler.class)
public class SMPMessagingHandler extends AbstractMessagingHandler{

    public static final String SMP_METADATA_FIELD_NAME = "smp_metadata";

    public static final String MODULE_DEFINITION_XML = "/smp-messaging-definition.xml";

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
