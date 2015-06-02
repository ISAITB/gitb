package com.gitb.engine;

import akka.actor.ActorRef;
import com.gitb.core.ActorConfiguration;
import com.gitb.engine.commands.session.DestroyCommand;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.actors.util.ActorUtils;
import com.gitb.core.Configuration;
import com.gitb.tbs.SUTConfiguration;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by serbay on 9/5/14.
 */
public class ConfigureTest extends ActorSystemTest {

	Logger logger = LoggerFactory.getLogger(ConfigureTest.class);

	@Test
	public void testConfigureCommand() throws Exception {

		String instance = TestbedService.initiate("Sample/Actor1/testcases/TestCase1");

		ActorRef ref = ActorUtils.getIdentity(system.getActorSystem(), "/user/session/" + instance);

		assertNotNull(ref);

        List<ActorConfiguration> actorConfigurations = new ArrayList<>();

		Configuration c = new Configuration();
		c.setName("c1");
		c.setValue("v1");

        ActorConfiguration ac = new ActorConfiguration();
        ac.getConfig().add(c);

        actorConfigurations.add(ac);

		List<SUTConfiguration> sutConfigurations = TestbedService.configure(instance, actorConfigurations);

		TestCaseContext context = SessionManager
									.getInstance()
									.getContext(instance);

		assertNotNull(context);
		assertNotNull(sutConfigurations);

		//assertFalse(context.getConfigurations("test-actor-2").isEmpty());

		system
			.getSessionSupervisor()
			.tell(new DestroyCommand(instance), ActorRef.noSender());

		ref = ActorUtils.getIdentity(system.getActorSystem(), "/user/session/"+instance);

		assertNull(ref);
	}
}