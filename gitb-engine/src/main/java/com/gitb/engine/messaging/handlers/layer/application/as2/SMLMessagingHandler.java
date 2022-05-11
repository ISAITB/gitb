package com.gitb.engine.messaging.handlers.layer.application.as2;

import com.gitb.core.MessagingModule;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.layer.application.dns.DNSMessagingHandler;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;

/**
 * Created by serbay.
 */
@MessagingHandler(name="SMLMessaging")
public class SMLMessagingHandler extends DNSMessagingHandler {

	public static final String MODULE_DEFINITION_XML = "/messaging/sml-messaging-definition.xml";

	private static final MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

	@Override
	public MessagingModule getModuleDefinition() {
		return module;
	}
}
