package com.gitb.engine.remote.messaging;

import com.gitb.ms.NotifyForMessageRequest;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

/**
 * Created by simatosc on 25/11/2016.
 */
public class CallbackManager {

    private static CallbackManager INSTANCE = new CallbackManager();
    private ConcurrentMap<String, CallbackData> callbackLocks = new ConcurrentHashMap<>();

    private CallbackManager() {
    }

    public static CallbackManager getInstance() {
        return INSTANCE;
    }

    public void callbackReceived(NotifyForMessageRequest receivedCallback) {
        if (receivedCallback.getSessionId() != null) {
            CallbackData data = callbackLocks.get(receivedCallback.getSessionId());
            if (data != null) {
                data.data = receivedCallback;
                data.lock.release();
            }
        }
    }

    public NotifyForMessageRequest waitForCallback(String sessionId) {
        Semaphore lock = new Semaphore(0, true);
        try {
            callbackLocks.put(sessionId, new CallbackData(lock));
            lock.acquire();

            CallbackData callbackData = callbackLocks.get(sessionId);
            NotifyForMessageRequest response = null;
            if (callbackData != null) {
                response = callbackData.data;
                callbackLocks.remove(sessionId);
            }
            return response;
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public void sessionEnded(String sessionId) {
        CallbackData data = callbackLocks.remove(sessionId);
        if (data != null) {
            data.lock.release();
        }
    }

    static class CallbackData {

        Semaphore lock;
        NotifyForMessageRequest data;

        CallbackData(Semaphore lock) {
            this.lock = lock;
        }

    }

}
