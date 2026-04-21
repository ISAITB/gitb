/*
 * Copyright (C) 2026 European Union
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

import com.gitb.core.ErrorCode;
import com.gitb.engine.ModuleManager;
import com.gitb.engine.SessionManager;
import com.gitb.engine.TestServiceInformation;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.utils.HandlerUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.processing.IProcessingHandler;
import com.gitb.remote.ClientConfiguration;
import com.gitb.remote.processing.RemoteProcessingModuleClient;
import com.gitb.remote.processing.RemoteProcessingModuleRestClient;
import com.gitb.tdl.HandlerApiType;
import com.gitb.utils.ErrorUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Properties;

public final class ProcessingContext {

    private final IProcessingHandler handler;
    private String session;
    private final String testSessionId;
    private final Long handlerTimeout;
    private final HandlerApiType declaredHandlerApiType;

    public ProcessingContext(String handler, String handlerDomainIdentifier, Properties transactionProperties, String testSessionId, Long handlerTimeout, HandlerApiType declaredHandlerApiType) {
        this.handlerTimeout = handlerTimeout;
        this.testSessionId = testSessionId;
        this.declaredHandlerApiType = declaredHandlerApiType;
        this.handler = resolveHandler(handler, handlerDomainIdentifier, transactionProperties, testSessionId);
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

    private IProcessingHandler resolveHandler(String handler, String handlerDomainIdentifier, Properties transactionProperties, String testSessionId) {
        if (isURL(handler)) {
            return getRemoteProcessor(handler, handlerDomainIdentifier, transactionProperties, testSessionId);
        } else {
            return ModuleManager.getInstance().getProcessingHandler(handler);
        }
    }

    private IProcessingHandler getRemoteProcessor(String handler, String handlerDomainIdentifier, Properties transactionProperties, String testSessionId) {
        TestCaseContext context = SessionManager.getInstance().getContext(testSessionId);
        TestServiceInformation serviceInformation = context.getRegisteredTestServiceInformation(handlerDomainIdentifier);
        HandlerApiType apiType = HandlerUtils.determineHandlerApiType(serviceInformation, declaredHandlerApiType);
        try {
            switch (apiType) {
                case REST -> {
                    return new RemoteProcessingModuleRestClient(
                            URI.create(handler),
                            context.prepareRemoteServiceCallProperties(transactionProperties, serviceInformation),
                            testSessionId,
                            context.getTestCaseIdentifier(),
                            () -> new ClientConfiguration(handlerTimeout)
                    );
                }
                case SOAP -> {
                    return new RemoteProcessingModuleClient(
                            URI.create(handler).toURL(),
                            context.prepareRemoteServiceCallProperties(transactionProperties, serviceInformation),
                            testSessionId,
                            context.getTestCaseIdentifier(),
                            new ClientConfiguration(handlerTimeout)
                    );
                }
                default -> throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Unsupported handler API type [%s]".formatted(apiType)));
            }
        } catch (MalformedURLException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote processing module found with an malformed URL [%s]".formatted(handler)), e);
        }
    }

}
