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

package com.gitb.engine.messaging.handlers.layer;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.engine.PropertyConstants;
import com.gitb.engine.SessionManager;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.ms.InitiateResponse;
import com.gitb.types.DataType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

import static com.gitb.engine.TestEngineConfiguration.HANDLER_API_ROOT;
import static com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils.getUriExtension;

/**
 * Parent class for all new generation messaging handlers that don't use threading and direct
 * socket connections to send and receive messages.
 */
public abstract class AbstractNonWorkerMessagingHandler extends AbstractMessagingHandler {

    @Override
    public MessagingModule getModuleDefinition() {
        return new MessagingModule();
    }

    @Override
    public void beginTransaction(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations) {
        // Do nothing.
    }

    @Override
    public void endTransaction(String sessionId, String transactionId, String stepId) {
        // Do nothing.
    }

    @Override
    public void endSession(String sessionId) {
        // Do nothing.
    }

    @Override
    public MessagingReport listenMessage(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations, Message inputs) {
        throw new IllegalStateException("Only send and receive operations are supported by this handler");
    }

    @Override
    public boolean needsMessagingServerWorker() {
        return false;
    }

    @Override
    public InitiateResponse initiate(List<ActorConfiguration> actorConfigurations) {
        return new InitiateResponse();
    }

    @Override
    public InitiateResponse initiateWithSession(List<ActorConfiguration> actorConfigurations, String testSessionId) {
        var response = new InitiateResponse();
        /*
         * For simplicity, all these handlers use the test session ID as the messaging session ID. This simplifies
         * things like test session logging directly from the handler implementation.
         */
        response.setSessionId(testSessionId);
        return response;
    }

    public String getReceptionEndpoint(String sessionId, String handlerApiPath, Message inputs, String uriExtensionInputName) {
        DataType systemData = SessionManager.getInstance().getContext(sessionId).getScope().getVariable(PropertyConstants.SYSTEM_MAP).getValue();
        String systemApiKey;
        if (systemData instanceof MapType systemMap) {
            if (systemMap.getItems().get("apiKey") instanceof StringType apiKey) {
                systemApiKey = apiKey.toString();
            } else {
                throw new IllegalStateException("The SYSTEM map did not contain the expected apiKey property");
            }
        } else {
            throw new IllegalStateException("No SYSTEM map was found in the test session");
        }
        Optional<String> uriExtension = getUriExtension(inputs.getFragments(), uriExtensionInputName);
        return "%s%s%s%s".formatted(
                StringUtils.appendIfMissing(HANDLER_API_ROOT, "/"),
                StringUtils.appendIfMissing(handlerApiPath, "/"),
                systemApiKey,
                uriExtension.map(uri -> {
                    if (uri.startsWith("?")) {
                        return uri;
                    } else {
                        return StringUtils.prependIfMissing(uri, "/");
                    }
                }).orElse("")
        );
    }

}
