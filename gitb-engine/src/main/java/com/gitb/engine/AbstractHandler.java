package com.gitb.engine;

import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.types.DataType;

import java.util.Map;

public abstract class AbstractHandler {

    protected TestCaseScope getScope(String sessionId) {
        return SessionManager.getInstance().getContext(sessionId).getScope();
    }

    protected static <T extends DataType> T getAndConvert(Map<String, DataType> inputs, String inputName, String dataType, Class<T> dataTypeClass) {
        return MessagingHandlerUtils.getAndConvert(inputs, inputName, dataType, dataTypeClass);
    }

}
