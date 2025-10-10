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

package com.gitb.engine.messaging.handlers.model;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;

import java.util.List;

/**
 * Created by serbay.
 */
public interface IListener {
    /**
     * Transform incoming message to outgoing message
     * @param incomingMessage Incoming message
     * @return Outgoing message
     */
    Message transformMessage(Message incomingMessage);

    /**
     * Transform listen configurations to be used when sending the message
     * to the receiver SUT.
     * @param incomingMessage Incoming message
     * @return Transformed configurations
     */
    List<Configuration> transformConfigurations(Message incomingMessage, List<Configuration> configurations);

    /**
     * Listens incoming message and forwards it to the relevant actor
     * after transforming it using {@link #transformMessage(com.gitb.messaging.Message)}.
     */
    Message listen(List<com.gitb.core.Configuration> configurations, Message inputs) throws Exception;
}
