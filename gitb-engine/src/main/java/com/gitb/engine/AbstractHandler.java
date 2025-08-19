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

import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tr.ObjectFactory;
import com.gitb.types.DataType;

import java.util.Map;

public abstract class AbstractHandler {

    protected final ObjectFactory objectFactory = new ObjectFactory();

    protected TestCaseScope getScope(String sessionId) {
        return SessionManager.getInstance().getContext(sessionId).getScope();
    }

    protected static <T extends DataType> T getAndConvert(Map<String, DataType> inputs, String inputName, String dataType, Class<T> dataTypeClass) {
        return MessagingHandlerUtils.getAndConvert(inputs, inputName, dataType, dataTypeClass);
    }

}
