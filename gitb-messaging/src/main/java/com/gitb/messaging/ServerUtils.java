package com.gitb.messaging;

import com.gitb.core.ActorConfiguration;
import com.gitb.messaging.server.tcp.TCPMessagingServer;
import com.gitb.types.DataType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by serbay on 9/25/14.
 */
public class ServerUtils {
	public static final String IP_ADDRESS_CONFIG_NAME = "network.host";
	public static final String PORT_CONFIG_NAME = "network.port";

	public static ActorConfiguration getActorConfiguration(String name, String endpoint, int port) throws IOException {
		TCPMessagingServer server = TCPMessagingServer.getInstance();

		ActorConfiguration actorConfiguration = new ActorConfiguration();

		actorConfiguration.setActor(name);
		actorConfiguration.setEndpoint(endpoint);
		actorConfiguration
			.getConfig()
			.add(ConfigurationUtils.constructConfiguration(IP_ADDRESS_CONFIG_NAME, server.getConfiguration().getIpAddress()));
		actorConfiguration
			.getConfig()
			.add(ConfigurationUtils.constructConfiguration(PORT_CONFIG_NAME, Integer.toString(port)));

		return actorConfiguration;
	}

    public static String getHeader(MapType headers, String header) {
        Map<String, DataType> headerMap = (Map<String, DataType>) headers.getValue();
        Iterator iterator = headerMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry pair  = (Map.Entry)iterator.next();
            String headerName  = (String) pair.getKey();
            if(headerName.equals(header)) {
                StringType headerType  = ((StringType) pair.getValue());
                return (String) headerType.getValue();
            }
        }

        return null;
    }
}
