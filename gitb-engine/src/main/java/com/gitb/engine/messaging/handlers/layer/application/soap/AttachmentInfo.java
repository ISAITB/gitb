package com.gitb.engine.messaging.handlers.layer.application.soap;

import com.gitb.types.DataType;

public record AttachmentInfo(String name, String contentType, DataType content, boolean isText) {
}
