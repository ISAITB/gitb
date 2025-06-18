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

package com.gitb.engine.actors;

import org.apache.pekko.actor.UntypedAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serbay on 9/8/14.
 *
 * Base actor implementation that is used to track the actor instance's lifetime and incoming
 * messages.
 */
public abstract class Actor extends UntypedAbstractActor {

	Logger logger = LoggerFactory.getLogger(Actor.class);

	@Override
	public void onReceive(Object message) throws Exception {}

	@Override
	public void preStart()  throws Exception{
		super.preStart();
		logger.debug("{} - starting", self().path());
	}

	@Override
	public void postStop()  throws Exception{
		super.postStop();
		logger.debug("{} - stopping", self().path());
	}

	@Override
	public String toString() {
		return self().path().toString();
	}
}
