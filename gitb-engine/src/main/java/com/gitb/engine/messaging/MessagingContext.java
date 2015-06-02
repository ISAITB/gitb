package com.gitb.engine.messaging;

import com.gitb.core.ActorConfiguration;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.tbs.SUTConfiguration;

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

	public MessagingContext(IMessagingHandler handler, String sessionId, List<ActorConfiguration> actorConfigurations, List<SUTConfiguration> sutHandlerConfigurations) {
		this.handler = handler;
		this.sessionId = sessionId;
		this.actorConfigurations = new CopyOnWriteArrayList<>(actorConfigurations);
		this.sutHandlerConfigurations = new CopyOnWriteArrayList<>(sutHandlerConfigurations);
		this.transactions = new ConcurrentHashMap<>();
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
		return transactions.remove(transactionId);
	}

	public String getHandlerId() {
		return handler.getModuleDefinition().getId();
	}

	public List<SUTConfiguration> getSutHandlerConfigurations() {
		return sutHandlerConfigurations;
	}
}
