package com.gitb.engine.messaging.handlers.layer.application.as2.peppol;

import com.gitb.core.MessagingModule;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.model.SessionContext;
import com.gitb.engine.messaging.handlers.model.TransactionContext;
import com.gitb.engine.messaging.handlers.model.tcp.ITransactionReceiver;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;

import java.io.IOException;

/**
 * Created by senan on 11.11.2014.
 */
@MessagingHandler(name="NonValidatingPeppolAS2Messaging")
public class NonValidatingPeppolAS2MessagingHandler extends PeppolAS2MessagingHandler {

    private static final String MODULE_DEFINITION_XML = "/messaging/non-validating-peppol-as2-messaging-definition.xml";

    private static MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

    public MessagingModule getModuleDefinition() {
        return module;
    }

    @Override
    public ITransactionReceiver getReceiver(SessionContext sessionContext, TransactionContext transactionContext) throws IOException {
        return new NonValidatingPeppolAS2Receiver(sessionContext, transactionContext);
    }

    @Override
    protected boolean validateMetadata() {
        return false;
    }
}
