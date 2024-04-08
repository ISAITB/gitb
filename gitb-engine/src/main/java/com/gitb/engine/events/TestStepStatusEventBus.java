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
