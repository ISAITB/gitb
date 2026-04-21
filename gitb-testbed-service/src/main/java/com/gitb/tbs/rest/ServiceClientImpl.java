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

package com.gitb.tbs.rest;

import com.gitb.engine.CallbackManager;
import com.gitb.engine.SessionManager;
import com.gitb.model.core.LogLevel;
import com.gitb.model.core.LogRequest;
import com.gitb.model.core.ServiceClient;
import com.gitb.model.ms.MessagingClient;
import com.gitb.model.ms.NotifyForMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.gitb.utils.MessagingReportUtils.getMessagingReport;
import static com.gitb.utils.ModelUtils.fromModel;

/**
 * REST endpoint implementation for service-originated callbacks and log events.
 * <p>
 * This controller bridges incoming API model payloads to the internal callback and
 * reporting structures used by the testbed runtime.
 */
@RestController
public class ServiceClientImpl implements MessagingClient, ServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceClientImpl.class);

    /**
     * Receives asynchronous messaging callback notifications from services.
     *
     * @param parameters the callback payload including session/call identifiers and report data
     */
    @Override
    @PostMapping(path = "/gitb/notifyForMessage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void notifyForMessage(@RequestBody NotifyForMessageRequest parameters) {
        CallbackManager.getInstance().callbackReceived(
                parameters.getSessionId(),
                parameters.getCallId(),
                getMessagingReport(fromModel(parameters.getReport())));
    }

    /**
     * Receives log events emitted by a service and routes them to the related test session.
     *
     * @param logRequest the incoming log payload
     */
    @Override
    @PostMapping(path = "/gitb/log")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void log(@RequestBody LogRequest logRequest) {
        if (logRequest.getSessionId() != null) {
            String testSessionId = determineTestSessionId(logRequest);
            if (testSessionId == null) {
                LOG.warn("Could not determine test session for ID [{}]", logRequest.getSessionId());
            } else {
                var level = switch (logRequest.getLevel()) {
                    case LogLevel.ERROR -> com.gitb.core.LogLevel.ERROR;
                    case LogLevel.WARNING -> com.gitb.core.LogLevel.WARNING;
                    case LogLevel.DEBUG -> com.gitb.core.LogLevel.DEBUG;
                    case null, default -> com.gitb.core.LogLevel.INFO;
                };
                CallbackManager.getInstance().logMessageReceived(testSessionId, logRequest.getMessage(), level);
            }
        } else {
            LOG.warn("Received log message from service but no session ID was provided");
        }
    }

    @GetMapping("/gitb")
    public RedirectView index() {
        return new RedirectView("gitb/swagger-ui.html?url=api-docs/gitb_client.json", true);
    }

    @GetMapping(path = "/gitb/api-docs/gitb_client.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getOpenApiSpec() throws IOException {
        try (var is = Thread.currentThread().getContextClassLoader().getResourceAsStream("rest/gitb_client.json")) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }
    }

    /**
     * Resolves the test session identifier for an incoming log request.
     *
     * @param logRequest the log request carrying a session identifier
     * @return the resolved test session identifier, or {@code null} if no mapping is found
     */
    private static String determineTestSessionId(LogRequest logRequest) {
        var sessionManager = SessionManager.getInstance();
        String testSessionId;
        if (sessionManager.exists(logRequest.getSessionId())) {
            testSessionId = logRequest.getSessionId();
        } else {
            testSessionId = sessionManager.getTestSessionForMessagingSession(logRequest.getSessionId());
            if (testSessionId == null) {
                testSessionId = sessionManager.getTestSessionForProcessingSession(logRequest.getSessionId());
            }
        }
        return testSessionId;
    }

}
