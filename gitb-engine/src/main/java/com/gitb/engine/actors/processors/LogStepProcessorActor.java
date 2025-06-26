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

package com.gitb.engine.actors.processors;

import com.gitb.core.LogLevel;
import com.gitb.engine.expr.ExpressionHandler;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.StepContext;
import com.gitb.tdl.Log;
import com.gitb.types.DataType;
import com.gitb.types.StringType;
import org.apache.pekko.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

public class LogStepProcessorActor extends AbstractTestStepActor<Log> {

    private static final Logger LOG = LoggerFactory.getLogger("TEST_SESSION");
    public static final String NAME = "log-p";

    public LogStepProcessorActor(Log step, TestCaseScope scope, String stepId, StepContext stepContext) {
        super(step, scope, stepId, stepContext);
    }

    @Override
    protected void init() {
        // Do nothing.
    }

    @Override
    protected void start() {
        processing();
        ExpressionHandler exprHandler = new ExpressionHandler(scope);
        StringType result = (StringType) exprHandler.processExpression(step).convertTo(DataType.STRING_DATA_TYPE);
        if (result != null) {
            var marker = MarkerFactory.getDetachedMarker(scope.getContext().getSessionId());
            var message = (String)result.getValue();
            var variableResolver = new VariableResolver(scope);
            var level = LogLevel.INFO;
            try {
                if (VariableResolver.isVariableReference(step.getLevel())) {
                    level = LogLevel.fromValue((String) variableResolver.resolveVariable(step.getLevel()).convertTo(DataType.STRING_DATA_TYPE).getValue());
                } else {
                    level = LogLevel.fromValue(step.getLevel());
                }
            } catch (Exception e) {
                LOG.warn(MarkerFactory.getDetachedMarker(scope.getContext().getSessionId()), String.format("Invalid log level [%s]. Considering %s as default.", step.getLevel(), LogLevel.INFO));
            }
            switch (level) {
                case ERROR: LOG.error(marker, message); break;
                case WARNING: LOG.warn(marker, message); break;
                case INFO: LOG.info(marker, message); break;
                default: LOG.debug(marker, message);
            }
        }
        completed();
    }

    public static ActorRef create(ActorContext context, Log step, TestCaseScope scope, String stepId, StepContext stepContext) throws Exception{
        return create(LogStepProcessorActor.class, context, step, scope, stepId, stepContext);
    }

}
