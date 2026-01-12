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

import com.gitb.engine.messaging.handlers.layer.application.simulated.SimulatedMessagingHandler;
import com.gitb.engine.processing.handlers.DisplayProcessor;
import com.gitb.tdl.BeginProcessingTransaction;
import com.gitb.tdl.BeginTransaction;
import com.gitb.tdl.MessagingStep;
import com.gitb.tdl.Process;

import java.util.HashSet;
import java.util.Set;

/**
 * Visitor that determines whether TAR reports need to be persisted.
 * <p></p>
 * Persistence is only needed when step report are being referred to in DisplayProcessor and SimulatedMessaging
 * handlers.
 */
public class PersistentReportVisitor implements StepTraversalVisitor {

    private boolean persistentReportsNeeded = false;
    private final Set<String> matchingProcessingTransactions = new HashSet<>();
    private final Set<String> matchingMessagingTransactions = new HashSet<>();

    public boolean isPersistentReportsNeeded() {
        return persistentReportsNeeded;
    }

    @Override
    public void visit(Object step, StepTraversalState state) {
        if (!persistentReportsNeeded) {
            if (step instanceof Process processStep) {
                if (DisplayProcessor.HANDLER_NAME.equals(processStep.getHandler())
                        || (processStep.getTxnId() != null && matchingProcessingTransactions.contains(processStep.getTxnId()))) {
                    this.persistentReportsNeeded = processStep.getInput().stream().anyMatch(input -> DisplayProcessor.INPUT_REPORT_STEPS.equals(input.getName()));
                }
            } else if (step instanceof MessagingStep messagingStep) {
                if (SimulatedMessagingHandler.HANDLER_NAME.equals(messagingStep.getHandler())
                        || (messagingStep.getTxnId() != null && matchingMessagingTransactions.contains(messagingStep.getTxnId()))) {
                    this.persistentReportsNeeded = messagingStep.getInput().stream().anyMatch(input -> SimulatedMessagingHandler.INPUT_REPORT_STEPS.equals(input.getName()));
                }
            } else if (step instanceof BeginProcessingTransaction startStep) {
                if (DisplayProcessor.HANDLER_NAME.equals(startStep.getHandler())) {
                    matchingProcessingTransactions.add(startStep.getTxnId());
                }
            } else if (step instanceof BeginTransaction startStep) {
                if (SimulatedMessagingHandler.HANDLER_NAME.equals(startStep.getHandler())) {
                    matchingMessagingTransactions.add(startStep.getTxnId());
                }
            }
        }
    }

}
