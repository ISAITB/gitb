package com.gitb.messaging.callback;

public record SessionCallbackData(String sessionId, String callId, String systemApiKey, CallbackData data) {
}
