package com.gitb.engine.actors.supervisors;

import com.gitb.engine.actors.SessionActor;
import com.gitb.engine.commands.interaction.*;
import com.gitb.engine.commands.session.CreateCommand;
import com.gitb.engine.commands.session.DestroyCommand;
import com.gitb.engine.events.TestStepStatusEventBus;
import com.gitb.exceptions.GITBEngineInternalError;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

/**
 * Created by serbay on 9/4/14.
 *
 * The supervisor of a Test Case Execution instance
 */
public class SessionSupervisor extends com.gitb.engine.actors.Actor {

    private static final Logger logger = LoggerFactory.getLogger(SessionSupervisor.class);
    public static final String NAME = "session";

	@Override
	public void onReceive(Object message) throws Exception {
		super.onReceive(message);
		/*
		 * The SessionSupervisor acts as a go-between for the specific session actors to make sure that all messages
		 * are received and processed in the correct order. Without this it could be that sessions starting in rapid
		 * succession have their messages processed out of sequence depending on arbitrary delivery times to each session
		 * actor (whereas going through the SessionSupervisor guarantees correct sequencing). In addition, while in the
		 * SessionSupervisor we can make direct child actor lookups that will return with no delay (as opposed to searching
		 * for actors by name outside the actor system that may have slight delays).
		 */
		if (message instanceof CreateCommand command) {
			// Create a new session
			String sessionId = command.getSessionId();
			SessionActor.create(getContext(), sessionId);
			ActorRef createdActor =  getContext().findChild(sessionId).orElseThrow(() -> new IllegalStateException("Session [%s] actorRef not created".formatted(sessionId)));
			TestStepStatusEventBus.getInstance().subscribe(createdActor, sessionId);
			logger.debug(MarkerFactory.getDetachedMarker(sessionId), "Created new test session [{}]", sessionId);
		} else if (message instanceof ConfigureCommand command) {
			// Configure a session
			String sessionId = command.getSessionId();
			ActorRef actor = getContext().findChild(sessionId).orElseThrow(() -> new IllegalStateException("Session [%s] actorRef not found to treat ConfigureCommand".formatted(sessionId)));
			actor.tell(command, self());
		} else if (message instanceof InitiatePreliminaryCommand command) {
			// Do the preliminary block
			String sessionId = command.getTestCaseId();
			ActorRef actor = getContext().findChild(sessionId).orElseThrow(() -> new IllegalStateException("Session [%s] actorRef not found to treat InitiatePreliminaryCommand".formatted(sessionId)));
			actor.tell(command, self());
		} else if (message instanceof StartCommand command) {
			// Start a test session
			String sessionId = command.getSessionId();
			ActorRef actor = getContext().findChild(sessionId).orElseThrow(() -> new IllegalStateException("Session [%s] actorRef not found to treat StartCommand".formatted(sessionId)));
			actor.tell(command, self());
		} else if (message instanceof DestroyCommand command) {
			// Destroy a session
			String sessionId = command.getSessionId();
			ActorRef actor = getContext().findChild(sessionId).orElseThrow(() -> new IllegalStateException("Session [%s] actorRef not found to treat DestroyCommand".formatted(sessionId)));
			TestStepStatusEventBus.getInstance().unsubscribe(actor, sessionId);
			actor.tell(PoisonPill.getInstance(), self());
		} else if (message instanceof ConnectionClosedEvent command) {
			// Handle connection closure
			String sessionId = command.getSessionId();
			ActorRef actor = getContext().findChild(sessionId).orElseThrow(() -> new IllegalStateException("Session [%s] actorRef not found to treat ConnectionClosedEvent".formatted(sessionId)));
			actor.tell(command, self());
		} else if (message instanceof StopCommand command) {
			// Stop a test session
			String sessionId = command.getSessionId();
			ActorRef actor = getContext().findChild(sessionId).orElseThrow(() -> new IllegalStateException("Session [%s] actorRef not found to treat StopCommand".formatted(sessionId)));
			actor.tell(command, self());
		} else if (message instanceof RestartCommand command) {
			// Restart session.
			String sessionId = command.getSessionId();
			ActorRef actor = getContext().findChild(sessionId).orElseThrow(() -> new IllegalStateException("Session [%s] actorRef not found to treat RestartCommand".formatted(sessionId)));
			actor.tell(command, self());
			self().tell(new CreateCommand(command.getNewSessionId()), self());
		} else {
            logger.error("Invalid command [{}]", message.getClass().getName());
            throw new GITBEngineInternalError("Invalid command [%s]".formatted(message.getClass().getName()));
		}
	}

	public static ActorRef create(ActorSystem system) {
		return system.actorOf(Props.create(SessionSupervisor.class), NAME);
	}
}
