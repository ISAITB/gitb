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

package com.gitb.engine.messaging.handlers.layer.application.domibus;

import com.gitb.core.Configuration;
import com.gitb.engine.CallbackManager;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.layer.AbstractNonWorkerMessagingHandler;
import com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ebms.Messaging;
import com.gitb.engine.messaging.handlers.layer.application.domibus.stub.ebms.PartInfo;
import com.gitb.messaging.DeferredMessagingReport;
import com.gitb.messaging.DeferredTask;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.messaging.callback.CallbackData;
import com.gitb.messaging.callback.CallbackType;
import com.gitb.tdl.MessagingStep;
import com.gitb.types.*;
import com.gitb.utils.XMLUtils;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.gitb.utils.MessagingReportUtils.generateErrorReport;
import static com.gitb.utils.MessagingReportUtils.generateSuccessReport;

@MessagingHandler(name="DomibusMessaging")
public class DomibusMessagingHandler extends AbstractNonWorkerMessagingHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DomibusMessagingHandler.class);
    private static final String INPUT_HEADER = "header";
    private static final String INPUT_PAYLOAD = "payload";
    private static final String INPUT_TYPE = "type";
    private static final String INPUT_MESSAGE_IDENTIFIER = "messageIdentifier";
    private static final String INPUT_SUCCESS_STATE = "successState";
    private static final String INPUT_FAILURE_STATE = "failureState";
    private static final String INPUT_MAXIMUM_POLL_ATTEMPTS = "maximumPollAttempts";
    private static final String INPUT_INITIAL_POLL_DELAY = "initialPollDelay";
    private static final String INPUT_POLL_INTERVAL = "pollInterval";
    private static final String INPUT_BACKEND_ADDRESS = "backendAddress";
    private static final String INPUT_BACKEND_USERNAME = "backendUsername";
    private static final String INPUT_BACKEND_PASSWORD = "backendPassword";
    private static final String INPUT_BACKEND_AUTH_TYPE = "backendAuthType";

    @Override
    public MessagingReport sendMessage(String sessionId, String transactionId, String stepId, List<Configuration> configurations, Message message) {
        BinaryType header = Optional.ofNullable(getAndConvert(message.getFragments(), INPUT_HEADER, DataType.BINARY_DATA_TYPE, BinaryType.class))
                .orElseThrow(() -> new IllegalArgumentException("Input [%s] must be provided.".formatted(INPUT_HEADER)));
        Messaging headerToUse = getAs4Header(header.getValue());
        List<byte[]> payloads = Optional.ofNullable(getAndConvert(message.getFragments(), INPUT_PAYLOAD, DataType.LIST_DATA_TYPE, ListType.class))
                .map(ListType::getElements)
                .map(elements -> elements.stream().map(element -> ((BinaryType) element.convertTo(DataType.BINARY_DATA_TYPE)).getValue()).toList())
                .orElseThrow(() -> new IllegalArgumentException("Input [%s] must be provided.".formatted(INPUT_PAYLOAD)));
        if (payloads.isEmpty()) {
            throw new IllegalArgumentException("No content provided to send.");
        }
        if (headerToUse.getUserMessage().getPayloadInfo().getPartInfo().size() != payloads.size()) {
            throw new IllegalStateException("The number of payloads provided [%s] must match the number of payload parts in the header [%s]".formatted(payloads.size(), headerToUse.getUserMessage().getPayloadInfo().getPartInfo().size()));
        }
        var client = new DomibusClient(getBackendInfo(message));
        // Build report.
        var messageForReport = new Message();
        List<String> messageIds = client.sendToDomibus(headerToUse, payloads);
        if (!messageIds.isEmpty()) {
            messageForReport.getFragments().put("messageIdentifier", new StringType(messageIds.getFirst()));
        }
        var messagePayloads = new ListType();
        List<PartInfo> partInfo = null;
        if (headerToUse.getUserMessage() != null && headerToUse.getUserMessage().getPayloadInfo() != null) {
            partInfo = headerToUse.getUserMessage().getPayloadInfo().getPartInfo();
        }
        int counter = 0;
        for (var payload: payloads) {
            var payloadInfo = new MapType();
            var payloadData = new BinaryType(payload);
            if (partInfo != null && partInfo.size() > counter) {
                var info = partInfo.get(counter);
                if (info != null) {
                    if (info.getHref() != null) {
                        payloadInfo.addItem("identifier", new StringType(info.getHref()));
                    }
                    if (info.getPartProperties() != null) {
                        info.getPartProperties().getProperty().stream()
                                .filter(p -> "MimeType".equals(p.getName()) && p.getValue() != null)
                                .findFirst()
                                .ifPresent(p -> payloadData.setContentType(p.getValue()));
                    }
                }
            }
            payloadInfo.addItem("content", payloadData);
            messagePayloads.append(payloadInfo);
            counter += 1;
        }
        messageForReport.getFragments().put("header", header);
        messageForReport.getFragments().put("payload", messagePayloads);
        return generateSuccessReport(messageForReport);
    }

    @Override
    public MessagingReport receiveMessage(String sessionId, String transactionId, String callId, MessagingStep step, Message inputs, List<Thread> messagingThreads) {
        // Parse and validate inputs.
        ReceiveType receiveType = Optional.ofNullable(getAndConvert(inputs.getFragments(), INPUT_TYPE, DataType.STRING_DATA_TYPE, StringType.class))
                .map(v -> ReceiveType.forValue(v.getValue()))
                .orElse(ReceiveType.MESSAGE);
        String messageIdentifier = Optional.ofNullable(getAndConvert(inputs.getFragments(), INPUT_MESSAGE_IDENTIFIER, DataType.STRING_DATA_TYPE, StringType.class))
                .map(StringType::getValue)
                .orElseThrow(() -> new IllegalArgumentException("Input [%s] must be provided.".formatted(INPUT_MESSAGE_IDENTIFIER)));
        int maxPollingAttempts = Optional.ofNullable(getAndConvert(inputs.getFragments(), INPUT_MAXIMUM_POLL_ATTEMPTS, DataType.NUMBER_DATA_TYPE, NumberType.class))
                .map(NumberType::intValue)
                .orElse(60); // 60 maximum attempts by default.
        long pollingInterval = Optional.ofNullable(getAndConvert(inputs.getFragments(), INPUT_POLL_INTERVAL, DataType.NUMBER_DATA_TYPE, NumberType.class))
                .map(NumberType::longValue)
                .orElse(10000L); // 10 seconds between polling attempts by default.
        long initialPollingDelay = Optional.ofNullable(getAndConvert(inputs.getFragments(), INPUT_INITIAL_POLL_DELAY, DataType.NUMBER_DATA_TYPE, NumberType.class))
                .map(NumberType::longValue)
                .orElse(500L); // 500 milliseconds before initial poll by default.
        Set<String> ackSuccessStates = null;
        Set<String> ackFailureStates = null;
        if (receiveType == ReceiveType.ACKNOWLEDGEMENT) {
            ackSuccessStates = Optional.ofNullable(getAndConvert(inputs.getFragments(), INPUT_SUCCESS_STATE, DataType.LIST_DATA_TYPE, ListType.class))
                    .map(v -> v.getElements().stream()
                            .flatMap(item -> Arrays.stream(((String) item.convertTo(DataType.STRING_DATA_TYPE).getValue()).split(",")))
                            .map(String::trim)
                            .collect(Collectors.toSet()))
                    .orElseGet(() -> Set.of("ACKNOWLEDGED"));
            ackFailureStates = Optional.ofNullable(getAndConvert(inputs.getFragments(), INPUT_FAILURE_STATE, DataType.LIST_DATA_TYPE, ListType.class))
                    .map(v -> v.getElements().stream()
                            .flatMap(item -> Arrays.stream(((String) item.convertTo(DataType.STRING_DATA_TYPE).getValue()).split(",")))
                            .map(String::trim)
                            .collect(Collectors.toSet()))
                    .orElseGet(() -> Set.of("SEND_FAILURE", "NOT_FOUND"));
        }
        // Begin polling for message or acknowledgement.
        DeferredTask<PollingState> task = createPollingTask(new PollingState(new DomibusClient(getBackendInfo(inputs)), Instant.now(), 0, maxPollingAttempts, pollingInterval, messageIdentifier, receiveType, ackSuccessStates, ackFailureStates, sessionId, callId), initialPollingDelay);
        // Return deferred report to complete this asynchronously.
        return new DeferredMessagingReport(new CallbackData(inputs, CallbackType.DOMIBUS), task);
    }

    private Message createReportForDownloadedMessage(String messageIdentifier, MessageData messageData) {
        var messageForReport = new Message();
        messageForReport.addInput("messageIdentifier", new StringType(messageIdentifier));
        if (messageData.getHeaderContent() != null) {
            messageForReport.addInput("header", new StringType(messageData.getHeaderContent()));
        }
        if (messageData.getPayloads() != null && !messageData.getPayloads().isEmpty()) {
            var payloads = new ListType();
            for (var payloadData: messageData.getPayloads()) {
                var payload = new MapType();
                var payloadContent = new BinaryType(payloadData.getValue());
                payload.addItem("identifier", new StringType(payloadData.getId()));
                payload.addItem("content", payloadContent);
                if (payloadData.getContentType() != null) {
                    payloadContent.setContentType(payloadData.getContentType());
                    payload.addItem("contentType", new StringType(payloadData.getContentType()));
                }
                payloads.append(payload);
            }
            messageForReport.addInput("payload", payloads);
        }
        return messageForReport;
    }

    private Message createReportForAcknowledgement(String messageIdentifier, String statusValue, boolean success) {
        var messageForReport = new Message();
        messageForReport.addInput("messageIdentifier", new StringType(messageIdentifier));
        messageForReport.addInput("status", new StringType(statusValue));
        return messageForReport;
    }

    private DeferredTask.Result<PollingState> completeWithReport(MessagingReport report, PollingState state) {
        CallbackManager.getInstance().callbackReceived(state.sessionId(), state.callId(), report);
        return new DeferredTask.Result<>(report, null, null);
    }

    private DeferredTask.Result<PollingState> nextPollingTask(PollingState currentState) {
        if ((currentState.currentPollAttemp() + 1) < currentState.maximumPollAttempts()) {
            // Schedule next poll.
            PollingState newState = currentState.nextAttempt();
            LOG.debug(addMarker(currentState.sessionId()), "{} [{}] not found after {} {}...", (currentState.pollType() == ReceiveType.MESSAGE)?"Message":"Acknowledgement", currentState.messageIdentifier(), (currentState.currentPollAttemp() + 1), (currentState.currentPollAttemp() == 0?"attempt":"attempts"));
            return new DeferredTask.Result<>(null, newState.pollInterval(), newState);
        } else {
            // Expire the task.
            LOG.debug(addMarker(currentState.sessionId()), "{} [{}] not found ({} {} in total).", (currentState.pollType() == ReceiveType.MESSAGE)?"Message":"Acknowledgement", currentState.messageIdentifier(), currentState.maximumPollAttempts(), (currentState.maximumPollAttempts() == 0?"attempt":"attempts"));
            return new DeferredTask.Result<>(null, null, null);
        }
    }

    private DeferredTask<PollingState> createPollingTask(PollingState initialState, long initialDelay) {
        LOG.info(addMarker(initialState.sessionId()), "Polling for {} [{}]...", (initialState.pollType() == ReceiveType.MESSAGE)?"message":"acknowledgement", initialState.messageIdentifier());
        Function<PollingState, DeferredTask.Result<PollingState>> executionHandler = (state) -> {
            if (state.pollType() == ReceiveType.ACKNOWLEDGEMENT) {
                String status = state.client().checkStatus(state.messageIdentifier());
                if (status != null) {
                    if (state.ackSuccessStates().contains(status)) {
                        LOG.debug(addMarker(state.sessionId()), "Successful acknowledgement [{}] for message [{}].", status, state.messageIdentifier());
                        MessagingReport report = generateSuccessReport(createReportForAcknowledgement(state.messageIdentifier(), status, true));
                        return completeWithReport(report, state);
                    } else if (state.ackFailureStates().contains(status)) {
                        LOG.debug(addMarker(state.sessionId()), "Failed acknowledgement [{}] for message [{}].", status, state.messageIdentifier());
                        MessagingReport report = generateErrorReport(createReportForAcknowledgement(state.messageIdentifier(), status, false));
                        return completeWithReport(report, state);
                    }
                }
                // Plan new polling attempt.
                return nextPollingTask(state);
            } else {
                // Download message
                if (state.client().getPendingMessageIds().stream().anyMatch(id -> Objects.equals(id, state.messageIdentifier()))) {
                    // Found the message we are looking for.
                    LOG.debug(addMarker(state.sessionId()), "Downloaded message [{}].", state.messageIdentifier());
                    MessageData messageData = state.client().downloadMessage(state.messageIdentifier());
                    MessagingReport report = generateSuccessReport(createReportForDownloadedMessage(state.messageIdentifier(), messageData));
                    return completeWithReport(report, state);
                } else {
                    return nextPollingTask(state);
                }
            }
        };
        Function<PollingState, MessagingReport> expiryHandler = (state) -> generateErrorReport("Failed to find message after %s %s.".formatted(state.maximumPollAttempts(), (state.maximumPollAttempts() == 1)?"attempt":"attempts"));
        return new DeferredTask<>(initialState, executionHandler, expiryHandler, initialDelay);
    }

    private BackendInfo getBackendInfo(Message message) {
        var backendInfo = new BackendInfo(
                Objects.requireNonNull(getAndConvert(message.getFragments(), INPUT_BACKEND_ADDRESS, DataType.STRING_DATA_TYPE, StringType.class), "Input [%s] must be provided.".formatted(INPUT_BACKEND_ADDRESS)).getValue(),
                Optional.ofNullable(getAndConvert(message.getFragments(), INPUT_BACKEND_USERNAME, DataType.STRING_DATA_TYPE, StringType.class)).map(StringType::getValue).orElse(null),
                Optional.ofNullable(getAndConvert(message.getFragments(), INPUT_BACKEND_PASSWORD, DataType.STRING_DATA_TYPE, StringType.class)).map(StringType::getValue).orElse(null),
                Optional.ofNullable(getAndConvert(message.getFragments(), INPUT_BACKEND_AUTH_TYPE, DataType.STRING_DATA_TYPE, StringType.class)).map(v -> AuthType.forValue(v.getValue())).orElse(AuthType.BASIC)
        );
        if (backendInfo.username() != null && backendInfo.password() == null || backendInfo.username() == null && backendInfo.password() != null) {
            throw new IllegalArgumentException("When providing backend credentials, both the [%s] and [%s] inputs need to be provided.".formatted(INPUT_BACKEND_USERNAME, INPUT_BACKEND_PASSWORD));
        }
        return backendInfo;
    }

    private Messaging getAs4Header(byte[] headerBytes) {
        Messaging messaging;
        try (var inputStream = new ByteArrayInputStream(headerBytes)) {
            messaging = XMLUtils.unmarshal(Messaging.class, new StreamSource(inputStream));
        } catch (IOException | JAXBException e) {
            throw new IllegalStateException("Error while preparing header", e);
        }
        return messaging;
    }

}
