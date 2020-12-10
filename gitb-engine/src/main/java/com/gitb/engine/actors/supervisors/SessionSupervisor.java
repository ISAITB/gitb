package com.gitb.engine.actors.supervisors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.gitb.engine.actors.SessionActor;
import com.gitb.engine.commands.session.CreateCommand;
import com.gitb.engine.commands.session.DestroyCommand;
import com.gitb.engine.events.TestStepStatusEventBus;
import com.gitb.exceptions.GITBEngineInternalError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

/**
 * Created by serbay on 9/4/14.
 *
 * The supervisor of a Test Case Execution instance
 */
public class SessionSupervisor extends com.gitb.engine.actors.Actor {
    private static Logger logger = LoggerFactory.getLogger(SessionSupervisor.class);

    public static final String NAME = "session";

	@Override
	public void onReceive(Object message) throws Exception {
		super.onReceive(message);
        //Creating a session for Test Case Execution
		if(message instanceof CreateCommand) {
			String sessionId = ((CreateCommand) message).getSessionId();
			ActorRef actor = SessionActor.create(getContext(), sessionId);
			TestStepStatusEventBus
				.getInstance()
				.subscribe(actor, sessionId);
			logger.debug(MarkerFactory.getDetachedMarker(sessionId), "Created new test session [" + sessionId + "]");
		}
        //Closing a Test Case Execution session
        else if(message instanceof DestroyCommand) {
			String sessionId = ((DestroyCommand) message).getSessionId();
			ActorRef actor = getContext().findChild(sessionId).orElseThrow(() -> new IllegalStateException("Session actorRef not found"));

			TestStepStatusEventBus
				.getInstance()
				.unsubscribe(actor, sessionId);

			actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
		} else {
            logger.error("Invalid command ["+message.getClass().getName()+"]");
            throw new GITBEngineInternalError("Invalid command ["+message.getClass().getName()+"]");
		}
	}

	public static ActorRef create(ActorSystem system) {
		return system.actorOf(Props.create(SessionSupervisor.class), NAME);
	}
}
