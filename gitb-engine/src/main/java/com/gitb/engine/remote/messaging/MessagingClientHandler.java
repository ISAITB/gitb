package com.gitb.engine.remote.messaging;

import com.gitb.engine.TestEngineConfiguration;
import com.gitb.engine.remote.BaseAddressingHandler;

/**
 * Created by simatosc on 28/11/2016.
 */
public class MessagingClientHandler extends BaseAddressingHandler {

    @Override
    protected String callbackURL() {
        return TestEngineConfiguration.MESSAGING_CALLBACK_URL;
    }
}
