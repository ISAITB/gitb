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

package com.gitb.engine.processing;

import com.gitb.engine.ModuleManager;
import com.gitb.core.ErrorCode;
import com.gitb.engine.remote.ClientConfiguration;
import com.gitb.engine.remote.processing.RemoteProcessingModuleClient;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.processing.IProcessingHandler;
import com.gitb.utils.ErrorUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public final class ProcessingContext {

    private final IProcessingHandler handler;
    private String session;
    private final String testSessionId;
    private final Long handlerTimeout;

    public ProcessingContext(String handler, Properties transactionProperties, String testSessionId, Long handlerTimeout) {
        this.handlerTimeout = handlerTimeout;
        this.testSessionId = testSessionId;
        this.handler = resolveHandler(handler, transactionProperties, testSessionId);
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getSession() {
        if (session == null) {
            return testSessionId;
        }
        return session;
    }

    public IProcessingHandler getHandler() {
        return handler;
    }

    private boolean isURL(String handler) {
        try {
            new URI(handler).toURL();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private IProcessingHandler resolveHandler(String handler, Properties transactionProperties, String testSessionId) {
        if (isURL(handler)) {
            return getRemoteProcessor(handler, transactionProperties, testSessionId);
        } else {
            return ModuleManager.getInstance().getProcessingHandler(handler);
        }
    }

    private IProcessingHandler getRemoteProcessor(String handler, Properties transactionProperties, String testSessionId) {
        try {
            return new RemoteProcessingModuleClient(
                    new URI(handler).toURL(),
                    transactionProperties,
                    testSessionId,
                    new ClientConfiguration(handlerTimeout)
            );
        } catch (MalformedURLException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote processing module found with an malformed URL [" + handler + "]"), e);
        } catch (URISyntaxException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote processing module found with an invalid URI syntax [" + handler + "]"), e);
        }
    }

}
