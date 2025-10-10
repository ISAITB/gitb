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
import com.gitb.engine.SessionManager;
import com.gitb.ps.ProcessingClient;
import com.gitb.ps.LogRequest;
import com.gitb.ps.Void;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcessingClientImpl implements ProcessingClient {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessingClientImpl.class);

    @Override
    public Void log(LogRequest logRequest) {
        // The received session ID is a processing session ID
        if (logRequest.getSessionId() != null) {
            var testSessionId = SessionManager.getInstance().getTestSessionForProcessingSession(logRequest.getSessionId());
            if (testSessionId == null) {
                LOG.warn("Could not determine test session for processing session [{}]", logRequest.getSessionId());
            } else {
                CallbackManager.getInstance().logMessageReceived(testSessionId, logRequest.getMessage(), logRequest.getLevel());
            }
        } else {
            LOG.warn("Received log message from processing service but no session ID was provided");
        }
        return new Void();
    }

}
