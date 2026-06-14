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
import com.gitb.engine.actors.processors.TestCaseProcessorActor;
import com.gitb.tr.TAR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TestCapturingClient implements TestbedClient {

    private final CompletableFuture<Void> configComplete = new CompletableFuture<>();
    private final CompletableFuture<TestStepStatus> sessionResult = new CompletableFuture<>();
    private final List<TestStepStatus> allStatuses = Collections.synchronizedList(new ArrayList<>());
    private final List<String> logMessages = Collections.synchronizedList(new ArrayList<>());

    @Override
    public Void configurationComplete(ConfigurationCompleteRequest request) {
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
        sessionResult.completeExceptionally(
                new IllegalStateException("Unexpected interact step in test: " + request));
        return new Void();
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
