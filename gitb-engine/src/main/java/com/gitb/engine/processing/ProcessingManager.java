package com.gitb.engine.processing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum ProcessingManager {
    INSTANCE;

    private final Map<String, ProcessingContext> transactions = new ConcurrentHashMap<>();

    public void setTransaction(String transaction, ProcessingContext processingContext) {
        transactions.put(transaction, processingContext);
    }

    public ProcessingContext getProcessingContext(String transaction) {
        return transactions.get(transaction);
    }

    public void removeTransaction(String transaction) {
        transactions.remove(transaction);
    }

}
