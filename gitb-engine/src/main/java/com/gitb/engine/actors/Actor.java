package com.gitb.engine.actors;

import akka.actor.UntypedActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serbay on 9/8/14.
 *
 * Base actor implementation that is used to track the actor instance's lifetime and incoming
 * messages.
 */
public abstract class Actor extends UntypedActor {

	Logger logger = LoggerFactory.getLogger(Actor.class);

	@Override
	public void onReceive(Object message) {
//		logger.debug(self().path() + " - received - " + message + " - from " + getSender().path());
	}

	@Override
	public void preStart()  throws Exception{
		super.preStart();
		logger.debug(self().path() + " - starting");
	}

	@Override
	public void postStop()  throws Exception{
		super.postStop();
		logger.debug(self().path() + " - stopping");
	}

	@Override
	public String toString() {
		return self().path().toString();
	}
}
