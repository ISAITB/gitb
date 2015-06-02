package com.gitb.engine;

import akka.actor.ActorRef;
import com.gitb.engine.commands.session.DestroyCommand;
import com.gitb.engine.actors.util.ActorUtils;
import org.junit.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by serbay on 9/4/14.
 */
public class SessionTest extends ActorSystemTest {

	@Test
	public void testCreateAndDestroySession() throws Exception {

		String instance = TestbedService.initiate("Sample/Actor1/testcases/TestCase1");

		ActorRef ref = ActorUtils.getIdentity(system.getActorSystem(), "/user/session/" + instance);

		assertNotNull(ref);

		system
			.getSessionSupervisor()
			.tell(new DestroyCommand(instance), ActorRef.noSender());

		ref = ActorUtils.getIdentity(system.getActorSystem(), "/user/session/"+instance);

		assertNull(ref);
	}

	@Test
	public void testMultipleSessionManagement() throws Exception {
		ActorRef sessionSupervisor = system.getSessionSupervisor();

		List<String> instances = new ArrayList<>();

		for (int i = 0; i < 100; i++) {

			String instance = TestbedService.initiate("Sample/Actor1/testcases/TestCase1");

			instances.add(instance);
		}

		Collections.shuffle(instances);

		for(String instance : instances) {
			ActorRef ref = ActorUtils.getIdentity(system.getActorSystem(), "/user/session/"+instance);

			assertNotNull(ref);
		}

		Collections.shuffle(instances);

		for(String instance : instances) {
			sessionSupervisor.tell(new DestroyCommand(instance), ActorRef.noSender());

			ActorRef ref = ActorUtils.getIdentity(system.getActorSystem(), "/user/session/"+instance);

			assertNull(ref);
		}
	}
}
