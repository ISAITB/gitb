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

package com.gitb.engine.messaging.handlers.layer.application.simulated;

import com.gitb.core.Configuration;
import com.gitb.engine.CallbackManager;
import com.gitb.engine.TestEngine;
import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.messaging.handlers.layer.AbstractNonWorkerMessagingHandler;
import com.gitb.engine.messaging.handlers.utils.MessagingHandlerUtils;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.messaging.DeferredMessagingReport;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.tdl.MessagingStep;
import com.gitb.tr.TestResultType;
import com.gitb.types.DataType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

@MessagingHandler(name="SimulatedMessaging")
public class SimulatedMessagingHandler extends AbstractNonWorkerMessagingHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SimulatedMessagingHandler.class);
    private static final String INPUT_PARAMETERS = "parameters";
    private static final String INPUT_CONTENT_TYPES = "contentTypes";
    private static final String INPUT_RESULT = "result";
    private static final String INPUT_DELAY = "delay";

    private MessagingReport createReport(String sessionId, Message message) {
        // Overall result
        var result = TestResultType.SUCCESS;
        if (message.hasInput(INPUT_RESULT)) {
            try {
                result = TestResultType.valueOf(((String) message.getFragments().get(INPUT_RESULT).convertTo(DataType.STRING_DATA_TYPE).getValue()));
            } catch (IllegalArgumentException | NullPointerException e) {
                LOG.warn(addMarker(sessionId), String.format("Invalid value for input '%s'. Considering '%s' by default.", INPUT_RESULT, TestResultType.SUCCESS));
            }
        }
        var messageForReport = new Message();
        if (message.hasInput(INPUT_PARAMETERS)) {
            var fragments = message.getFragments().get(INPUT_PARAMETERS);
            if (fragments instanceof MapType) {
                for (var fragment: ((MapType) fragments).getItems().entrySet()) {
                    messageForReport.addInput(fragment.getKey(), fragment.getValue());
                }
            }
        }
        var report = MessagingHandlerUtils.generateSuccessReport(messageForReport);
        TestCaseUtils.applyContentTypes(message.getFragments().get(INPUT_CONTENT_TYPES), report.getReport().getContext());
        report.getReport().setResult(result);
        return report;
    }

    @Override
    public MessagingReport sendMessage(String sessionId, String transactionId, String stepId, List<Configuration> configurations, Message message) {
        return createReport(sessionId, message);
    }

    @Override
    public MessagingReport receiveMessage(String sessionId, String transactionId, String callId, MessagingStep step, Message message, List<Thread> messagingThreads) {
        var report = createReport(sessionId, message);
        if (message.getFragments().containsKey(INPUT_DELAY)) {
            var delay = ((Double)message.getFragments().get(INPUT_DELAY).convertTo(DataType.NUMBER_DATA_TYPE).getValue()).longValue();
            if (delay > 0) {
                TestEngine.getInstance().getEngineActorSystem().getActorSystem().getScheduler().scheduleOnce(
                        scala.concurrent.duration.Duration.apply(delay, TimeUnit.MILLISECONDS), () -> CallbackManager.getInstance().callbackReceived(sessionId, callId, report),
                        scala.concurrent.ExecutionContext.global());
                return new DeferredMessagingReport();
            }
        }
        return report;
    }

}
