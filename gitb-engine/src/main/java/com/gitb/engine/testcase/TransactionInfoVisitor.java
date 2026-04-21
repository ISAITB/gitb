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

package com.gitb.engine.testcase;

import com.gitb.core.Configuration;
import com.gitb.core.TestRole;
import com.gitb.engine.expr.PossibleDomainIdentifier;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.tdl.*;
import com.gitb.utils.ActorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.*;

/**
 * Visitor that collects transaction information (from, handler, to) for the test case.
 */
public class TransactionInfoVisitor implements StepTraversalVisitor {

    private final List<TransactionInfo> transactions = new ArrayList<>();

    public List<TransactionInfo> getTransactions() {
        return transactions;
    }

    @Override
    public void visit(Object step, StepTraversalState state) {
        if (step instanceof BeginTransaction beginTransactionStep) {
            String fromActor = beginTransactionStep.getFrom();
            String toActor = beginTransactionStep.getTo();
            if (fromActor == null && toActor == null) {
                fromActor = state.testCase().getActors().getActor().getFirst().getId();
                toActor = state.testCase().getActors().getActor().getLast().getId(); // This would be the same actor is the test case defines only one.
            } else if (fromActor == null) {
                // Get the first actor (differing from the 'to' actor).
                fromActor = state.testCase().getActors().getActor().stream()
                        .filter(actor -> !beginTransactionStep.getTo().equals(actor.getId()))
                        .findFirst()
                        .map(TestRole::getId)
                        .orElseGet(() -> state.testCase().getActors().getActor().getFirst().getId());
            } else if (toActor == null) {
                // Get the first actor (differing from the 'from' actor).
                toActor = state.testCase().getActors().getActor().stream()
                        .filter(actor -> !beginTransactionStep.getFrom().equals(actor.getId()))
                        .findFirst()
                        .map(TestRole::getId)
                        .orElseGet(() -> state.testCase().getActors().getActor().getFirst().getId());
            }
            transactions.add(buildTransactionInfo(fromActor, toActor, beginTransactionStep.getHandler(), beginTransactionStep.getHandlerTimeout(), beginTransactionStep.getProperty(), state, beginTransactionStep.getHandlerApiType()));
        } else if (step instanceof MessagingStep messagingStep) {
            if (StringUtils.isBlank(messagingStep.getTxnId()) && StringUtils.isNotBlank(messagingStep.getHandler())) {
                String fromActor;
                String toActor;
                if (step instanceof ReceiveOrListen) {
                    fromActor = Objects.requireNonNullElseGet(messagingStep.getFrom(), state.context()::getDefaultSutActor);
                    toActor = Objects.requireNonNullElseGet(messagingStep.getTo(), state.context()::getDefaultNonSutActor);
                } else {
                    fromActor = Objects.requireNonNullElseGet(messagingStep.getFrom(), state.context()::getDefaultNonSutActor);
                    toActor = Objects.requireNonNullElseGet(messagingStep.getTo(), state.context()::getDefaultSutActor);
                }
                transactions.add(buildTransactionInfo(fromActor, toActor, messagingStep.getHandler(), messagingStep.getHandlerTimeout(), messagingStep.getProperty(), state, messagingStep.getHandlerApiType()));
            }
        } else if (step instanceof UserInteraction interactionStep) {
            if (!Strings.CS.equals(interactionStep.getHandlerEnabled(), "false") && interactionStep.getHandler() != null) {
                // We have an interaction step that may delegate processing to a custom handler.
                String interactionActor = state.context().getDefaultSutActor();
                List<Configuration> stepProperties = Optional.ofNullable(interactionStep.getHandlerConfig()).map(HandlerConfiguration::getProperty).orElseGet(Collections::emptyList);
                transactions.add(buildTransactionInfo(interactionActor, interactionActor, interactionStep.getHandler(), interactionStep.getHandlerTimeout(), stepProperties, state, interactionStep.getHandlerApiType()));
            }
        }
    }

    private TransactionInfo buildTransactionInfo(String from, String to, String handler, String handlerTimeout, List<Configuration> properties, StepTraversalState state, HandlerApiType declaredHandlerApiType) {
        String handlerValue;
        String handlerDomainIdentifier;
        if (VariableResolver.isVariableReference(handler)) {
            PossibleDomainIdentifier handlerInfo = state.getExpressionHandler().getVariableResolver().resolveAsPossibleDomainIdentifier(handler);
            handlerValue = handlerInfo.value();
            handlerDomainIdentifier = handlerInfo.domainIdentifier();
        } else {
            handlerValue = handler;
            handlerDomainIdentifier = null;
        }
        return new TransactionInfo(
                TestCaseUtils.fixedOrVariableValue(ActorUtils.extractActorId(from), String.class, state.scriptletCallStack(), state.getExpressionHandler()),
                TestCaseUtils.fixedOrVariableValue(ActorUtils.extractEndpointName(from), String.class, state.scriptletCallStack(), state.getExpressionHandler()),
                TestCaseUtils.fixedOrVariableValue(ActorUtils.extractActorId(to), String.class, state.scriptletCallStack(), state.getExpressionHandler()),
                TestCaseUtils.fixedOrVariableValue(ActorUtils.extractEndpointName(to), String.class, state.scriptletCallStack(), state.getExpressionHandler()),
                handlerValue,
                handlerDomainIdentifier,
                handlerTimeout,
                declaredHandlerApiType,
                TestCaseUtils.getStepProperties(properties, state.getExpressionHandler().getVariableResolver())
        );
    }

}
