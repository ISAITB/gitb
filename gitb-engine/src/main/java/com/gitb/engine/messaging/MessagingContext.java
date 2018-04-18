package com.gitb.engine.messaging;

import com.gitb.core.ActorConfiguration;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.tbs.SUTConfiguration;

import java.util.ArrayList;
import java.util.Collection;
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
	private final IMessagingHandler handler;
	private final List<ActorConfiguration> actorConfigurations;
	private final List<SUTConfiguration> sutHandlerConfigurations;
	private final Map<String, TransactionContext> transactions;
	private final List<Thread> messagingThreads;
    private int transactionCount; //number of different transactions that the handler is responsible for

	public MessagingContext(IMessagingHandler handler, String sessionId, List<ActorConfiguration> actorConfigurations, List<SUTConfiguration> sutHandlerConfigurations, int transactionCount) {
		this.handler = handler;
		this.sessionId = sessionId;
		this.actorConfigurations = new CopyOnWriteArrayList<>(actorConfigurations);
		this.sutHandlerConfigurations = new CopyOnWriteArrayList<>(sutHandlerConfigurations);
		this.transactions = new ConcurrentHashMap<>();
        this.transactionCount = transactionCount;
        this.messagingThreads = new ArrayList<>();
	}

	public String getSessionId() {
		return sessionId;
	}

	public IMessagingHandler getHandler() {
		return handler;
	}

	public List<ActorConfiguration> getActorConfigurations() {
		return actorConfigurations;
	}

	public Collection<TransactionContext> getTransactions() {
		return transactions.values();
	}

	public void setTransaction(String transactionId, TransactionContext transactionContext) {
		transactions.put(transactionId, transactionContext);
	}

	public TransactionContext getTransaction(String transactionId) {
		return transactions.get(transactionId);
	}

	public TransactionContext removeTransaction(String transactionId) {
        transactionCount--;
		return transactions.remove(transactionId);
	}

    public boolean hasMoreTransactions() {
        return transactionCount > 0;
    }

	public String getHandlerId() {
		return handler.getModuleDefinition().getId();
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
