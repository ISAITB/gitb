package com.gitb.tbs.logging;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import com.gitb.core.AnyContent;
import com.gitb.core.StepStatus;
import com.gitb.engine.ITestbedServiceCallbackHandler;
import com.gitb.engine.TestEngine;
import com.gitb.engine.actors.processors.AbstractTestStepActor;
import com.gitb.tbs.TestStepStatus;
import com.gitb.tbs.TestbedClient;
import com.gitb.tr.TAR;

/**
 * Custom logging appender that is used session-specific log messages to the test bed client.
 */
public class TestSessionAppender extends AppenderBase<ILoggingEvent> {

    public static String LOG_EVENT_STEP_ID = "-999";

    private PatternLayoutEncoder encoder;

    public PatternLayoutEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public void start() {
        if (this.encoder == null) {
            addError("No encoder set for the appender named ["+ name +"].");
            return;
        }
        encoder.start();
        super.start();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (eventObject.getMarker() != null) {
            String sessionId = eventObject.getMarker().getName();
            ITestbedServiceCallbackHandler tbsCallbackHandle = TestEngine.getInstance().getTbsCallbackHandle();
            TestbedClient testbedClient = tbsCallbackHandle.getTestbedClient(sessionId);
            //Construct the Callback response
            TestStepStatus testStepStatus = new TestStepStatus();
            testStepStatus.setTcInstanceId(sessionId);
            testStepStatus.setStepId(LOG_EVENT_STEP_ID);
            testStepStatus.setStatus(StepStatus.PROCESSING);
            TAR report = new TAR();
            report.setContext(new AnyContent());
            report.getContext().setValue(getLogMessage(eventObject));
            testStepStatus.setReport(report);
            //Call the UpdateStatus callback
            testbedClient.updateStatus(testStepStatus);
        }
    }

    private String getLogMessage(ILoggingEvent eventObject) {
        StringBuilder messageBuilder = new StringBuilder(new String(encoder.encode(eventObject)));
        addCauseMessage(messageBuilder, eventObject.getThrowableProxy());
        return messageBuilder.toString();
    }

    private void addCauseMessage(StringBuilder messageBuilder, IThrowableProxy throwable) {
        if (throwable != null) {
            messageBuilder.append("\t[");
            if (throwable.getClassName() != null) {
                String replacementValue = throwable.getClassName().replaceAll(AbstractTestStepActor.EXCEPTION_SANITIZATION_EXPRESSION, "$1");
                if (throwable.getClassName().equals(replacementValue)) {
                    replacementValue = "-";
                }
                messageBuilder.append(replacementValue);
            } else {
                messageBuilder.append("-");
            }
            messageBuilder.append("]: ");
            if (throwable.getMessage() != null) {
                messageBuilder.append(throwable.getMessage().replaceAll(AbstractTestStepActor.EXCEPTION_SANITIZATION_EXPRESSION, "$1"));
            } else {
                messageBuilder.append("null");
            }
            messageBuilder.append('\n');
            addCauseMessage(messageBuilder, throwable.getCause());
        }
    }

}
