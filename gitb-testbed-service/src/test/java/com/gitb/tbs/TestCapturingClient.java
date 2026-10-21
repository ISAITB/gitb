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

package com.gitb.tbs;

import com.gitb.PropertyConstants;
import com.gitb.engine.TestbedService;
import com.gitb.engine.actors.processors.TestCaseProcessorActor;
import com.gitb.tr.TAR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class TestCapturingClient implements TestbedClient {

    private volatile Function<InteractWithUsersRequest, List<UserInput>> interactionResponder;
    private final CompletableFuture<Void> configComplete = new CompletableFuture<>();
    private final CompletableFuture<ConfigurationCompleteRequest> configurationResult = new CompletableFuture<>();
    private final CompletableFuture<TestStepStatus> sessionResult = new CompletableFuture<>();
    private final List<TestStepStatus> allStatuses = Collections.synchronizedList(new ArrayList<>());
    private final List<String> logMessages = Collections.synchronizedList(new ArrayList<>());

    @Override
    public Void configurationComplete(ConfigurationCompleteRequest request) {
        configurationResult.complete(request);
        configComplete.complete(null);
        return new Void();
    }

    @Override
    public Void updateStatus(TestStepStatus status) {
        allStatuses.add(status);
        String stepId = status.getStepId();
        if (PropertyConstants.LOG_EVENT_STEP_ID.equals(stepId)
                && status.getReport() instanceof TAR tar
                && tar.getContext() != null) {
            String msg = tar.getContext().getValue();
            if (msg != null) {
                logMessages.add(msg);
            }
        }
        if (TestCaseProcessorActor.TEST_SESSION_END_STEP_ID.equals(stepId)
                || TestCaseProcessorActor.TEST_SESSION_END_EXTERNAL_STEP_ID.equals(stepId)) {
            sessionResult.complete(status);
        }
        return new Void();
    }

    @Override
    public Void interactWithUsers(InteractWithUsersRequest request) {
        if (interactionResponder == null) {
            sessionResult.completeExceptionally(
                    new IllegalStateException("Unexpected interact step in test: " + request));
        } else {
            // Respond from a separate thread, as a user would, rather than from within the engine's call.
            CompletableFuture.runAsync(() -> {
                try {
                    TestbedService.provideInput(request.getTcInstanceid(), request.getStepId(), interactionResponder.apply(request), false);
                } catch (Exception e) {
                    sessionResult.completeExceptionally(e);
                }
            });
        }
        return new Void();
    }

    /**
     * Sets the function providing the user inputs to submit when an interact step is reached. Without one, any
     * interact step fails the test.
     */
    public void respondToInteractionsWith(Function<InteractWithUsersRequest, List<UserInput>> responder) {
        this.interactionResponder = responder;
    }

    /**
     * The outcome of the session's configuration (carrying an error code and description if it failed).
     */
    public CompletableFuture<ConfigurationCompleteRequest> configurationResult() {
        return configurationResult;
    }

    public CompletableFuture<Void> configComplete() {
        return configComplete;
    }

    public CompletableFuture<TestStepStatus> sessionResult() {
        return sessionResult;
    }

    public List<TestStepStatus> allStatuses() {
        return Collections.unmodifiableList(allStatuses);
    }

    public List<String> logMessages() {
        return Collections.unmodifiableList(logMessages);
    }
}
