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

package com.gitb.tbs.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import com.gitb.core.AnyContent;
import com.gitb.core.LogLevel;
import com.gitb.core.StepStatus;
import com.gitb.engine.PropertyConstants;
import com.gitb.engine.TestEngine;
import com.gitb.engine.actors.SessionActor;
import com.gitb.engine.actors.processors.AbstractTestStepActor;
import com.gitb.engine.commands.interaction.LogCommand;
import com.gitb.tbs.TestStepStatus;
import com.gitb.tr.TAR;
import org.apache.pekko.actor.ActorRef;

/**
 * Custom logging appender that is used session-specific log messages to the test bed client.
 */
public class TestSessionAppender extends AppenderBase<ILoggingEvent> {

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
        var markerList =  eventObject.getMarkerList();
        if (markerList != null && !markerList.isEmpty()) {
            String sessionId = markerList.get(0).getName();
            //Construct the Callback response
            TestStepStatus testStepStatus = new TestStepStatus();
            testStepStatus.setTcInstanceId(sessionId);
            testStepStatus.setStepId(PropertyConstants.LOG_EVENT_STEP_ID);
            testStepStatus.setStatus(StepStatus.PROCESSING);
            TAR report = new TAR();
            report.setContext(new AnyContent());
            report.getContext().setValue(getLogMessage(eventObject));
            testStepStatus.setReport(report);
            // Trigger log message send
            TestEngine
                    .getInstance()
                    .getEngineActorSystem()
                    .getActorSystem()
                    .actorSelection(SessionActor.getPath(sessionId))
                    .tell(new LogCommand(sessionId, testStepStatus, toLogLevel(eventObject.getLevel())), ActorRef.noSender());
        }
    }

    private LogLevel toLogLevel(Level level) {
        var logLevel = LogLevel.DEBUG;
        if (level != null) {
            logLevel = switch (level.levelInt) {
                case Level.ERROR_INT -> LogLevel.ERROR;
                case Level.WARN_INT -> LogLevel.WARNING;
                case Level.INFO_INT -> LogLevel.INFO;
                default -> logLevel;
            };
        }
        return logLevel;
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
