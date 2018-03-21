package com.gitb.messaging.layer.application.as2.peppol;

import com.gitb.core.MessagingModule;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.model.tcp.ITransactionReceiver;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import org.kohsuke.MetaInfServices;

import java.io.IOException;

/**
 * Created by senan on 11.11.2014.
 */
@MetaInfServices(IMessagingHandler.class)
public class NonValidatingPeppolAS2MessagingHandler extends PeppolAS2MessagingHandler {

    private static final String MODULE_DEFINITION_XML = "/non-validating-peppol-as2-messaging-definition.xml";

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
