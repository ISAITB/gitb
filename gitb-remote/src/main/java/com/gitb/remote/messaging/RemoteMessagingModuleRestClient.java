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

package com.gitb.remote.messaging;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.messaging.DeferredMessagingReport;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.model.core.BasicRequest;
import com.gitb.model.ms.*;
import com.gitb.remote.ClientConfiguration;
import com.gitb.remote.RemoteServiceRestClient;
import com.gitb.tdl.MessagingStep;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ModelUtils;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.gitb.utils.MessagingReportUtils.generateErrorReport;
import static com.gitb.utils.MessagingReportUtils.getMessagingReport;

public class RemoteMessagingModuleRestClient extends RemoteServiceRestClient implements IMessagingHandler {

    private MessagingModule serviceModule;
    private final Consumer<String> sessionEndListener;

    public RemoteMessagingModuleRestClient(URI serviceURL, Properties callProperties, String sessionId, String testCaseIdentifier, Supplier<ClientConfiguration> clientConfigurationProvider, Consumer<String> sessionEndListener) {
        super(serviceURL, callProperties, sessionId, testCaseIdentifier, clientConfigurationProvider);
        this.sessionEndListener = sessionEndListener;
    }

    public RemoteMessagingModuleRestClient(URI serviceURL, Properties callProperties) {
        this(serviceURL, callProperties, null, null, null, null);
    }

    @Override
    protected String getServiceLocation() {
        if (serviceModule != null) {
            return serviceModule.getServiceLocation();
        }
        return null;
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public MessagingModule getModuleDefinition() {
        return getModuleDefinition(true);
    }

    public MessagingModule getModuleDefinition(boolean cacheResult) {
        if (serviceModule != null) {
            return serviceModule;
        } else {
            com.gitb.ms.GetModuleDefinitionResponse response = call("GET", "getModuleDefinition", Optional.empty(), Optional.of(com.gitb.model.ms.GetModuleDefinitionResponse.class))
                    .map(ModelUtils::fromModel)
                    .orElseThrow(() -> new IllegalStateException("Remote service did not return a valid response"));
            MessagingModule result = response.getModule();
            if (cacheResult) {
                if (result == null) result = new MessagingModule();
                serviceModule = result;
            }
            return result;
        }
    }

    @Override
    public com.gitb.ms.InitiateResponse initiate(List<ActorConfiguration> actorConfigurations) {
        InitiateRequest request = InitiateRequest.builder()
                .withActorConfiguration(actorConfigurations.stream()
                        .map(actor -> com.gitb.model.core.ActorConfiguration.builder()
                                .withActor(actor.getActor())
                                .withEndpoint(actor.getEndpoint())
                                .withConfig(actor.getConfig().stream()
                                        .map(ModelUtils::toModel)
                                        .toArray(com.gitb.model.core.Configuration[]::new))
                                .build())
                        .toArray(com.gitb.model.core.ActorConfiguration[]::new))
                .build();
        // Skip step-specific client timeouts when initiating.
        com.gitb.ms.InitiateResponse response = call("POST", "initiate", Optional.of(request), Optional.of(com.gitb.model.ms.InitiateResponse.class), null, false)
                .map(ModelUtils::fromModel)
                .orElseThrow(() -> new IllegalStateException("Error raised by remote messaging service while processing the initiate call"));
        if (response.getSessionId() == null) {
            // Set the test session ID as the default.
            response.setSessionId(testSessionId);
        }
        return response;
    }

    @Override
    public void beginTransaction(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations) {
        BeginTransactionRequest request = BeginTransactionRequest.builder()
                .withSessionId(sessionId)
                .withFrom(from)
                .withTo(to)
                .withConfig(configurations.stream()
                        .map(ModelUtils::toModel)
                        .toArray(com.gitb.model.core.Configuration[]::new))
                .build();
        call("POST", "beginTransaction", Optional.of(request), Optional.empty(), stepIdMap(stepId));
    }

    @Override
    public MessagingReport sendMessage(String sessionId, String transactionId, String stepId, List<Configuration> configurations, Message message) {
        SendRequest request = SendRequest.builder()
                .withSessionId(sessionId)
                .withInput(message.getFragments().entrySet().stream()
                        .map(entry -> ModelUtils.toModel(DataTypeUtils.convertDataTypeToAnyContent(entry.getKey(), entry.getValue())))
                        .toArray(com.gitb.model.core.AnyContent[]::new))
                .build();
        SendResponse response = call("POST", "send", Optional.of(request), Optional.of(com.gitb.model.ms.SendResponse.class), stepIdMap(stepId))
                .orElseThrow(() -> new IllegalStateException("Remote service did not return a valid response"));
        if (response == null || response.getReport() == null) {
            return generateErrorReport("No response received");
        } else {
            return getMessagingReport(ModelUtils.fromModel(response.getReport()));
        }
    }

    @Override
    public MessagingReport receiveMessage(String sessionId, String transactionId, String callId, MessagingStep step, Message message, List<Thread> messagingThreads) {
        ReceiveRequest request = ReceiveRequest.builder()
                .withSessionId(sessionId)
                .withCallId(callId)
                .withInput(message.getFragments().entrySet().stream()
                        .map(entry -> ModelUtils.toModel(DataTypeUtils.convertDataTypeToAnyContent(entry.getKey(), entry.getValue())))
                        .toArray(com.gitb.model.core.AnyContent[]::new))
                .build();
        call("POST", "receive", Optional.of(request), Optional.empty(), stepIdMap(step.getId()));
        return new DeferredMessagingReport();
    }

    @Override
    public MessagingReport listenMessage(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations, Message inputs) {
        // Not applicable
        return null;
    }

    @Override
    public void endTransaction(String sessionId, String transactionId, String stepId) {
        BasicRequest request = BasicRequest.builderForBasicRequest()
                .withSessionId(sessionId)
                .build();
        call("POST", "endTransaction", Optional.of(request), Optional.empty(), stepIdMap(stepId));
    }

    @Override
    public void endSession(String sessionId) {
        try {
            FinalizeRequest request = FinalizeRequest.builder()
                    .withSessionId(sessionId)
                    .build();
            call("POST", "finalize", Optional.of(request), Optional.empty());
        } finally {
            if (sessionEndListener != null) {
                sessionEndListener.accept(sessionId);
            }
        }
    }
}
