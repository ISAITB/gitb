package com.gitb.engine.remote.validation;

import com.gitb.engine.TestEngineConfiguration;
import com.gitb.engine.remote.BaseAddressingHandler;

public class ValidationClientHandler extends BaseAddressingHandler {

    @Override
    protected String callbackURL() {
        return TestEngineConfiguration.VALIDATION_CALLBACK_URL;
    }
}
