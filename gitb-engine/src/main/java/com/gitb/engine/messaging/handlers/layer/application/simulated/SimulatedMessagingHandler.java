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
    private static final String INPUT__PARAMETERS = "parameters";
    private static final String INPUT__CONTENT_TYPES = "contentTypes";
    private static final String INPUT__RESULT = "result";
    private static final String INPUT__DELAY = "delay";

    private MessagingReport createReport(String sessionId, Message message) {
        // Overall result
        var result = TestResultType.SUCCESS;
        if (message.hasInput(INPUT__RESULT)) {
            try {
                result = TestResultType.valueOf(((String) message.getFragments().get(INPUT__RESULT).convertTo(StringType.STRING_DATA_TYPE).getValue()));
            } catch (IllegalArgumentException | NullPointerException e) {
                LOG.warn(addMarker(sessionId), String.format("Invalid value for input '%s'. Considering '%s' by default.", INPUT__RESULT, TestResultType.SUCCESS));
            }
        }
        var messageForReport = new Message();
        if (message.hasInput(INPUT__PARAMETERS)) {
            var fragments = message.getFragments().get(INPUT__PARAMETERS);
            if (fragments instanceof MapType) {
                for (var fragment: ((MapType) fragments).getItems().entrySet()) {
                    messageForReport.addInput(fragment.getKey(), fragment.getValue());
                }
            }
        }
        var report = MessagingHandlerUtils.generateSuccessReport(messageForReport);
        TestCaseUtils.applyContentTypes(message.getFragments().get(INPUT__CONTENT_TYPES), report.getReport().getContext());
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
        if (message.getFragments().containsKey(INPUT__DELAY)) {
            var delay = ((Double)message.getFragments().get(INPUT__DELAY).convertTo(DataType.NUMBER_DATA_TYPE).getValue()).longValue();
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
