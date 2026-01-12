/*
 * Copyright (C) 2026 European Union
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

package com.gitb.engine.messaging.handlers.layer.application.domibus;

import java.time.Instant;
import java.util.Set;

public record PollingState(
        DomibusClient client,
        Instant pollStart,
        int currentPollAttemp,
        int maximumPollAttempts,
        long pollInterval,
        String messageIdentifier,
        ReceiveType pollType,
        Set<String> ackSuccessStates,
        Set<String> ackFailureStates,
        String sessionId,
        String callId) {

    public PollingState nextAttempt() {
        return new PollingState(client, pollStart, currentPollAttemp + 1, maximumPollAttempts, pollInterval, messageIdentifier, pollType, ackSuccessStates, ackFailureStates, sessionId, callId);
    }

}
