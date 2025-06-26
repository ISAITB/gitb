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

package com.gitb.engine.messaging.handlers.model;

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
		// Do nothing.
	}
}
