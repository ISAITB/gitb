package com.gitb.messaging.layer.application.smp;

import com.gitb.core.MessagingModule;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.layer.AbstractMessagingHandler;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.messaging.model.tcp.ITransactionReceiver;
import com.gitb.messaging.model.tcp.ITransactionSender;
import com.gitb.messaging.server.Configuration;
import com.gitb.messaging.server.IMessagingServer;
import com.gitb.messaging.server.tcp.TCPMessagingServer;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import org.kohsuke.MetaInfServices;

import java.io.IOException;

/**
 * Created by senan on 06.01.2015.
 */
@MetaInfServices(IMessagingHandler.class)
public class SMPFixedMessagingHandler extends SMPMessagingHandler {

    private static final Configuration DEFAULT_CONFIGURATION = Configuration.defaultConfiguration();

    public static final String MODULE_DEFINITION_XML = "/smp-fixed-messaging-definition.xml";

    private static MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

    @Override
    public MessagingModule getModuleDefinition() {
        return module;
    }

    @Override
    protected IMessagingServer getMessagingServer() throws IOException {
        return new TCPMessagingServer(new Configuration(
                80, 80,
                DEFAULT_CONFIGURATION.getIpAddress(), DEFAULT_CONFIGURATION.getActorName(),
                null, null, null));
    }

}
