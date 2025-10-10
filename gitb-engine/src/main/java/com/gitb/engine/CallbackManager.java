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
import java.util.function.Function;

public class CallbackManager {

    private static final CallbackManager INSTANCE = new CallbackManager();
    private static final Logger LOG = LoggerFactory.getLogger(CallbackManager.class);

    private final Map<String, Set<String>> sessionToCallMap = new HashMap<>();
    private final Map<String, ActorRef> callToActorMap = new HashMap<>();

    private final Map<String, SessionCallbackData> callToDataMap = new HashMap<>();
    private final Map<String, Set<String>> systemToCallMap = new HashMap<>();

    private final Object mutex = new Object();

    private CallbackManager() {
    }

    public static CallbackManager getInstance() {
        return INSTANCE;
    }

    public void registerCallbackData(SessionCallbackData data) {
        synchronized (mutex) {
            callToDataMap.put(data.callId(), data);
            Set<String> existingCallIds = systemToCallMap.computeIfAbsent(data.systemApiKey(), (k) -> new HashSet<>());
            existingCallIds.add(data.callId());
        }
    }

    public Optional<SessionCallbackData> lookupHandlingData(CallbackType type, String systemApiKey, Function<Message, Boolean> matchFunction) {
        synchronized (mutex) {
            if (systemToCallMap.containsKey(systemApiKey)) {
                return systemToCallMap.get(systemApiKey)
                        .stream().filter(callId -> {
                            var callbackData = callToDataMap.get(callId);
                            return callbackData != null && callbackData.data().type() == type && matchFunction.apply(callbackData.data().inputs());
                        })
                        .findFirst()
                        .map(callToDataMap::get);
            }
            return Optional.empty();
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
                systemToCallMap.remove(data.systemApiKey());
            }
        }
    }

    public void sessionEnded(String sessionId) {
        synchronized (mutex) {
            if (sessionToCallMap.containsKey(sessionId)) {
                for (String callId: sessionToCallMap.get(sessionId)) {
                    callToActorMap.remove(callId);
                    var data = callToDataMap.remove(callId);
                    if (data != null) {
                        systemToCallMap.remove(data.systemApiKey());
                    }
                }
                sessionToCallMap.remove(sessionId);
            }
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
