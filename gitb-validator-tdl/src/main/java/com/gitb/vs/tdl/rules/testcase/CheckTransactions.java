package com.gitb.vs.tdl.rules.testcase;

import com.gitb.tdl.Process;
import com.gitb.tdl.*;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.util.Utils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

public class CheckTransactions extends AbstractTestCaseObserver {

    private Set<String> allMessagingTransactionIds = new HashSet<>();
    private Set<String> allProcessingTransactionIds = new HashSet<>();
    private Set<String> openMessagingTransactionIds = new HashSet<>();
    private Set<String> openProcessingTransactionIds = new HashSet<>();
    private Set<String> referencedMessagingTransactionIds = new HashSet<>();
    private Set<String> referencedProcessingTransactionIds = new HashSet<>();

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
            if (openMessagingTransactionIds.isEmpty()) {
                addReportItem(ErrorCode.MESSAGING_STEP_OUTSIDE_TX, currentTestCase.getId(), Utils.stepNameWithScriptlet(stepObj, currentScriptlet));
            } else {
                String txId = ((MessagingStep)stepObj).getTxnId();
                if (!openMessagingTransactionIds.contains(txId)) {
                    addReportItem(ErrorCode.INVALID_TX_REFERENCE_FOR_MESSAGING_STEP, currentTestCase.getId(), Utils.stepNameWithScriptlet(stepObj, currentScriptlet), txId);
                } else {
                    referencedMessagingTransactionIds.add(txId);
                }
            }
        } else if (stepObj instanceof Process) {
            String txId = ((Process)stepObj).getTxnId();
            if (StringUtils.isBlank(txId)) {
                if (StringUtils.isBlank(((Process)stepObj).getHandler())) {
                    addReportItem(ErrorCode.MISSING_TX_AND_HANDLER_FOR_PROCESSING_STEP, currentTestCase.getId());
                }
            } else {
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
