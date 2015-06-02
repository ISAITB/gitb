package com.gitb.messaging;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.messaging.model.InitiateResponse;
import com.gitb.tr.TestResultType;
import com.gitb.utils.ActorUtils;
import com.gitb.utils.ConfigurationUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by serbay on 9/26/14.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class MessagingHandlerTest {
	protected static IMessagingHandler receiverHandler;
	protected static IMessagingHandler senderHandler;
    protected static IMessagingHandler listenerHandler;

	protected static String receiverSessionId;
	protected static String senderSessionId;
    protected static String listenerSessionId;
	protected static List<ActorConfiguration> senderActorConfigurations;
	protected static List<ActorConfiguration> receiverActorConfigurations;
    protected static List<ActorConfiguration> listenerActorConfigurations;

	protected abstract void messageReceived(Message message);
	protected abstract Message constructMessage();

	@Test
	public void test_1_InitSession() {
        ActorConfiguration senderActorConfiguration, receiverActorConfiguration;
        ActorConfiguration proxySenderActorConfiguration, proxyReceiverActorConfiguration;

        receiverActorConfigurations = new ArrayList<>();
        listenerActorConfigurations = new ArrayList<>();
		senderActorConfigurations = new ArrayList<>();

        /*
        Create two actor configurations one of them will act as a receiver
        the other one will act as a sender.
         */
		senderActorConfiguration = new ActorConfiguration();
		senderActorConfiguration.setActor("test");
		senderActorConfiguration.getConfig().add(ConfigurationUtils.constructConfiguration(ServerUtils.IP_ADDRESS_CONFIG_NAME, "127.0.0.1"));
		senderActorConfiguration.getConfig().add(ConfigurationUtils.constructConfiguration(ServerUtils.PORT_CONFIG_NAME, "7000"));
		receiverActorConfigurations.add(senderActorConfiguration);

//        receiverActorConfigurations.add(senderActorConfiguration);

        InitiateResponse receiverResponse = receiverHandler.initiate(receiverActorConfigurations);
        assertNotNull(receiverResponse);

        receiverActorConfiguration = receiverResponse.getActorConfigurations().get(0);
        receiverActorConfiguration.setActor("test2");

        listenerActorConfigurations.add(senderActorConfiguration);
        listenerActorConfigurations.add(receiverActorConfiguration);

        InitiateResponse listenerResponse = listenerHandler.initiate(listenerActorConfigurations);
        assertNotNull(listenerResponse);

        proxyReceiverActorConfiguration = listenerResponse.getActorConfigurations().get(0);
        proxyReceiverActorConfiguration.setActor("test2");
        proxySenderActorConfiguration = listenerResponse.getActorConfigurations().get(1);
        proxySenderActorConfiguration.setActor("test");

        listenerSessionId = listenerResponse.getSessionId();
        assertNotNull(listenerSessionId);

		receiverSessionId = receiverResponse.getSessionId();
		assertNotNull(receiverSessionId);

        senderActorConfigurations.add(proxyReceiverActorConfiguration);

		InitiateResponse response2 = senderHandler.initiate(senderActorConfigurations);

		senderSessionId = response2.getSessionId();
		assertNotNull(senderSessionId);
	}

	@Test
	public void test_2_SendReceiveMessage() {

		receiverHandler.beginTransaction(receiverSessionId, "t1", "test", "test2", new ArrayList<Configuration>());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);

                    listenerHandler.beginTransaction(listenerSessionId, "t2", "test", "test2", new ArrayList<Configuration>());

                    listenerHandler.listenMessage(listenerSessionId, "t2", "test", "test2", new ArrayList<Configuration>());

                    listenerHandler.endTransaction(listenerSessionId, "t2");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(2000);

					senderHandler.beginTransaction(senderSessionId, "t3", "test2", "test", new ArrayList<Configuration>());

					senderHandler.sendMessage(senderSessionId, "t3", sendConfigurations(), constructMessage());

					senderHandler.endTransaction(senderSessionId, "t3");

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();

		MessagingReport report = receiverHandler.receiveMessage(receiverSessionId, "t1", receiveConfigurations());

		assertNotNull(report);

        assertNotNull(report.getReport());

        assertTrue(report.getReport().getResult() == TestResultType.SUCCESS);

		assertNotNull(report.getMessage());

		messageReceived(report.getMessage());

		receiverHandler.endTransaction(receiverSessionId, "t1");
	}

	@Test
	public void test_3_EndSession() {
        listenerHandler.endSession(listenerSessionId);
		receiverHandler.endSession(receiverSessionId);
		senderHandler.endSession(senderSessionId);
	}

	protected List<Configuration> sendConfigurations() {
		return new ArrayList<>();
	}
	protected List<Configuration> receiveConfigurations() {
		return new ArrayList<>();
	};

}
