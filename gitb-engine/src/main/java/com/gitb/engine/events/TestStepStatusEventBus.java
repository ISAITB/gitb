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

package com.gitb.engine.events;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.event.japi.LookupEventBus;
import com.gitb.engine.events.model.TestStepStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serbay on 9/8/14.
 */
public class TestStepStatusEventBus extends LookupEventBus<TestStepStatusEvent, ActorRef, String> {

	private static final Logger logger = LoggerFactory.getLogger(TestStepStatusEventBus.class);

	private static TestStepStatusEventBus instance;

	public synchronized static TestStepStatusEventBus getInstance() {
		if(instance == null) {
			instance = new TestStepStatusEventBus();
		}

		return instance;
	}

	@Override
	public int mapSize() {
		return 128;
	}

	@Override
	public int compareSubscribers(ActorRef a, ActorRef b) {
		return a.compareTo(b);
	}

	@Override
	public String classify(TestStepStatusEvent event) {
		return event.getSessionId();
	}

	@Override
	public void publish(TestStepStatusEvent event, ActorRef subscriber) {
		if(event.getSender() != null) {
			subscriber.tell(event, event.getSender());
		} else {
			subscriber.tell(event, ActorRef.noSender());
		}
	}
}
