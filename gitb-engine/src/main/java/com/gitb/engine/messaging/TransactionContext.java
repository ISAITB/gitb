package com.gitb.engine.messaging;

/**
 * Created by serbay on 9/29/14.
 *
 * Class that encapsulates objects related to a transaction residing in a messaging session.
 */
public class TransactionContext {
	private final String transactionId;

	public TransactionContext(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getTransactionId() {
		return transactionId;
	}
}
