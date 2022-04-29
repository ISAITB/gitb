package com.gitb.messaging.layer.application.simulated;

import com.gitb.core.*;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.Message;
import com.gitb.messaging.MessagingReport;
import com.gitb.messaging.layer.AbstractMessagingHandler;
import com.gitb.messaging.utils.MessagingHandlerUtils;
import com.gitb.ms.InitiateResponse;
import com.gitb.tr.TestResultType;
import com.gitb.types.DataType;
import com.gitb.types.StringType;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@MetaInfServices(IMessagingHandler.class)
public class SimulatedMessagingHandler extends AbstractMessagingHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SimulatedMessagingHandler.class);
    private static final String INPUT__PARAMETERS = "parameters";
    private static final String INPUT__RESULT = "result";
    private static final String INPUT__DELAY = "delay";

    @Override
    public MessagingModule getModuleDefinition() {
        var module = new MessagingModule();
        module.setId("SimulatedMessaging");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0.0");
        module.setInputs(new TypedParameters());
        module.getInputs().getParam().add(createParameter(INPUT__PARAMETERS, "map", ConfigurationType.SIMPLE, UsageEnumeration.O, "The map of input parameters that will be displayed as data in the step's report."));
        module.getInputs().getParam().add(createParameter(INPUT__RESULT, "string", ConfigurationType.SIMPLE, UsageEnumeration.O, String.format("The result of the step. On of '%s', '%s' or '%s'. If not specified the default considered is '%s'.", TestResultType.SUCCESS, TestResultType.WARNING, TestResultType.WARNING, TestResultType.SUCCESS)));
        module.getInputs().getParam().add(createParameter(INPUT__DELAY, "number", ConfigurationType.SIMPLE, UsageEnumeration.O, "A duration in milliseconds after which the receive call should be completed."));
        return module;
    }

    private TypedParameter createParameter(String name, String type, ConfigurationType kind, UsageEnumeration usage, String description) {
        var parameter = new TypedParameter();
        parameter.setName(name);
        parameter.setType(type);
        parameter.setKind(kind);
        parameter.setUse(usage);
        parameter.setDesc(description);
        return parameter;
    }

    private MessagingReport createReport(Message message) {
        // Overall result
        var result = TestResultType.SUCCESS;
        if (message.hasInput(INPUT__RESULT)) {
            try {
                result = TestResultType.valueOf(((String) message.getFragments().get(INPUT__RESULT).convertTo(StringType.STRING_DATA_TYPE).getValue()));
            } catch (IllegalArgumentException | NullPointerException e) {
                LOG.warn(addMarker(), String.format("Invalid value for input '%s'. Considering '%s' by default.", INPUT__RESULT, TestResultType.SUCCESS));
            }
        }
        var messageForReport = new Message();
        if (message.hasInput(INPUT__PARAMETERS)) {
            messageForReport.addInput(INPUT__PARAMETERS, message.getFragments().get(INPUT__PARAMETERS));
        }
        var report = MessagingHandlerUtils.generateSuccessReport(messageForReport);
        report.getReport().setResult(result);
        return report;
    }

    @Override
    public MessagingReport sendMessage(String sessionId, String transactionId, List<Configuration> configurations, Message message) {
        return createReport(message);
    }

    @Override
    public MessagingReport receiveMessage(String sessionId, String transactionId, String callId, List<Configuration> configurations, Message message, List<Thread> messagingThreads) {
        if (message.getFragments().containsKey(INPUT__DELAY)) {
            var delay = ((Double)message.getFragments().get(INPUT__DELAY).convertTo(DataType.NUMBER_DATA_TYPE).getValue()).longValue();
            // TODO wait for the prescribed delay.
        }
        return createReport(message);
    }

    @Override
    public void beginTransaction(String sessionId, String transactionId, String from, String to, List<Configuration> configurations) {
        // Do nothing.
    }

    @Override
    public MessagingReport listenMessage(String sessionId, String transactionId, String from, String to, List<Configuration> configurations, Message inputs) {
        throw new IllegalStateException("The SimulatedMessaging handler can only be used for send and receive operations");
    }

    @Override
    public void endTransaction(String sessionId, String transactionId) {
        // Do nothing.
    }

    @Override
    public void endSession(String sessionId) {
        // Do nothing.
    }

    @Override
    public InitiateResponse initiate(List<ActorConfiguration> actorConfigurations) {
        return new InitiateResponse();
    }
}
