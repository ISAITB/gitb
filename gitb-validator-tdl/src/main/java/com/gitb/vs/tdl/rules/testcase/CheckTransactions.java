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

package com.gitb.vs.tdl.rules.testcase;

import com.gitb.tdl.Process;
import com.gitb.tdl.*;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.util.Utils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

public class CheckTransactions extends AbstractTestCaseObserver {

    private final Set<String> allMessagingTransactionIds = new HashSet<>();
    private final Set<String> allProcessingTransactionIds = new HashSet<>();
    private final Set<String> openMessagingTransactionIds = new HashSet<>();
    private final Set<String> openProcessingTransactionIds = new HashSet<>();
    private final Set<String> referencedMessagingTransactionIds = new HashSet<>();
    private final Set<String> referencedProcessingTransactionIds = new HashSet<>();

    @Override
    public void initialiseTestCase(TestCase currentTestCase) {
        super.initialiseTestCase(currentTestCase);
        allMessagingTransactionIds.clear();
        allProcessingTransactionIds.clear();
        openMessagingTransactionIds.clear();
        openProcessingTransactionIds.clear();
        referencedMessagingTransactionIds.clear();
        referencedProcessingTransactionIds.clear();
    }

    @Override
    public void initialiseScriptlet(Scriptlet scriptlet) {
        super.initialiseScriptlet(scriptlet);
        allMessagingTransactionIds.clear();
        allProcessingTransactionIds.clear();
        openMessagingTransactionIds.clear();
        openProcessingTransactionIds.clear();
        referencedMessagingTransactionIds.clear();
        referencedProcessingTransactionIds.clear();
    }

