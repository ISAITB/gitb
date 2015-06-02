package com.gitb.engine;

import akka.actor.ActorRef;
import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.engine.actors.processors.TestCaseProcessorActor;
import com.gitb.engine.actors.util.ActorUtils;
import com.gitb.engine.commands.session.DestroyCommand;
import com.gitb.messaging.ServerUtils;
import com.gitb.tbs.SUTConfiguration;
import com.gitb.tbs.UserInput;
import com.gitb.utils.ConfigurationUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by serbay on 9/9/14.
 */
public class TestbedServiceTest extends ActorSystemTest {
	private static List<ActorConfiguration> actorConfigurations;

	static {
		actorConfigurations = new ArrayList<>();

		ActorConfiguration receiverActorConfiguration = new ActorConfiguration();
		receiverActorConfiguration.setActor("actor1");
		receiverActorConfiguration.getConfig().add(ConfigurationUtils.constructConfiguration(ServerUtils.IP_ADDRESS_CONFIG_NAME, "127.0.0.1"));
		receiverActorConfiguration.getConfig().add(ConfigurationUtils.constructConfiguration(ServerUtils.PORT_CONFIG_NAME, "7000"));
		actorConfigurations.add(receiverActorConfiguration);
	}

	@Test
	@Ignore
	public void testTestbedService() {
		final String clientInstance = TestbedService.initiate("Sample/Actor1/testcases/TestCaseMessagingClient");
		String serverInstance = TestbedService.initiate("Sample/Actor1/testcases/TestCaseMessagingServer");

		final List<SUTConfiguration> sutConfigurations = TestbedService.configure(serverInstance, actorConfigurations);

		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);

					List<ActorConfiguration> serverActorConfigurations = new ArrayList<>();

					ActorConfiguration serverActorConfiguration = new ActorConfiguration();
					serverActorConfiguration.setActor("actor2");
					serverActorConfiguration.getConfig().add(ConfigurationUtils.constructConfiguration(ServerUtils.IP_ADDRESS_CONFIG_NAME, "127.0.0.1"));
					serverActorConfiguration.getConfig().add(ConfigurationUtils.constructConfiguration(ServerUtils.PORT_CONFIG_NAME, ConfigurationUtils.getConfiguration(sutConfigurations.get(0).getConfigs().get(0).getConfig(), "network.port").getValue()));
					serverActorConfigurations.add(serverActorConfiguration);

					TestbedService.configure(clientInstance, serverActorConfigurations);

					TestbedService.initiatePreliminary(clientInstance);
/*
					List<UserInput> userInputs = new ArrayList<UserInput>();
					UserInput address = new UserInput();
					address.setId("1");
					NamedItemValue addressItem = new NamedItemValue();
					addressItem.setName("network_host");
					addressItem.setValue("127.0.0.1");
					address.getItem().add(addressItem);
					userInputs.add(address);
					UserInput port = new UserInput();
					port.setId("2");
					NamedItemValue portItem = new NamedItemValue();
					portItem.setName("network_port");
					portItem.setValue(ConfigurationUtils.getConfiguration(sutConfigurations.get(0).getConfigs().get(0).getConfig(), "network.port").getValue());
					port.getItem().add(portItem);
					userInputs.add(port);

					TestbedService.provideInput(clientInstance, "0", userInputs);*/

					TestbedService.start(clientInstance);

					TestbedService.stop(clientInstance);

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();

		TestbedService.start(serverInstance);

		TestbedService.stop(serverInstance);
	}

	@Test @Ignore
	public void testTestbedServiceCommands() throws Exception {

		String instance = TestbedService.initiate("Sample/Actor1/testcases/TestCase1");

		ActorRef ref = ActorUtils.getIdentity(system.getActorSystem(), "/user/session/" + instance);

		assertNotNull(ref);

		ActorRef tcref = ActorUtils.getIdentity(system.getActorSystem(), "/user/session/" + instance+"/"+ TestCaseProcessorActor.NAME);

		assertNotNull(tcref);

        List<ActorConfiguration> actorConfigurations = new ArrayList<>();

        Configuration c = new Configuration();
        c.setName("c1");
        c.setValue("v1");

        ActorConfiguration ac = new ActorConfiguration();
        ac.getConfig().add(c);

        actorConfigurations.add(ac);

        Object configureResponse = TestbedService.configure(instance, actorConfigurations);
        TestbedService.start(instance);
		TestbedService.stop(instance);
		TestbedService.restart(instance);

		system
			.getSessionSupervisor()
			.tell(new DestroyCommand(instance), ActorRef.noSender());

		ref = ActorUtils.getIdentity(system.getActorSystem(), "/user/session/"+instance);

		assertNull(ref);
	}
}
