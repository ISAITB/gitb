package com.gitb.messaging.model;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by serbay on 9/25/14.
 */
public class TransactionContext {
	private final String transactionId;
	private final long startTime;
	private final ActorConfiguration self;
	private final ActorConfiguration with;
	private final List<Configuration> configurations;
    private final List<Exception> nonCriticalErrors;
	private final Map<Class<?>, Object> parameters;

	public TransactionContext(String transactionId, ActorConfiguration self, ActorConfiguration with, List<Configuration> configurations) {
		this.transactionId = transactionId;
        this.self = self;
		this.with = with;
		this.configurations = configurations;
		this.parameters = new ConcurrentHashMap<>();
		this.startTime = Calendar.getInstance().getTimeInMillis();
        this.nonCriticalErrors = new CopyOnWriteArrayList<>();
    }

	public String getTransactionId() {
		return transactionId;
	}

	public long getStartTime() {
		return startTime;
	}

    public ActorConfiguration getSelf() {
        return self;
    }

    public ActorConfiguration getWith() {
		return with;
	}

	public List<Configuration> getConfigurations() {
		return configurations;
	}

	public <T> void setParameter(Class<T> clazz, T parameter) {
		parameters.put(clazz, parameter);
	}

	public <T> T getParameter(Class<T> clazz) {
		return (T) parameters.get(clazz);
	}

	public Collection<Object> getParameters() {
		return new ArrayList<>(parameters.values());
	}

    public Collection<Exception> getNonCriticalErrors() {
        return new ArrayList<>(nonCriticalErrors);
    }

    public void addNonCriticalError(Exception exception) {
        nonCriticalErrors.add(exception);
    }

    public void clearNonCriticalErrors() {
        nonCriticalErrors.clear();
    }

	public void end() {
	}
}
