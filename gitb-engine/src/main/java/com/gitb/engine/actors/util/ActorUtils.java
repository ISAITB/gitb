package com.gitb.engine.actors.util;

import akka.actor.*;
import akka.pattern.AskableActorRef;
import akka.pattern.AskableActorSelection;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Created by serbay on 9/8/14.
 */
public class ActorUtils {

	public static Object askBlocking(ActorSelection ref, Object message) throws Exception {
		Timeout t = Timeout.durationToTimeout(FiniteDuration.apply(30, TimeUnit.SECONDS));
		Future<ActorRef> f = ref.resolveOne(t);
		ActorRef actorRef = Await.result(f, t.duration());
		return askBlocking(actorRef, message);
	}

	public static Object askBlocking(ActorRef ref, Object message) throws Exception {
		Timeout t = Timeout.durationToTimeout(FiniteDuration.apply(30, TimeUnit.SECONDS));

		AskableActorRef askRef = new AskableActorRef(ref);

		Future<Object> f = askRef.ask(message, t);

		Object result = Await.result(f, t.duration());

		if(result instanceof Exception) {
			throw (Exception) result;
		}

		return result;
	}

	public static ActorRef getIdentity(ActorSystem system, String path) throws Exception {

		ActorSelection sel = system.actorSelection(path);

		Timeout t = Timeout.apply(30, TimeUnit.SECONDS);

		AskableActorSelection asker = new AskableActorSelection(sel);

		Future<Object> f = asker.ask(new Identify(1), t);

		ActorIdentity identity = (ActorIdentity) Await.result(f, t.duration());

		return identity.getActorRef().orElseThrow(() -> new IllegalStateException("ActorRef for ["+path+"] not found"));
	}
}
