package com.gitb.messaging.layer.application.as2;

import com.gitb.core.MessagingModule;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.layer.application.dns.DNSMessagingHandler;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import org.kohsuke.MetaInfServices;

/**
 * Created by serbay.
 */
@MetaInfServices(IMessagingHandler.class)
public class SMLMessagingHandler extends DNSMessagingHandler {

	public static final String MODULE_DEFINITION_XML = "/sml-messaging-definition.xml";

	private static final MessagingModule module = MessagingHandlerUtils.readModuleDefinition(MODULE_DEFINITION_XML);

	@Override
	public MessagingModule getModuleDefinition() {
		return module;
	}
}
