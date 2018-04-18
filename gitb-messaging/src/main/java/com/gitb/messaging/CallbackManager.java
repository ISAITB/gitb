package com.gitb.messaging;

import com.gitb.messaging.utils.MessagingHandlerUtils;
import com.gitb.ms.NotifyForMessageRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

/**
 * Created by simatosc on 25/11/2016.
 */
public class CallbackManager {

    private static CallbackManager INSTANCE = new CallbackManager();
    private ConcurrentMap<String, CallbackData> callbackLocks = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Set<String>> sessionToCallMap = new ConcurrentHashMap<>();
    private final Object mutex = new Object();

    private CallbackManager() {
    }

    public static CallbackManager getInstance() {
        return INSTANCE;
    }

    public void callbackReceived(String sessionId, String callId, MessagingReport result) {
        synchronized (mutex) {
            if (sessionId != null) {
                List<CallbackData> callbacks = getLock(sessionId, callId);
                if (callbacks != null) {
                    for (CallbackData data: callbacks) {
                        // We are assigning the result to many because we have no way of knowing which call this refers to (i.e. we had parallel calls in one session).
                        data.data = result;
                        data.lock.release();
                    }
                }
            }
        }
    }

    public void callbackReceived(NotifyForMessageRequest receivedCallback) {
        callbackReceived(receivedCallback.getSessionId(), receivedCallback.getCallId(), MessagingHandlerUtils.getMessagingReport(receivedCallback.getReport()));
    }

    private void createLock(String sessionId, String callId) {
        synchronized (mutex) {
            Set<String> existingSessionCallIds = sessionToCallMap.get(sessionId);
            List<CallbackData> callbacks = new ArrayList<>();
            if (existingSessionCallIds == null) {
                existingSessionCallIds = new HashSet<>();
                sessionToCallMap.put(sessionId, existingSessionCallIds);
            }
            String mapKey = sessionId+"|"+callId;
            Semaphore lock = new Semaphore(0, true);
            existingSessionCallIds.add(mapKey);
            CallbackData callback = new CallbackData(lock);
            callback.callId = mapKey;
            callbackLocks.put(mapKey, callback);
            callbacks.add(callback);
        }
    }

    private List<CallbackData> getLock(String sessionId, String callId) {
        synchronized (mutex) {
            List<CallbackData> callbacks = new ArrayList<>();
            if (sessionToCallMap.containsKey(sessionId)) {
                Set<String> existingSessionCallIds = sessionToCallMap.get(sessionId);
                if (callId != null) {
                    CallbackData callback = callbackLocks.get(sessionId+"|"+callId);
                    if (callback != null) {
                        callbacks.add(callback);
                    }
                } else {
                    for (String id: existingSessionCallIds) {
                        callbacks.add(callbackLocks.get(id));
                    }
                }
            }
            return callbacks;
        }
    }

    public void prepareForCallback(String sessionId, String callId) {
        createLock(sessionId, callId);
    }

    public MessagingReport waitForCallback(String sessionId, String callId) {
        try {
            List<CallbackData> callbacks = getLock(sessionId, callId);
            // A wait will always be returning a single callback (i.e. we always know the call ID).
            callbacks.get(0).lock.acquire();
            MessagingReport response = callbacks.get(0).data;
            for (CallbackData callback: callbacks) {
                cleanup(sessionId, callback);
            }
            return response;
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void cleanup(String sessionId, CallbackData data) {
        synchronized (mutex) {
            callbackLocks.remove(data.callId);
            if (sessionToCallMap.containsKey(sessionId)) {
                Set<String> callIds = sessionToCallMap.get(sessionId);
                if (callIds != null) {
                    callIds.remove(data.callId);
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
                    CallbackData data = callbackLocks.remove(callId);
                    if (data != null) {
                        data.lock.release();
                    }
                }
            }
        }
    }

    static class CallbackData {

        Semaphore lock;
        String callId;
        MessagingReport data;

        CallbackData(Semaphore lock) {
            this.lock = lock;
        }

    }

}
