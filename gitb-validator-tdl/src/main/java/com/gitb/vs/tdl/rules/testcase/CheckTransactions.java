package com.gitb.vs.tdl.rules.testcase;

import com.gitb.tdl.Process;
import com.gitb.tdl.*;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.util.Utils;

import java.util.HashSet;
import java.util.Set;

public class CheckTransactions extends AbstractTestCaseObserver {

    private Set<String> openMessagingTransactionIds;
    private Set<String> openProcessingTransactionIds;

    @Override
    public void initialiseTestCase(TestCase currentTestCase) {
        super.initialiseTestCase(currentTestCase);
        openMessagingTransactionIds = new HashSet<>();
        openProcessingTransactionIds = new HashSet<>();
    }

    @Override
    public void initialiseScriptlet(Scriptlet scriptlet) {
        super.initialiseScriptlet(scriptlet);
        openMessagingTransactionIds = new HashSet<>();
        openProcessingTransactionIds = new HashSet<>();
    }

    @Override
    public void handleStep(Object stepObj) {
        if (stepObj instanceof BeginTransaction) {
            openMessagingTransactionIds.add(((BeginTransaction)stepObj).getTxnId());
        } else if (stepObj instanceof BeginProcessingTransaction) {
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
                addReportItem(ErrorCode.MESSAGING_STEP_OUTSIDE_TX, currentTestCase.getId(), Utils.getStepName(stepObj));
            } else {
                String txId = ((MessagingStep)stepObj).getTxnId();
                if (!openMessagingTransactionIds.contains(txId)) {
                    addReportItem(ErrorCode.INVALID_TX_REFERENCE_FOR_MESSAGING_STEP, currentTestCase.getId(), Utils.getStepName(stepObj), txId);
                }
            }
        } else if (stepObj instanceof Process) {
            if (openProcessingTransactionIds.isEmpty()) {
                addReportItem(ErrorCode.PROCESSING_STEP_OUTSIDE_TX, currentTestCase.getId());
            } else {
                String txId = ((Process)stepObj).getTxnId();
                if (!openProcessingTransactionIds.contains(txId)) {
                    addReportItem(ErrorCode.INVALID_TX_REFERENCE_FOR_PROCESSING_STEP, currentTestCase.getId(), txId);
                }
            }
        }
    }

}
