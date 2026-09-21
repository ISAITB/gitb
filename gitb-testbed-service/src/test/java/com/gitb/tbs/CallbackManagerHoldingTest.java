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

import com.gitb.engine.CallbackManager;
import com.gitb.engine.TestEngineConfiguration;
import com.gitb.engine.TestbedService;
import com.gitb.messaging.Message;
import com.gitb.messaging.callback.CallbackData;
import com.gitb.messaging.callback.CallbackType;
import com.gitb.messaging.callback.SessionCallbackData;
import org.apache.pekko.actor.ActorRef;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.gitb.tbs.TdlTestHelper.runAsync;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Directly exercises {@link CallbackManager}'s handling of incoming calls that cannot be immediately matched to
 * a parked receive step: precise (non-consuming, non-stealing) matching, FIFO handoff, the concurrently-held
 * call cap, and the {@code sessionEnded} scoping fix. Each test backs its {@code CallbackManager} calls with a
 * real, independently active test session (via {@link TdlTestHelper#runAsync}) so that the "is there an active
 * session for this system API key" gate in {@code lookupHandlingData}/{@code sessionEnded} is exercised
 * realistically rather than mocked.
 */
class CallbackManagerHoldingTest extends BaseIntegrationTest {

    @BeforeAll
    static void stubHolderFixture() {
        stubTdl("msg-cbm-holder", "tdl/msg/msg-cbm-holder.xml");
    }

    private void stopQuietly(String sessionId) {
        try {
            TestbedService.stop(sessionId, false);
        } catch (Exception e) {
            // Best-effort cleanup only.
        }
    }

    @Test
    void pendingCallMatchingIsPreciseAndNonConsuming() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var run = runAsync("msg-cbm-holder", apiKey, List.of());
        try {
            CallbackManager manager = CallbackManager.getInstance();
            Function<Message, Boolean> wantsA = msg -> "A".equals(msg.getFragments().get("marker").toString());
            Function<Message, Boolean> wantsB = msg -> "B".equals(msg.getFragments().get("marker").toString());

            // No matching registration yet, but a session for this API key is active - the call is held.
            var futureA = manager.lookupHandlingData(CallbackType.HTTP, apiKey, wantsA);
            assertFalse(futureA.isDone(), "Call should be held pending a matching registration");

            // A registration that does NOT satisfy futureA's criteria must not steal it.
            manager.registerCallbackData(new SessionCallbackData(run.sessionId(), "call-B", apiKey, new CallbackData(messageWithMarker("B"), CallbackType.HTTP)));
            assertFalse(futureA.isDone(), "Held call must not be claimed by a registration that does not match its criteria");

            // That non-matching registration must still be independently reachable by a lookup that does fit it.
            var futureB = manager.lookupHandlingData(CallbackType.HTTP, apiKey, wantsB);
            assertTrue(futureB.isDone());
            Optional<SessionCallbackData> resultB = futureB.get();
            assertTrue(resultB.isPresent());
            assertEquals("call-B", resultB.get().callId());

            // A second, immediately following lookup with the same criteria must NOT also match call-B - proves
            // the fix for lookupHandlingData not consuming/claiming the entry it returns.
            var futureB2 = manager.lookupHandlingData(CallbackType.HTTP, apiKey, wantsB);
            assertFalse(futureB2.isDone(), "A claimed registration must not be matchable a second time");

            // Finally, a registration that DOES satisfy futureA's criteria must claim it.
            manager.registerCallbackData(new SessionCallbackData(run.sessionId(), "call-A", apiKey, new CallbackData(messageWithMarker("A"), CallbackType.HTTP)));
            Optional<SessionCallbackData> resultA = futureA.get(2, TimeUnit.SECONDS);
            assertTrue(resultA.isPresent());
            assertEquals("call-A", resultA.get().callId());
        } finally {
            stopQuietly(run.sessionId());
        }
    }

    @Test
    void pendingCallsAreServedInArrivalOrder() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var run = runAsync("msg-cbm-holder", apiKey, List.of());
        try {
            CallbackManager manager = CallbackManager.getInstance();
            Function<Message, Boolean> matchesAny = msg -> true;
            var future1 = manager.lookupHandlingData(CallbackType.HTTP, apiKey, matchesAny);
            var future2 = manager.lookupHandlingData(CallbackType.HTTP, apiKey, matchesAny);
            assertFalse(future1.isDone());
            assertFalse(future2.isDone());

            manager.registerCallbackData(new SessionCallbackData(run.sessionId(), "call-1", apiKey, new CallbackData(new Message(), CallbackType.HTTP)));
            Optional<SessionCallbackData> result1 = future1.get(2, TimeUnit.SECONDS);
            assertTrue(result1.isPresent());
            assertEquals("call-1", result1.get().callId(), "The oldest held call should be served first");
            assertFalse(future2.isDone(), "The second held call must still be waiting");

            manager.registerCallbackData(new SessionCallbackData(run.sessionId(), "call-2", apiKey, new CallbackData(new Message(), CallbackType.HTTP)));
            Optional<SessionCallbackData> result2 = future2.get(2, TimeUnit.SECONDS);
            assertTrue(result2.isPresent());
            assertEquals("call-2", result2.get().callId());
        } finally {
            stopQuietly(run.sessionId());
        }
    }

    @Test
    void pendingCallCountIsBounded() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var run = runAsync("msg-cbm-holder", apiKey, List.of());
        long originalTimeout = TestEngineConfiguration.CALLBACK_WAIT_TIMEOUT;
        int originalLimit = TestEngineConfiguration.CALLBACK_WAIT_LIMIT;
        TestEngineConfiguration.CALLBACK_WAIT_TIMEOUT = 300;
        TestEngineConfiguration.CALLBACK_WAIT_LIMIT = 2;
        try {
            CallbackManager manager = CallbackManager.getInstance();
            Function<Message, Boolean> matchesNothing = msg -> false;
            var future1 = manager.lookupHandlingData(CallbackType.HTTP, apiKey, matchesNothing);
            var future2 = manager.lookupHandlingData(CallbackType.HTTP, apiKey, matchesNothing);
            var future3 = manager.lookupHandlingData(CallbackType.HTTP, apiKey, matchesNothing);
            assertFalse(future1.isDone(), "First call should be held");
            assertFalse(future2.isDone(), "Second call should be held");
            assertTrue(future3.isDone(), "Third call should be rejected immediately once the cap is reached");
            assertTrue(future3.get().isEmpty());
            // The two held calls self-expire via the (here, short) configured wait window - proving the cap
            // does not leak entries and normal expiry still applies to calls admitted under it.
            assertTrue(future1.get(2, TimeUnit.SECONDS).isEmpty());
            assertTrue(future2.get(2, TimeUnit.SECONDS).isEmpty());
        } finally {
            TestEngineConfiguration.CALLBACK_WAIT_TIMEOUT = originalTimeout;
            TestEngineConfiguration.CALLBACK_WAIT_LIMIT = originalLimit;
            stopQuietly(run.sessionId());
        }
    }

    @Test
    void sessionEndedDropsHeldCallOnceItWasTheLastActiveSessionForTheSystem() throws Exception {
        String apiKey = UUID.randomUUID().toString();
        var run = runAsync("msg-cbm-holder", apiKey, List.of());
        try {
            CallbackManager manager = CallbackManager.getInstance();
            var pending = manager.lookupHandlingData(CallbackType.HTTP, apiKey, msg -> true);
            assertFalse(pending.isDone());

            manager.sessionEnded(run.sessionId());

            // This was the only (and now ending) active session for the system API key, so the held call - even
            // though it was never itself registered by this session - can never be matched anymore.
            assertTrue(pending.get(2, TimeUnit.SECONDS).isEmpty());
        } finally {
            stopQuietly(run.sessionId());
        }
    }

    @Test
    void sessionEndedDoesNotRemoveOtherSessionsRegisteredCalls() throws Exception {
        String sharedApiKey = UUID.randomUUID().toString();
        var run1 = runAsync("msg-cbm-holder", sharedApiKey, List.of());
        var run2 = runAsync("msg-cbm-holder", sharedApiKey, List.of());
        try {
            CallbackManager manager = CallbackManager.getInstance();
            manager.registerForNotification(ActorRef.noSender(), run1.sessionId(), "call-1");
            manager.registerCallbackData(new SessionCallbackData(run1.sessionId(), "call-1", sharedApiKey, new CallbackData(new Message(), CallbackType.HTTP)));
            manager.registerForNotification(ActorRef.noSender(), run2.sessionId(), "call-2");
            manager.registerCallbackData(new SessionCallbackData(run2.sessionId(), "call-2", sharedApiKey, new CallbackData(new Message(), CallbackType.HTTP)));

            manager.sessionEnded(run1.sessionId());

            // Session 2 is still active and its own registered call must survive - pre-fix, sessionEnded(run1)
            // wiped the *entire* systemToCallMap bucket for the shared API key, including session 2's entry.
            var lookup = manager.lookupHandlingData(CallbackType.HTTP, sharedApiKey, msg -> true);
            assertTrue(lookup.isDone());
            Optional<SessionCallbackData> result = lookup.get();
            assertTrue(result.isPresent());
            assertEquals("call-2", result.get().callId());
        } finally {
            stopQuietly(run1.sessionId());
            stopQuietly(run2.sessionId());
        }
    }

    private static Message messageWithMarker(String marker) {
        Message msg = new Message();
        msg.addInput("marker", new com.gitb.types.StringType(marker));
        return msg;
    }

}
