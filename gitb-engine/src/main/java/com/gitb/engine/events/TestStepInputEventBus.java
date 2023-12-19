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
