package com.gitb.messaging;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.messaging.layer.application.dns.DNSMessagingHandler;
import com.gitb.messaging.model.InitiateResponse;
import com.gitb.utils.ConfigurationUtils;
import org.junit.Test;
import org.xbill.DNS.Address;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by serbay.
 */
public class DNSMessagingHandlerTest {

	public static final String TEST_DOMAIN = "B-351cd3bce374194b60c770852a53d0e6.iso6523-actorid-upis.gitb.com.";
	public static final String TEST_DOMAIN_ADDRESS = "192.168.1.31";

	@Test
	public void testDNSQuery() {
		IMessagingHandler handler = new DNSMessagingHandler();

		List<ActorConfiguration> clientActorConfigurations = new ArrayList<>();

		ActorConfiguration receiverActorConfiguration = new ActorConfiguration();
		receiverActorConfiguration.setActor("test");
		receiverActorConfiguration.getConfig().add(ConfigurationUtils.constructConfiguration(ServerUtils.IP_ADDRESS_CONFIG_NAME, "192.168.1.28"));
		receiverActorConfiguration.getConfig().add(ConfigurationUtils.constructConfiguration(ServerUtils.PORT_CONFIG_NAME, "7000"));
		clientActorConfigurations.add(receiverActorConfiguration);

		InitiateResponse response = handler.initiate(clientActorConfigurations);
		assertTrue(response.getActorConfigurations().size() == 1);

		final ActorConfiguration dnsServerActorConfiguration = response.getActorConfigurations().get(0);
		Configuration dnsPortConfiguration = ConfigurationUtils.getConfiguration(dnsServerActorConfiguration.getConfig(), ServerUtils.PORT_CONFIG_NAME);
		assertNotNull(dnsPortConfiguration);

		assertEquals(dnsPortConfiguration.getValue(), "53");

		handler.beginTransaction(response.getSessionId(), "t1", "test", dnsServerActorConfiguration.getActor(), new ArrayList<Configuration>());

		List<Configuration> dnsConfigurations = new ArrayList<>();
		Configuration domainConfiguration = new Configuration();
		domainConfiguration.setName(DNSMessagingHandler.DNS_DOMAIN_CONFIG_NAME);
		domainConfiguration.setValue(TEST_DOMAIN);
		dnsConfigurations.add(domainConfiguration);
		Configuration addressConfiguration = new Configuration();
		addressConfiguration.setName(DNSMessagingHandler.DNS_ADDRESS_FIELD_NAME);
		addressConfiguration.setValue(TEST_DOMAIN_ADDRESS);
		dnsConfigurations.add(addressConfiguration);

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000000);

					Configuration dnsServerAddressConfiguration = ConfigurationUtils.getConfiguration(dnsServerActorConfiguration.getConfig(), ServerUtils.IP_ADDRESS_CONFIG_NAME);

					assertNotNull(dnsServerAddressConfiguration);

					System.setProperty("dns.server", dnsServerAddressConfiguration.getValue());
					InetAddress addr = Address.getByName(TEST_DOMAIN);
					assertEquals(addr.getHostAddress(), TEST_DOMAIN_ADDRESS);

				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		}).start();

		MessagingReport report = handler.receiveMessage(response.getSessionId(), "t1", dnsConfigurations);

		assertNotNull(report.getReport());
		//assertTrue(report.getReport().getResult() == TestResultType.SUCCESS);
	}

}
