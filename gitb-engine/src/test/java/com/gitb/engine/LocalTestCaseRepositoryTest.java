package com.gitb.engine;

import com.gitb.core.ActorConfiguration;
import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.engine.utils.ArtifactUtils;
import com.gitb.messaging.ServerUtils;
import com.gitb.tbs.SUTConfiguration;
import com.gitb.tdl.TestArtifact;
import com.gitb.tdl.TestCase;
import com.gitb.types.DataType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by senan on 9/11/14.
 */
public class LocalTestCaseRepositoryTest extends ActorSystemTest {

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
    public void testGettingTestCaseFromLocalRepository() {
        TestCase testCase = TestCaseManager.getTestCaseDescription("Sample/Actor1/testcases/TestCase1");
        assertNotNull(testCase);
    }

    @Test
    public void testExecution() throws InterruptedException {

	    List<String> instances = new ArrayList<>();
	    for (int i = 0; i < 1; i++) {
		    String instance = TestbedService.initiate("Sample/Actor1/testcases/TestCase1");
		    instances.add(instance);
	    }
	    Collections.shuffle(instances);
	    for (String instance : instances) {
		    List<SUTConfiguration> sutConfigurations = TestbedService.configure(instance, actorConfigurations);

		    assertNotNull(sutConfigurations);
	    }
	    Collections.shuffle(instances);
	    for (String instance : instances) {
		    TestbedService.start(instance);
	    }
	    Collections.shuffle(instances);
	    for (String instance : instances) {
		    TestbedService.stop(instance);
	    }
    }

	@Test
	public void testArtifactResolution() throws IOException {
		String instance = TestbedService.initiate("Sample/Actor1/testcases/TestCase1");

		List<SUTConfiguration> sutConfigurations = TestbedService.configure(instance, actorConfigurations);

		assertNotNull(sutConfigurations);

		TestCaseContext context = SessionManager.getInstance().getContext(instance);

		assertNotNull(context);

		TestCaseScope scope = context.getScope();

		scope
			.createVariable("messageUUID")
			.setValue(new StringType(UUID.randomUUID().toString()));

		scope
			.createVariable("messageTime")
			.setValue(new StringType(Long.toString(new Date().getTime())));

		scope
			.createVariable("senderID")
			.setValue(new StringType("testSender"));

		scope
			.createVariable("ackTypeCode")
			.setValue(new StringType("testATC"));

		scope
			.createVariable("messageID")
			.setValue(new StringType("testMID"));

		TestArtifact artifact = new TestArtifact();
		artifact.setEncoding("utf-8");
		artifact.setName("Schema1");
		artifact.setValue("Sample/Actor1/artifacts/Schema1.xsd");
		artifact.setType(DataType.OBJECT_DATA_TYPE);

		DataType artifactContent = ArtifactUtils.resolveArtifact(context, scope, artifact);

		assertNotNull(artifactContent);

//		TestbedService.stop(instance);
	}
}
