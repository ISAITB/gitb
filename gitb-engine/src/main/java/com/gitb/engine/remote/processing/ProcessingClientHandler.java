package com.gitb.engine.remote.processing;

import com.gitb.engine.TestEngineConfiguration;
import com.gitb.engine.remote.BaseAddressingHandler;

public class ProcessingClientHandler extends BaseAddressingHandler {

    @Override
    protected String callbackURL() {
        return TestEngineConfiguration.PROCESSING_CALLBACK_URL;
    }
}
