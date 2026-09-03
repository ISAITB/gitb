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

package com.gitb.engine;

import com.gitb.core.LogLevel;
import com.gitb.engine.commands.messaging.NotificationReceived;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.messaging.callback.CallbackType;
import com.gitb.messaging.callback.SessionCallbackData;
import org.apache.pekko.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CallbackManager {

    private static final CallbackManager INSTANCE = new CallbackManager();
    private static final Logger LOG = LoggerFactory.getLogger(CallbackManager.class);

    private final Map<String, Set<String>> sessionToCallMap = new HashMap<>();
    private final Map<String, ActorRef> callToActorMap = new HashMap<>();

    private final Map<String, SessionCallbackData> callToDataMap = new HashMap<>();
    private final Map<String, Set<String>> systemToCallMap = new HashMap<>();

    /*
     * Incoming calls that arrived before a matching receive step had registered its callback data. Kept per
     * system API key, in arrival order, so that when a matching step registers we hand it the oldest waiting
     * call first (FIFO). Each pending call is released - i.e. its future completed - exactly once, either
     * because a matching registration claims it (registerCallbackData), its wait window elapses
     * (lookupHandlingData), or the last active session for its system API key ends (sessionEnded).
     */
    private final Map<String, LinkedList<PendingCall>> pendingCalls = new HashMap<>();
    private int pendingCallCount;

    private final Object mutex = new Object();

    private record PendingCall(CallbackType type, String systemApiKey, Function<Message, Boolean> matchFunction,
                                CompletableFuture<Optional<SessionCallbackData>> result, long createdAt) {
    }

    private CallbackManager() {
    }

    public static CallbackManager getInstance() {
        return INSTANCE;
    }

    public void registerCallbackData(SessionCallbackData data) {
        synchronized (mutex) {
            // First check whether an incoming call is already being held awaiting exactly this registration.
            if (claimPendingCall(data)) {
                return;
            }
            callToDataMap.put(data.callId(), data);
            Set<String> existingCallIds = systemToCallMap.computeIfAbsent(data.systemApiKey(), (k) -> new LinkedHashSet<>());
            existingCallIds.add(data.callId());
        }
    }

    /**
     * Looks up the callback data to handle an incoming call. If no test step is currently parked to receive it,
     * the call is held (up to {@link TestEngineConfiguration#CALLBACK_WAIT_TIMEOUT}) so that a step reached
     * shortly afterwards can still serve it - this avoids a race whereby an incoming call sent by a system under
     * test reaches the test engine just before the corresponding {@code receive} test step is executed.
     *
     * @param type The type of callback expected (HTTP or SOAP).
     * @param systemApiKey The system API key extracted from the incoming call's path.
     * @param matchFunction Additional matching criteria (e.g. HTTP method, URI extension) to apply to a candidate.
     * @return A future that completes with the matched callback data, or with an empty result if the call could
     * not (or could no longer) be matched.
     */
    public CompletableFuture<Optional<SessionCallbackData>> lookupHandlingData(CallbackType type, String systemApiKey, Function<Message, Boolean> matchFunction) {
        synchronized (mutex) {
            Optional<SessionCallbackData> immediateMatch = findAndClaimMatch(type, systemApiKey, matchFunction);
            if (immediateMatch.isPresent()) {
                return CompletableFuture.completedFuture(immediateMatch);
            }
            long waitTimeout = TestEngineConfiguration.CALLBACK_WAIT_TIMEOUT;
            if (waitTimeout <= 0) {
                // Holding incoming calls is disabled - preserve the previous immediate-rejection behaviour.
                return CompletableFuture.completedFuture(Optional.empty());
            }
            if (!SessionManager.getInstance().hasActiveSessionForSystem(systemApiKey)) {
                // No active test session is even configured with this system API key - this call can never be matched.
                return CompletableFuture.completedFuture(Optional.empty());
            }
            if (pendingCallCount >= TestEngineConfiguration.CALLBACK_WAIT_LIMIT) {
                LOG.warn("Rejecting an incoming call for system API key [{}] as the maximum number of concurrently held calls ({}) has been reached.", systemApiKey, TestEngineConfiguration.CALLBACK_WAIT_LIMIT);
                return CompletableFuture.completedFuture(Optional.empty());
            }
            var pending = new PendingCall(type, systemApiKey, matchFunction, new CompletableFuture<>(), System.currentTimeMillis());
            pendingCalls.computeIfAbsent(systemApiKey, k -> new LinkedList<>()).add(pending);
            pendingCallCount++;
            // Whatever completes the future (a matching registration, our own timeout, or a session ending), always clean up.
            pending.result().whenComplete((result, error) -> deregisterPending(pending));
            pending.result().completeOnTimeout(Optional.empty(), waitTimeout, TimeUnit.MILLISECONDS);
            return pending.result();
        }
    }

    /**
     * Looks for a registered callback matching the given criteria and, if found, claims it - i.e. removes it from
     * the registry - so that it cannot also be matched by a concurrently arriving call.
     * <br>
     * Must be called while holding {@link #mutex}.
     */
    private Optional<SessionCallbackData> findAndClaimMatch(CallbackType type, String systemApiKey, Function<Message, Boolean> matchFunction) {
        Set<String> callIds = systemToCallMap.get(systemApiKey);
        if (callIds == null) {
            return Optional.empty();
        }
        Iterator<String> iterator = callIds.iterator();
        while (iterator.hasNext()) {
            String callId = iterator.next();
            var data = callToDataMap.get(callId);
            if (data != null && data.data().type() == type && matchFunction.apply(data.data().inputs())) {
                iterator.remove();
                if (callIds.isEmpty()) {
                    systemToCallMap.remove(systemApiKey);
                }
                callToDataMap.remove(callId);
                return Optional.of(data);
            }
        }
        return Optional.empty();
    }

    /**
     * Looks for a held incoming call matching the callback data just registered by a test step and, if found,
     * completes it directly (bypassing {@link #systemToCallMap}/{@link #callToDataMap} entirely since the call
     * is served immediately).
     * <br>
     * Must be called while holding {@link #mutex}.
     *
     * @return {@code true} if a held call was matched and completed.
     */
    private boolean claimPendingCall(SessionCallbackData data) {
        LinkedList<PendingCall> pendingForSystem = pendingCalls.get(data.systemApiKey());
        if (pendingForSystem == null) {
            return false;
        }
        PendingCall matched = null;
        for (PendingCall pending : pendingForSystem) {
            if (pending.type() == data.data().type() && pending.matchFunction().apply(data.data().inputs())) {
                matched = pending;
                break;
            }
        }
        if (matched == null) {
            return false;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(MarkerFactory.getDetachedMarker(data.sessionId()), "Matched an incoming call that had been held for [{}] ms while waiting for the corresponding receive step to be reached.", System.currentTimeMillis() - matched.createdAt());
        }
        // Completing this triggers the pending call's own whenComplete callback (deregisterPending), which
        // removes it from pendingForSystem - do this after we've stopped iterating over that same list.
        matched.result().complete(Optional.of(data));
        return true;
    }

    private void deregisterPending(PendingCall pending) {
        synchronized (mutex) {
            var pendingForSystem = pendingCalls.get(pending.systemApiKey());
            if (pendingForSystem != null && pendingForSystem.remove(pending)) {
                pendingCallCount--;
                if (pendingForSystem.isEmpty()) {
                    pendingCalls.remove(pending.systemApiKey());
                }
            }
        }
    }

    public void registerForNotification(ActorRef actor, String sessionId, String callId) {
        synchronized (mutex) {
            Set<String> existingSessionCallIds = sessionToCallMap.computeIfAbsent(sessionId, k -> new HashSet<>());
            existingSessionCallIds.add(callId);
            callToActorMap.put(callId, actor);
        }
    }

    public void callbackReceived(String sessionId, String callId, Exception error) {
        callbackReceived(sessionId, callId, null, error);
    }

    public void callbackReceived(String sessionId, String callId, MessagingReport result) {
        callbackReceived(sessionId, callId, result, null);
    }

    private void callbackReceived(String sessionId, String callId, MessagingReport result, Exception error) {
        synchronized (mutex) {
            if (sessionToCallMap.containsKey(sessionId)) {
                // Step 1 - Get the calls that are linked to this notification.
                List<String> relevantCallIds = new ArrayList<>();
                if (callId != null) {
                    relevantCallIds.add(callId);
                } else {
                    Set<String> existingSessionCallIds = sessionToCallMap.get(sessionId);
                    if (existingSessionCallIds != null) {
                        relevantCallIds.addAll(existingSessionCallIds);
                    }
                }
                // Step 2 - Get the actors linked to the calls.
                List<ActorRef> actorsToNotify = new ArrayList<>();
                for (String relevantCallId: relevantCallIds) {
                    ActorRef actor = callToActorMap.get(relevantCallId);
                    if (actor != null) {
                        actorsToNotify.add(actor);
                    }
                }
                // Step 3 - Send a notification message to the actors.
                try {
                    for (ActorRef actor: actorsToNotify) {
                        if (!actor.isTerminated()) {
                            actor.tell(new NotificationReceived(result, error), ActorRef.noSender());
                        }
                    }
                } finally {
                    // Step 4 - Cleanup.
                    for (String relevantCallId: relevantCallIds) {
                        cleanup(sessionId, relevantCallId);
                    }
                }
            }
        }
    }

    private void cleanup(String sessionId, String callId) {
        synchronized (mutex) {
            if (sessionToCallMap.containsKey(sessionId)) {
                Set<String> callIds = sessionToCallMap.get(sessionId);
                if (callIds != null) {
                    callIds.remove(callId);
                    if (callIds.isEmpty()) {
                        sessionToCallMap.remove(sessionId);
                    }
                } else {
                    sessionToCallMap.remove(sessionId);
                }
            }
            callToActorMap.remove(callId);
            var data = callToDataMap.remove(callId);
            if (data != null) {
                var calls = systemToCallMap.get(data.systemApiKey());
                if (calls != null) {
                    calls.remove(callId);
                }
                if (calls == null || calls.isEmpty()) {
                    systemToCallMap.remove(data.systemApiKey());
                }
            }
        }
    }

    public void sessionEnded(String sessionId) {
        Set<String> affectedSystemApiKeys = new HashSet<>();
        synchronized (mutex) {
            Set<String> callIds = sessionToCallMap.remove(sessionId);
            if (callIds != null) {
                for (String callId : callIds) {
                    callToActorMap.remove(callId);
                    var data = callToDataMap.remove(callId);
                    if (data != null) {
                        // Only remove this session's own call ids - other still-running sessions may be using
                        // the same system API key and must keep their own parked calls registered.
                        var systemCallIds = systemToCallMap.get(data.systemApiKey());
                        if (systemCallIds != null) {
                            systemCallIds.remove(callId);
                            if (systemCallIds.isEmpty()) {
                                systemToCallMap.remove(data.systemApiKey());
                            }
                        }
                        affectedSystemApiKeys.add(data.systemApiKey());
                    }
                }
            }
        }
        // Also consider the session's own configured system API key, even if it never itself registered a call
        // (e.g. it ended before ever reaching its receive step) - it may still be the last session "holding
        // open" a pending call for that system that would otherwise only be dropped once its wait window elapses.
        var context = SessionManager.getInstance().getContext(sessionId);
        if (context != null && context.getSystemApiKey() != null) {
            affectedSystemApiKeys.add(context.getSystemApiKey());
        }
        // If this was the last active session for a given system API key, no held incoming call for that key
        // can ever be matched anymore - drop them now rather than making the caller wait out the full window.
        for (String systemApiKey : affectedSystemApiKeys) {
            if (!SessionManager.getInstance().hasActiveSessionForSystem(systemApiKey, sessionId)) {
                dropPendingCalls(systemApiKey);
            }
        }
    }

    private void dropPendingCalls(String systemApiKey) {
        List<PendingCall> toDrop;
        synchronized (mutex) {
            var pendingForSystem = pendingCalls.get(systemApiKey);
            if (pendingForSystem == null || pendingForSystem.isEmpty()) {
                return;
            }
            toDrop = new ArrayList<>(pendingForSystem);
        }
        // Complete outside the lock covering the copy above; each completion re-enters the lock individually
        // via deregisterPending (triggered by the whenComplete callback registered in lookupHandlingData).
        for (PendingCall pending : toDrop) {
            pending.result().complete(Optional.empty());
        }
    }

    public void logMessageReceived(String testSessionId, String message, LogLevel level) {
        if (testSessionId == null) {
            LOG.warn("Received log message but no session ID was provided");
        } else {
            if (SessionManager.getInstance().exists(testSessionId)) {
                if (message != null && !message.isBlank()) {
                    if (level == LogLevel.ERROR) {
                        LOG.error(MarkerFactory.getDetachedMarker(testSessionId), message);
                    } else if (level == LogLevel.WARNING) {
                        LOG.warn(MarkerFactory.getDetachedMarker(testSessionId), message);
                    } else if (level == LogLevel.INFO) {
                        LOG.info(MarkerFactory.getDetachedMarker(testSessionId), message);
                    } else {
                        LOG.debug(MarkerFactory.getDetachedMarker(testSessionId), message);
                    }
                } else {
                    LOG.warn("Received blank log message for test session [{}]", testSessionId);
                }
            } else {
                LOG.warn("Received log message for unknown session ID [{}]", testSessionId);
            }
        }
    }

}
