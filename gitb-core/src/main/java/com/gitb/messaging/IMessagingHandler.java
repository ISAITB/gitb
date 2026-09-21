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

package com.gitb.messaging;

import com.gitb.StepHandler;
import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.core.MessagingModule;
import com.gitb.ms.InitiateResponse;
import com.gitb.tdl.MessagingStep;
import com.gitb.types.MapType;

import java.util.List;
import java.util.Set;

/**
 * Created by tuncay on 9/1/14.
 */
public interface IMessagingHandler extends StepHandler {

    /**
     * Returns the messaging module definition
     * @return module definition
     */
    MessagingModule getModuleDefinition();

    /**
     * Does initial configuration for the messaging module to begin
     * transactions and returns the session id
     * @param actorConfigurations actor configurations for the transaction that will be created
     * @return initial configuration object that consists some configurations
     * and session id
     */
    InitiateResponse initiate(List<ActorConfiguration> actorConfigurations);

    /**
     * Initiate a transaction with Messaging Handler
     */
    void beginTransaction(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations);

    /**
     *
     */
    MessagingReport sendMessage(String sessionId, String transactionId, String stepId, List<Configuration> configurations, Message message);

    /**
     *
     */
    MessagingReport receiveMessage(String sessionId, String transactionId, String callId, MessagingStep step, Message message, List<Thread> messagingThreads);

    /**
     *
     */
    MessagingReport listenMessage(String sessionId, String transactionId, String stepId, String from, String to, List<Configuration> configurations, Message inputs);

    /**
     * Close the transaction (Connection)
     * @param transactionId transaction id
     */
    void endTransaction(String sessionId, String transactionId, String stepId);

    /**
     * Close the session with the transactions in it
     * @param sessionId session id
     */
    void endSession(String sessionId);

    /**
     * The names of a {@code receive} step's {@code result/output} entries that this handler already accounts for
     * itself when producing the step's report - typically because they were consumed to build the response
     * returned to the caller and are consequently already reflected in the handler's own report (e.g. HTTP status/
     * headers/body). Any resolved output whose name is not listed here is added by the test engine to the step's
     * report as an additional top-level item, alongside whatever the handler itself produced.
     *
     * @return The output names to skip. Empty by default, meaning that every resolved output is added.
     */
    default Set<String> getResultOutputNamesHandledInternally() {
        return Set.of();
    }

    /**
     * Builds the preview of this step's own variable made available to a receive step's {@code result/steps} and
     * {@code result/output}, for handlers whose response must be resolved live before a caller awaiting it can be
     * answered (i.e. handlers that supply {@code CallbackData} enabling a servlet-driven call - see
     * {@code CallbackManager#requestResult}; currently {@code HttpMessagingV2}/{@code SoapMessagingV2}). Never
     * called for handlers that don't resolve a response this way. The shape of the returned map is entirely up to
     * the handler and should be documented as part of its own handler documentation (e.g.
     * {@code HttpMessagingV2}/{@code SoapMessagingV2} expose {@code request}/{@code response} sub-maps - a
     * different protocol may reasonably expose something else).
     *
     * @param request The actual incoming request/message that matched this step.
     * @param responseDefaults The step's own declared (default) input values - what the response would be built
     *                          from if {@code result} were not defined.
     * @return The preview map. Defaults to a flat merge of both messages' fragments (request values winning on a
     *         name clash) for handlers that don't override this.
     */
    default MapType buildResultPreview(Message request, Message responseDefaults) {
        MapType preview = new MapType();
        if (responseDefaults != null) {
            responseDefaults.getFragments().forEach(preview::addItem);
        }
        if (request != null) {
            request.getFragments().forEach(preview::addItem);
        }
        return preview;
    }

}
