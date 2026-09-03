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

import com.gitb.core.ActorConfiguration;
import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.CallbackManager;
import com.gitb.engine.TestbedService;
import com.gitb.messaging.MessagingReport;
import com.gitb.tbs.impl.TestbedServiceCallbackHandler;
import com.gitb.tr.TestResultType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TdlTestHelper {

    private TdlTestHelper() {}

    public static TestRunResult run(String testCaseId) throws Exception {
        return run(testCaseId, null, List.of());
    }

    public static TestRunResult run(String testCaseId, String apiKey) throws Exception {
        return run(testCaseId, apiKey, List.of());
    }

    public static TestRunResult run(String testCaseId, String apiKey, List<AnyContent> inputs) throws Exception {
        TestCapturingClient client = new TestCapturingClient();
        String sessionId = TestbedService.initiate(testCaseId, null);
        TestbedServiceCallbackHandler.getInstance().registerForTest(sessionId, client);
        TestbedService.configure(sessionId, buildConfigurations(apiKey), inputs);
        TestbedService.start(sessionId);
        TestStepStatus status = client.sessionResult().get(30, TimeUnit.SECONDS);
        return new TestRunResult(status, client.logMessages());
    }

    /**
     * A session started via {@link #runAsync} without blocking for its outcome, paired with its session ID so
     * that the caller can interact with it (e.g. issue calls against its configured system API key, or stop it
     * explicitly) while it is still executing.
     */
    public record AsyncRun(String sessionId, CompletableFuture<TestRunResult> result) {}

    /**
     * As {@link #run(String, String, List)} but returns immediately without waiting for the session to complete,
     * so that the caller can act (e.g. simulate an incoming asynchronous call) while the session is still
     * progressing through its steps.
     */
    public static AsyncRun runAsync(String testCaseId, String apiKey, List<AnyContent> inputs) throws Exception {
        TestCapturingClient client = new TestCapturingClient();
        String sessionId = TestbedService.initiate(testCaseId, null);
        TestbedServiceCallbackHandler.getInstance().registerForTest(sessionId, client);
        TestbedService.configure(sessionId, buildConfigurations(apiKey), inputs);
        // configure() only tells the SessionSupervisor actor - it does not itself wait for the configuration
        // (including the SYSTEM api key ending up in the session's TestCaseContext) to actually be applied.
        // Callers of runAsync rely on that having happened by the time this method returns (e.g. to immediately
        // fire a call against the session's system API key), so wait for the same signal the real TestbedClient
        // callback would receive before starting the session.
        client.configComplete().get(5, TimeUnit.SECONDS);
        TestbedService.start(sessionId);
        CompletableFuture<TestRunResult> resultFuture = client.sessionResult()
                .thenApply(status -> new TestRunResult(status, client.logMessages()));
        return new AsyncRun(sessionId, resultFuture);
    }

    public static TestRunResult runWithCallback(String testCaseId, String apiKey, List<AnyContent> inputs,
                                                long callbackDelayMs, MessagingReport callbackReport) throws Exception {
        TestCapturingClient client = new TestCapturingClient();
        String sessionId = TestbedService.initiate(testCaseId, null);
        TestbedServiceCallbackHandler.getInstance().registerForTest(sessionId, client);
        TestbedService.configure(sessionId, buildConfigurations(apiKey), inputs);
        TestbedService.start(sessionId);
        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.schedule(
                    () -> CallbackManager.getInstance().callbackReceived(sessionId, null, callbackReport),
                    callbackDelayMs, TimeUnit.MILLISECONDS
            );
        }
        TestStepStatus status = client.sessionResult().get(30, TimeUnit.SECONDS);
        return new TestRunResult(status, client.logMessages());
    }

    public static void assertSuccess(TestRunResult result) {
        result.assertSuccess();
    }

    public static void assertFailed(TestRunResult result) {
        result.assertFailed();
    }

    public static void assertWarning(TestRunResult result) {
        result.assertWarning();
    }

    public static void assertSuccess(TestStepStatus status) {
        assertEquals(TestResultType.SUCCESS, status.getReport().getResult(),
                "Expected session SUCCESS but got: " + status.getReport().getResult());
    }

    public static void assertFailed(TestStepStatus status) {
        assertEquals(TestResultType.FAILURE, status.getReport().getResult(),
                "Expected session FAILURE but got: " + status.getReport().getResult());
    }

    public static void assertWarning(TestStepStatus status) {
        assertEquals(TestResultType.WARNING, status.getReport().getResult(),
                "Expected session WARNING but got: " + status.getReport().getResult());
    }

    public static AnyContent stringInput(String name, String value) {
        AnyContent input = new AnyContent();
        input.setName(name);
        input.setValue(value);
        input.setType("string");
        input.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        return input;
    }

    public static AnyContent binaryInput(String name, byte[] value) {
        AnyContent input = new AnyContent();
        input.setName(name);
        input.setValue(java.util.Base64.getEncoder().encodeToString(value));
        input.setType("binary");
        input.setEmbeddingMethod(ValueEmbeddingEnumeration.BASE_64);
        return input;
    }

    private static List<ActorConfiguration> buildConfigurations(String apiKey) {
        List<ActorConfiguration> configs = new ArrayList<>();
        ActorConfiguration sutConfig = new ActorConfiguration();
        sutConfig.setActor("SUT");
        configs.add(sutConfig);
        if (apiKey != null) {
            ActorConfiguration sysConfig = new ActorConfiguration();
            // Must be the reserved actor name SessionConfigurationData looks for - not the TDL actor id "SYSTEM" -
            // otherwise this is treated as a plain (unused) SUT actor configuration and never reaches the
            // session's SYSTEM scope map at all.
            sysConfig.setActor(com.gitb.PropertyConstants.ACTOR_CONFIG_SYSTEM);
            Configuration apiKeyConfig = new Configuration();
            apiKeyConfig.setName("apiKey");
            apiKeyConfig.setValue(apiKey);
            sysConfig.getConfig().add(apiKeyConfig);
            configs.add(sysConfig);
        }
        return configs;
    }
}
