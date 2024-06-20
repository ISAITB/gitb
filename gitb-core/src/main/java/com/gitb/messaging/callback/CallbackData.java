package com.gitb.messaging.callback;

import com.gitb.messaging.Message;

public record CallbackData(Message inputs, CallbackType type) {
}
