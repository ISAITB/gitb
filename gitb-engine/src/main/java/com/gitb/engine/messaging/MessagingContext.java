package com.gitb.engine.messaging;

import com.gitb.messaging.IMessagingHandler;
import com.gitb.tbs.SUTConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by serbay on 9/29/14.
 *
 * Class that encapsulates objects related to messaging sessions opened with each messaging handler
 * within a test execution.
 */
public class MessagingContext {
	/**
	 * Messaging handler session id.
	 * Should not be confused with the session id in the testbed service interface.
	 */
	private final String sessionId;

	/**
	 * Messaging handler corresponding to the messaging session
	 */
	private final String handlerIdentifier;
	private final IMessagingHandler handler;
	private final List<SUTConfiguration> sutHandlerConfigurations;
	private final Map<String, TransactionContext> transactions;
	private final List<Thread> messagingThreads;

	public MessagingContext(IMessagingHandler handler, String handlerIdentifier, String sessionId, List<SUTConfiguration> sutHandlerConfigurations) {
		this.handler = handler;
		this.handlerIdentifier = handlerIdentifier;
		this.sessionId = sessionId;
		this.sutHandlerConfigurations = new CopyOnWriteArrayList<>(sutHandlerConfigurations);
		this.transactions = new ConcurrentHashMap<>();
        this.messagingThreads = new ArrayList<>();
	}

	public String getSessionId() {
		return sessionId;
	}

	public IMessagingHandler getHandler() {
		return handler;
	}

	public String getHandlerIdentifier() {
		return handlerIdentifier;
	}

	public void setTransaction(String transactionId, TransactionContext transactionContext) {
		transactions.put(transactionId, transactionContext);
	}

	public TransactionContext getTransaction(String transactionId) {
		return transactions.get(transactionId);
	}

	public void removeTransaction(String transactionId) {
		transactions.remove(transactionId);
	}

	public List<SUTConfiguration> getSutHandlerConfigurations() {
		return sutHandlerConfigurations;
	}

	public List<Thread> getMessagingThreads() {
		return messagingThreads;
	}

	public void cleanup() {
		for (Thread thread: messagingThreads) {
			if (thread.isAlive() && !thread.isInterrupted()) {
				thread.interrupt();
			}
		}
	}
}
