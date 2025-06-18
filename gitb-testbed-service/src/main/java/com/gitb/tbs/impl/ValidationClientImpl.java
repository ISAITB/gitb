/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.tbs.impl;

import com.gitb.engine.CallbackManager;
import com.gitb.vs.LogRequest;
import com.gitb.vs.ValidationClient;
import com.gitb.vs.Void;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ValidationClientImpl implements ValidationClient {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationClientImpl.class);

    @Override
    public Void log(LogRequest logRequest) {
        // The received session ID is the test session ID
        if (logRequest.getSessionId() != null) {
            CallbackManager.getInstance().logMessageReceived(logRequest.getSessionId(), logRequest.getMessage(), logRequest.getLevel());
        } else {
            LOG.warn("Received log message from validation service but no session ID was provided");
        }
        return new Void();
    }

}
