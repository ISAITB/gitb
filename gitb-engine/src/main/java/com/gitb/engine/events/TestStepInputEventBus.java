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
import com.gitb.engine.events.model.InputEvent;

/**
 * Created by tuncay on 9/23/14.
 */
public class TestStepInputEventBus extends LookupEventBus<InputEvent, ActorRef, String> {
    public static final String SEPERATOR = ":";
    private static TestStepInputEventBus instance;

    public synchronized static TestStepInputEventBus getInstance() {
        if(instance == null) {
            instance = new TestStepInputEventBus();
        }

        return instance;
    }

    @Override
    public int mapSize() {
        return 128;
    }

    @Override
    public int compareSubscribers(ActorRef actorRef, ActorRef actorRef2) {
        return actorRef.compareTo(actorRef2);
    }

    @Override
    public String classify(InputEvent inputEvent) {
        return getClassifier(inputEvent.getSessionId(),inputEvent.getStepId());
    }

    @Override
    public void publish(InputEvent inputEvent, ActorRef actorRef) {
        actorRef.tell(inputEvent, ActorRef.noSender());
    }

    public static String getClassifier(String sessionId, String stepId) {
        if(stepId == null)
            return sessionId;
        else
            return sessionId + SEPERATOR + stepId;
    }
}
