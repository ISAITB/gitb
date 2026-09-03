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

import com.gitb.engine.TestEngineConfiguration;
import com.gitb.tbs.servers.HttpMessagingServer;
import com.gitb.tbs.servers.SoapMessagingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.gitb.tbs.TdlTestHelper.runAsync;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises {@link HttpMessagingServer} and {@link SoapMessagingServer} directly over (mock) HTTP, covering the
 * race between an incoming call from a system under test and the corresponding {@code receive} test step -
 * previously untested (see {@link MessagingHandlersTest}, whose receive tests inject reports directly into
 * {@code CallbackManager}, bypassing these controllers entirely).
 */
class CallbackHoldingServletTest extends BaseIntegrationTest {

    private static ExecutorService callbackExecutor;
    private static MockMvc mockMvc;

    @BeforeAll
    static void setUpServer() {
        callbackExecutor = Executors.newFixedThreadPool(4);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new HttpMessagingServer(callbackExecutor),
                new SoapMessagingServer(callbackExecutor)
        ).build();
        stubTdl("msg-http-receive-race", "tdl/msg/msg-http-receive-race.xml");
        stubTdl("msg-soap-receive-race", "tdl/msg/msg-soap-receive-race.xml");
        stubTdl("msg-http-receive-race-timeout", "tdl/msg/msg-http-receive-race-timeout.xml");
    }

    @AfterAll
    static void tearDownServer() {
        callbackExecutor.shutdownNow();
    }

    /**
     * Performs the given request and awaits and dispatches the async result. These controllers return
     * {@code CompletableFuture}, which Spring MVC's {@code DeferredResultMethodReturnValueHandler} always
     * processes asynchronously (releasing the container thread via {@code HttpServletRequest.startAsync()}) -
     * asserted here rather than merely assumed, since that is precisely the property the whole point of using
     * async responses (not consuming a worker thread per held call) depends on.
     */
    private MvcResult performAndAwait(RequestBuilder requestBuilder, long asyncResultTimeoutMs) throws Exception {
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertTrue(mvcResult.getRequest().isAsyncStarted(), "Expected the messaging controller to process this request asynchronously");
        mvcResult.getAsyncResult(asyncResultTimeoutMs);
        return mockMvc.perform(asyncDispatch(mvcResult)).andReturn();
    }

    /**
     * Case (a) for HTTP: a call fired while the session is still a couple of steps away from its
     * {@code HttpMessagingV2} receive step is held and is served once that step is reached.
     */
    @Test
    void httpCallFiredEarlyIsHeldAndServed() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var run = runAsync("msg-http-receive-race", apiKey, List.of());
        try {
            // Fire the call ~500ms into the 2s delay step, well before the HttpMessagingV2 receive step is reached.
            Thread.sleep(500);
            MvcResult result = performAndAwait(
                    post("/http/" + apiKey + "/probe").contentType(MediaType.TEXT_PLAIN).content("ping"),
                    5000);
            assertEquals(200, result.getResponse().getStatus(), "Held call should have been matched and answered once the receive step was reached");
            run.result().get(30, TimeUnit.SECONDS).assertSuccess();
        } finally {
            stopQuietly(run.sessionId());
        }
    }

    /**
     * Case (a) for SOAP: same as above but for the {@code SoapMessagingV2} handler, which - unlike the HTTP one -
     * additionally has to reparse a real SOAP envelope from the captured request body once dispatched.
     */
    @Test
    void soapCallFiredEarlyIsHeldAndServed() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var run = runAsync("msg-soap-receive-race", apiKey, List.of());
        try {
            Thread.sleep(500);
            String envelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soapenv:Body><ping/></soapenv:Body></soapenv:Envelope>";
            MvcResult result = performAndAwait(
                    post("/soap/" + apiKey + "/probe").contentType("text/xml; charset=UTF-8").content(envelope),
                    5000);
            assertEquals(200, result.getResponse().getStatus(), "Held call should have been matched and answered once the receive step was reached");
            run.result().get(30, TimeUnit.SECONDS).assertSuccess();
        } finally {
            stopQuietly(run.sessionId());
        }
    }

    /**
     * Case (b): with holding disabled ({@code CALLBACK_WAIT_TIMEOUT=0}), an early call is rejected immediately -
     * the previous (pre-fix) behaviour - even though a session for its system API key is actively running.
     */
    @Test
    void httpCallRejectedImmediatelyWhenHoldingDisabled() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        long original = TestEngineConfiguration.CALLBACK_WAIT_TIMEOUT;
        TestEngineConfiguration.CALLBACK_WAIT_TIMEOUT = 0;
        var run = runAsync("msg-http-receive-race-timeout", apiKey, List.of());
        try {
            Thread.sleep(300);
            long start = System.nanoTime();
            MvcResult result = performAndAwait(post("/http/" + apiKey + "/probe").content("ping"), 2000);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertEquals(404, result.getResponse().getStatus());
            assertTrue(elapsedMs < 500, "Call should have been rejected near-instantly, took " + elapsedMs + " ms");
            // Nothing ever serves the receive step - it fails via its own short timeout.
            run.result().get(10, TimeUnit.SECONDS).assertFailed();
        } finally {
            TestEngineConfiguration.CALLBACK_WAIT_TIMEOUT = original;
            stopQuietly(run.sessionId());
        }
    }

    /**
     * Case (d): a call held past a (short) configured wait window is rejected once that window elapses, without
     * waiting for the receive step to ever be reached.
     */
    @Test
    void httpCallHeldPastWaitWindowIsRejected() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        long original = TestEngineConfiguration.CALLBACK_WAIT_TIMEOUT;
        TestEngineConfiguration.CALLBACK_WAIT_TIMEOUT = 300;
        // Delay step is 1500ms - well beyond the 300ms wait window - so the call must be dropped before it.
        var run = runAsync("msg-http-receive-race-timeout", apiKey, List.of());
        try {
            Thread.sleep(200);
            long start = System.nanoTime();
            MvcResult result = performAndAwait(post("/http/" + apiKey + "/probe").content("ping"), 2000);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertEquals(404, result.getResponse().getStatus());
            assertTrue(elapsedMs >= 250, "Call should have been held for close to the wait window, took only " + elapsedMs + " ms");
            assertTrue(elapsedMs < 1200, "Call should have been dropped well before the receive step was ever reached, took " + elapsedMs + " ms");
            run.result().get(10, TimeUnit.SECONDS).assertFailed();
        } finally {
            TestEngineConfiguration.CALLBACK_WAIT_TIMEOUT = original;
            stopQuietly(run.sessionId());
        }
    }

    /**
     * Case (c): a call for a system API key that has no active session at all is dropped immediately, without
     * being held for the (here, deliberately generous) configured wait window.
     */
    @Test
    void httpCallForUnknownSystemIsRejectedImmediately() throws Exception {
        String unknownApiKey = UUID.randomUUID().toString();
        long original = TestEngineConfiguration.CALLBACK_WAIT_TIMEOUT;
        TestEngineConfiguration.CALLBACK_WAIT_TIMEOUT = 5000;
        try {
            long start = System.nanoTime();
            MvcResult result = performAndAwait(post("/http/" + unknownApiKey + "/probe").content("ping"), 2000);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertEquals(404, result.getResponse().getStatus());
            assertTrue(elapsedMs < 500, "Call for an unknown system API key should have been rejected near-instantly, took " + elapsedMs + " ms");
        } finally {
            TestEngineConfiguration.CALLBACK_WAIT_TIMEOUT = original;
        }
    }

    /**
     * A held call's payload is spilled to disk (see {@code CallbackPayloadStore}) rather than pinning heap for
     * as long as it is parked, and the file is removed once the call has been served. The storage folder
     * resolved here is the real one used by the running {@code CallbackPayloadStore} singleton - it cannot be
     * pointed at a per-test temp directory since the singleton (like {@code SessionManager}'s own temp storage)
     * resolves its location once, at engine startup, in {@link BaseIntegrationTest}'s static initializer.
     */
    @Test
    void heldCallPayloadIsSpilledToDiskWhilePendingAndRemovedOnceServed() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var storageDir = Path.of(TestEngineConfiguration.TEMP_CALLBACK_STORAGE_LOCATION);
        var run = runAsync("msg-http-receive-race", apiKey, List.of());
        try {
            Thread.sleep(500);
            MvcResult mvcResult = mockMvc.perform(post("/http/" + apiKey + "/probe").contentType(MediaType.TEXT_PLAIN).content("ping-to-disk")).andReturn();
            assertTrue(mvcResult.getRequest().isAsyncStarted(), "Expected the messaging controller to process this request asynchronously");
            // Give the container thread a brief moment to finish capture() (which happens before the async wait begins) before inspecting the folder.
            Thread.sleep(200);
            try (var files = Files.list(storageDir)) {
                assertTrue(files.findAny().isPresent(), "Expected the held call's payload to have been spilled to disk while parked");
            }
            mvcResult.getAsyncResult(5000);
            mvcResult = mockMvc.perform(asyncDispatch(mvcResult)).andReturn();
            assertEquals(200, mvcResult.getResponse().getStatus(), "Held call should have been matched and answered once the receive step was reached");
            run.result().get(30, TimeUnit.SECONDS).assertSuccess();
            try (var files = Files.list(storageDir)) {
                assertTrue(files.findAny().isEmpty(), "Expected the held call's payload to have been released once served");
            }
        } finally {
            stopQuietly(run.sessionId());
        }
    }

    /**
     * A call that arrives after its receive step is already parked is matched immediately (see
     * {@code CallbackManager.lookupHandlingData}'s eager path), so its payload is never worth spilling to disk -
     * only a call actually about to be held is.
     */
    @Test
    void httpCallMatchedImmediatelyIsNeverSpilledToDisk() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var storageDir = Path.of(TestEngineConfiguration.TEMP_CALLBACK_STORAGE_LOCATION);
        // The delay step is 2s - wait past it so the HttpMessagingV2 receive step is already parked when we call.
        var run = runAsync("msg-http-receive-race", apiKey, List.of());
        try {
            Thread.sleep(2200);
            MvcResult result = performAndAwait(post("/http/" + apiKey + "/probe").content("ping-immediate"), 5000);
            assertEquals(200, result.getResponse().getStatus());
            run.result().get(30, TimeUnit.SECONDS).assertSuccess();
            try (var files = Files.list(storageDir)) {
                assertTrue(files.findAny().isEmpty(), "An immediately-matched call should never have its payload spilled to disk");
            }
        } finally {
            stopQuietly(run.sessionId());
        }
    }

    /**
     * A held multipart call is served with all of its parts intact once matched - exercising the multipart
     * capture/spill/read-back path end-to-end (each part is spilled and read back independently of the body).
     */
    @Test
    void multipartCallFiredEarlyIsHeldAndServedWithPartsIntact() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var run = runAsync("msg-http-receive-race", apiKey, List.of());
        try {
            Thread.sleep(500);
            MvcResult result = performAndAwait(
                    multipart("/http/" + apiKey + "/probe")
                            .file(new MockMultipartFile("file1", "upload1.txt", "text/plain", "file-content-1".getBytes(StandardCharsets.UTF_8)))
                            .file(new MockMultipartFile("file2", "upload2.bin", "application/octet-stream", new byte[]{1, 2, 3, 4, 5})),
                    5000);
            assertEquals(200, result.getResponse().getStatus(), "Held multipart call should have been matched and answered once the receive step was reached");
            run.result().get(30, TimeUnit.SECONDS).assertSuccess();
        } finally {
            stopQuietly(run.sessionId());
        }
    }

    private void stopQuietly(String sessionId) {
        try {
            com.gitb.engine.TestbedService.stop(sessionId, false);
        } catch (Exception e) {
            // Best-effort cleanup only.
        }
    }

}