    @Override
    public void handleStep(Object stepObj) {
        if (stepObj instanceof BeginTransaction) {
            allMessagingTransactionIds.add(((BeginTransaction)stepObj).getTxnId());
            openMessagingTransactionIds.add(((BeginTransaction)stepObj).getTxnId());
        } else if (stepObj instanceof BeginProcessingTransaction) {
            allProcessingTransactionIds.add(((BeginProcessingTransaction)stepObj).getTxnId());
            openProcessingTransactionIds.add(((BeginProcessingTransaction)stepObj).getTxnId());
        } else if (stepObj instanceof EndTransaction) {
            if (openMessagingTransactionIds.isEmpty()) {
                addReportItem(ErrorCode.MESSAGING_TX_END_WITHOUT_START, currentTestCase.getId());
            } else {
                String txId = ((EndTransaction)stepObj).getTxnId();
                if (openMessagingTransactionIds.contains(txId)) {
                    openMessagingTransactionIds.remove(txId);
                } else {
                    addReportItem(ErrorCode.INVALID_TX_REFERENCE_FOR_MESSAGING_END, currentTestCase.getId(), txId);
                }
            }
        } else if (stepObj instanceof EndProcessingTransaction) {
            if (openProcessingTransactionIds.isEmpty()) {
                addReportItem(ErrorCode.PROCESSING_TX_END_WITHOUT_START, currentTestCase.getId());
            } else {
                String txId = ((EndProcessingTransaction)stepObj).getTxnId();
                if (openProcessingTransactionIds.contains(txId)) {
                    openProcessingTransactionIds.remove(txId);
                } else {
                    addReportItem(ErrorCode.INVALID_TX_REFERENCE_FOR_PROCESSING_END, currentTestCase.getId(), txId);
                }
            }
        } else if (stepObj instanceof MessagingStep) {
            String txId = ((MessagingStep) stepObj).getTxnId();
            if (StringUtils.isBlank(txId)) {
                if (StringUtils.isBlank(((MessagingStep)stepObj).getHandler())) {
                    addReportItem(ErrorCode.MISSING_TX_AND_HANDLER_STEP, currentTestCase.getId(), Utils.stepNameWithScriptlet(stepObj, currentScriptlet));
                }
            } else {
                if (StringUtils.isNotBlank(((MessagingStep) stepObj).getHandler())) {
                    addReportItem(ErrorCode.STEP_WITH_BOTH_TX_AND_HANDLER, currentTestCase.getId(), Utils.stepNameWithScriptlet(stepObj, currentScriptlet));
                }
                if (openMessagingTransactionIds.isEmpty()) {
                    addReportItem(ErrorCode.MESSAGING_STEP_OUTSIDE_TX, currentTestCase.getId(), Utils.stepNameWithScriptlet(stepObj, currentScriptlet));
                } else {
                    if (!openMessagingTransactionIds.contains(txId)) {
                        addReportItem(ErrorCode.INVALID_TX_REFERENCE_FOR_MESSAGING_STEP, currentTestCase.getId(), Utils.stepNameWithScriptlet(stepObj, currentScriptlet), txId);
                    } else {
                        referencedMessagingTransactionIds.add(txId);
                    }
                }
                if (!((MessagingStep) stepObj).getProperty().isEmpty()) {
                    addReportItem(ErrorCode.STEP_CONNECTION_PROPERTIES_IGNORED, currentTestCase.getId(), Utils.stepNameWithScriptlet(stepObj, currentScriptlet), txId);
                }
            }
        } else if (stepObj instanceof Process) {
            String txId = ((Process)stepObj).getTxnId();
            if (StringUtils.isBlank(txId)) {
                if (StringUtils.isBlank(((Process)stepObj).getHandler())) {
                    addReportItem(ErrorCode.MISSING_TX_AND_HANDLER_STEP, Utils.stepNameWithScriptlet(stepObj, currentScriptlet), currentTestCase.getId());
                }
            } else {
                if (StringUtils.isNotBlank(((Process) stepObj).getHandler())) {
                    addReportItem(ErrorCode.STEP_WITH_BOTH_TX_AND_HANDLER, currentTestCase.getId(), Utils.stepNameWithScriptlet(stepObj, currentScriptlet));
                }
                if (openProcessingTransactionIds.isEmpty()) {
                    addReportItem(ErrorCode.PROCESSING_STEP_OUTSIDE_TX, currentTestCase.getId());
                } else {
                    if (!openProcessingTransactionIds.contains(txId)) {
                        addReportItem(ErrorCode.INVALID_TX_REFERENCE_FOR_PROCESSING_STEP, currentTestCase.getId(), txId);
                    } else {
                        referencedProcessingTransactionIds.add(txId);
                    }
                }
            }
            if (!((Process) stepObj).getProperty().isEmpty()) {
                addReportItem(ErrorCode.STEP_CONNECTION_PROPERTIES_IGNORED, currentTestCase.getId(), Utils.stepNameWithScriptlet(stepObj, currentScriptlet), txId);
            }
        }
    }

    @Override
    public void finaliseTestCase() {
        checkUnusedTransactions();
        super.finaliseTestCase();
    }

    @Override
    public void finaliseScriptlet() {
        checkUnusedTransactions();
        super.finaliseScriptlet();
    }

    private void checkUnusedTransactions() {
        for (String txId: openMessagingTransactionIds) {
            addReportItem(ErrorCode.MESSAGING_TX_NOT_CLOSED, currentTestCase.getId(), txId);
        }
        for (String txId: openProcessingTransactionIds) {
            addReportItem(ErrorCode.PROCESSING_TX_NOT_CLOSED, currentTestCase.getId(), txId);
        }
        allMessagingTransactionIds.removeAll(referencedMessagingTransactionIds);
        for (String txId: allMessagingTransactionIds) {
            addReportItem(ErrorCode.MESSAGING_TX_NOT_USED, currentTestCase.getId(), txId);
        }
        allProcessingTransactionIds.removeAll(referencedProcessingTransactionIds);
        for (String txId: allProcessingTransactionIds) {
            addReportItem(ErrorCode.PROCESSING_TX_NOT_USED, currentTestCase.getId(), txId);
        }
    }

}
