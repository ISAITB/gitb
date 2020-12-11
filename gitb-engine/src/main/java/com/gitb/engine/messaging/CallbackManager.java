package com.gitb.engine.messaging;

import akka.actor.ActorRef;
import com.gitb.engine.commands.messaging.NotificationReceived;
import com.gitb.messaging.MessagingReport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CallbackManager {

    private static final CallbackManager INSTANCE = new CallbackManager();

    private final ConcurrentMap<String, Set<String>> sessionToCallMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ActorRef> callToActorMap = new ConcurrentHashMap<>();
    private final Object mutex = new Object();

    private CallbackManager() {
    }

    public static CallbackManager getInstance() {
        return INSTANCE;
    }

    public void registerForNotification(ActorRef actor, String sessionId, String callId) {
        synchronized (mutex) {
            Set<String> existingSessionCallIds = sessionToCallMap.computeIfAbsent(sessionId, k -> new HashSet<>());
            existingSessionCallIds.add(callId);
            callToActorMap.put(callId, actor);
        }
    }


    public void callbackReceived(String sessionId, String callId, MessagingReport result) {
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
                            actor.tell(new NotificationReceived(result), ActorRef.noSender());
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
            callToActorMap.remove(callId);
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
        }
    }

    public void sessionEnded(String sessionId) {
        synchronized (mutex) {
            if (sessionToCallMap.containsKey(sessionId)) {
                for (String callId: sessionToCallMap.get(sessionId)) {
                    callToActorMap.remove(callId);
                }
                sessionToCallMap.remove(sessionId);
            }
        }
    }

}
