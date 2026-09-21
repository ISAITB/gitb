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

import com.gitb.tbs.servers.HttpMessagingServer;
import com.gitb.tbs.servers.SoapMessagingServer;
import com.gitb.tr.TAR;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.gitb.tbs.TdlTestHelper.run;
import static com.gitb.tbs.TdlTestHelper.runAsync;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Exercises {@code receive/result}: a {@code HttpMessagingV2} response resolved dynamically (against the actual
 * incoming request, optionally running further TDL steps) rather than eagerly from the step's own declared inputs.
 */
class ReceiveResultTest extends BaseIntegrationTest {

    private static ExecutorService callbackExecutor;
    private static MockMvc mockMvc;

    @BeforeAll
    static void setUpServer() {
        callbackExecutor = Executors.newFixedThreadPool(4);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new HttpMessagingServer(callbackExecutor),
                new SoapMessagingServer(callbackExecutor)
        ).build();
        stubTdl("msg-http-receive-result-output", "tdl/msg/msg-http-receive-result-output.xml");
        stubTdl("msg-http-receive-result-steps", "tdl/msg/msg-http-receive-result-steps.xml");
        stubTdl("msg-http-receive-result-verify-fail", "tdl/msg/msg-http-receive-result-verify-fail.xml");
        stubTdl("msg-http-receive-result-timeout", "tdl/msg/msg-http-receive-result-timeout.xml");
        stubTdl("msg-simulated-receive-result-immediate", "tdl/msg/msg-simulated-receive-result-immediate.xml");
        stubTdl("msg-simulated-receive-result-deferred", "tdl/msg/msg-simulated-receive-result-deferred.xml");
    }

    @AfterAll
    static void tearDownServer() {
        callbackExecutor.shutdownNow();
    }

    private MvcResult performAndAwait(RequestBuilder requestBuilder, long asyncResultTimeoutMs) throws Exception {
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertTrue(mvcResult.getRequest().isAsyncStarted(), "Expected the messaging controller to process this request asynchronously");
        mvcResult.getAsyncResult(asyncResultTimeoutMs);
        return mockMvc.perform(asyncDispatch(mvcResult)).andReturn();
    }

    /**
     * A {@code result} with only {@code output} (no {@code steps}) resolves its response purely from expressions
     * evaluated against the real incoming request - here echoing the request body back with a status not declared
     * anywhere on the step's own (eager) inputs.
     */
    @Test
    void resultWithOutputOnlyEchoesRequestBody() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var run = runAsync("msg-http-receive-result-output", apiKey, List.of());
        try {
            Thread.sleep(500);
            MvcResult result = performAndAwait(
                    post("/http/" + apiKey + "/probe").contentType(MediaType.TEXT_PLAIN).content("ping-dynamic"),
                    5000);
            assertEquals(201, result.getResponse().getStatus(), "Status should come from result/output, not any step default");
            assertEquals("ping-dynamic", result.getResponse().getContentAsString(), "Body should echo the real incoming request, resolved dynamically");
            run.result().get(30, TimeUnit.SECONDS).assertSuccess();
        } finally {
            stopQuietly(run.sessionId());
        }
    }

    /**
     * A {@code result} with {@code steps} runs them in an isolated child scope exposing the real incoming request
     * as {@code $msg{request}} (a preview of what the step's own id will hold once it completes) - {@code output}
     * can then reference values that scope produced. A custom (handler-unrecognized) output name also lands as a
     * top-level sibling on {@code $msg}, visible to a later step - proving the generic actor-side report patching
     * works end-to-end, not just in the HTTP response returned to the caller.
     */
    @Test
    void resultWithStepsComputesResponseFromRequest() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var run = runAsync("msg-http-receive-result-steps", apiKey, List.of());
        try {
            Thread.sleep(500);
            MvcResult result = performAndAwait(
                    post("/http/" + apiKey + "/probe").contentType(MediaType.TEXT_PLAIN).content("ignored"),
                    5000);
            assertEquals(202, result.getResponse().getStatus());
            assertEquals("ack-POST", result.getResponse().getContentAsString(), "Body should come from the variable assigned in result/steps, derived from the real request method");
            run.result().get(30, TimeUnit.SECONDS).assertSuccess().assertLogs(List.of("outcome=accepted"), false);
        } finally {
            stopQuietly(run.sessionId());
        }
    }

    /**
     * The generic report patch is not just visible via {@code $msg} to later steps (see
     * {@link #resultWithStepsComputesResponseFromRequest()}) - it is also written into the receive step's own
     * presented report (its {@code TAR} context), a distinct object from the {@code Message} that feeds
     * {@code $msg}: the context is a one-time snapshot taken when the report is built, so patching the message
     * alone would not be enough - it would reach {@code $msg} but silently vanish from the presented report.
     */
    @Test
    void resultOutputIsPatchedIntoTheStepsOwnReportContext() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var run = runAsync("msg-http-receive-result-steps", apiKey, List.of());
        try {
            Thread.sleep(500);
            performAndAwait(post("/http/" + apiKey + "/probe").contentType(MediaType.TEXT_PLAIN).content("ignored"), 5000);
            TestRunResult result = run.result().get(30, TimeUnit.SECONDS).assertSuccess();
            boolean found = result.allStatuses().stream()
                    .filter(status -> status.getReport() instanceof TAR)
                    .map(status -> (TAR) status.getReport())
                    .filter(tar -> tar.getContext() != null)
                    .flatMap(tar -> tar.getContext().getItem().stream())
                    .anyMatch(item -> "validationOutcome".equals(item.getName()) && "accepted".equals(item.getValue()));
            assertTrue(found, "Expected the receive step's own report context to contain a patched 'validationOutcome' item");
        } finally {
            stopQuietly(run.sessionId());
        }
    }

    /**
     * A failing step within {@code result/steps} (here a failing {@code verify}) fails the {@code receive} step
     * itself and causes the SUT-facing HTTP call to be answered with a 500, rather than silently falling back to
     * any default response.
     */
    @Test
    void failingResultStepFailsCallAndSession() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var run = runAsync("msg-http-receive-result-verify-fail", apiKey, List.of());
        try {
            Thread.sleep(500);
            MvcResult result = performAndAwait(post("/http/" + apiKey + "/probe").content("ping"), 8000);
            assertEquals(500, result.getResponse().getStatus(), "A failing result/steps step should cause the incoming call to be answered with an error");
            run.result().get(30, TimeUnit.SECONDS).assertFailed();
        } finally {
            stopQuietly(run.sessionId());
        }
    }

    /**
     * {@code result/@timeout} bounds how long {@code result/steps} and {@code result/output} may take once a
     * matching call has actually arrived - distinct from the {@code receive} step's own {@code timeout}, which
     * only bounds waiting for a call to arrive in the first place (and has already elapsed favourably by then).
     */
    @Test
    void resultTimeoutFailsCallAndSessionWithoutWaitingForSteps() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var run = runAsync("msg-http-receive-result-timeout", apiKey, List.of());
        try {
            Thread.sleep(500);
            long start = System.nanoTime();
            MvcResult result = performAndAwait(post("/http/" + apiKey + "/probe").content("ping"), 8000);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertEquals(500, result.getResponse().getStatus());
            assertTrue(elapsedMs < 1800, "Should have failed via result/@timeout (300ms) well before the 2000ms inner delay completed, took " + elapsedMs + " ms");
            run.result().get(30, TimeUnit.SECONDS).assertFailed();
        } finally {
            stopQuietly(run.sessionId());
        }
    }

    /**
     * {@code result} generalizes to non-HTTP/SOAP handlers with zero handler-side changes: for
     * {@code SimulatedMessaging} the handler's own (already complete) report is used as the preview exposed to
     * {@code result/steps}, and the resolved {@code result/output} is patched onto it afterwards - no messaging
     * server is ever involved. This specific case (no {@code delay} input) is also the regression test for the
     * actor thread-safety fix: {@code SimulatedMessagingHandler} returns its report synchronously from within the
     * receive step's own background future, which must route the report back to the actor's own thread before
     * spawning the {@code result/steps} child actor - spawning it directly from that background thread would be
     * unsafe and would hang or fail this test.
     */
    @Test
    void resultGeneralizesToSimulatedHandlerImmediateReport() throws Exception {
        run("msg-simulated-receive-result-immediate").assertSuccess().assertLogs(List.of("verdict=ok-A1"), false);
    }

    /**
     * As {@link #resultGeneralizesToSimulatedHandlerImmediateReport()} but via the deferred/async callback path
     * ({@code delay} input), exercising the generic {@code NotificationReceived} entry point into 'result'
     * processing distinctly from the synchronous one.
     */
    @Test
    void resultGeneralizesToSimulatedHandlerDeferredReport() throws Exception {
        run("msg-simulated-receive-result-deferred").assertSuccess().assertLogs(List.of("verdict=ok-A1"), false);
    }

    private void stopQuietly(String sessionId) {
        try {
            com.gitb.engine.TestbedService.stop(sessionId, false);
        } catch (Exception e) {
            // Best-effort cleanup only.
        }
    }

}
