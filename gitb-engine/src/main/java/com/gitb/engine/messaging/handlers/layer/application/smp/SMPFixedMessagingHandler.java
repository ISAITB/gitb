package com.gitb.engine.messaging.handlers.layer.application.smp;

import com.gitb.core.MessagingModule;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.server.Configuration;
import com.gitb.engine.messaging.handlers.server.IMessagingServer;
import com.gitb.engine.messaging.handlers.server.tcp.TCPMessagingServer;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;

import java.io.IOException;

import static com.gitb.engine.TestEngineConfiguration.DEFAULT_MESSAGING_CONFIGURATION;

/**
 * Created by senan on 06.01.2015.
 */
@MessagingHandler(name="SMPFixedMessaging")
public class SMPFixedMessagingHandler extends SMPMessagingHandler {

    public static final String MODULE_DEFINITION_XML = "/messaging/smp-fixed-messaging-definition.xml";

    private static final MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

    @Override
    public MessagingModule getModuleDefinition() {
        return module;
    }

    @Override
    protected IMessagingServer getMessagingServer() throws IOException {
        return new TCPMessagingServer(new Configuration(
                80, 80,
                DEFAULT_MESSAGING_CONFIGURATION.getIpAddress(), DEFAULT_MESSAGING_CONFIGURATION.getActorName(),
                null, null, null));
    }

}
