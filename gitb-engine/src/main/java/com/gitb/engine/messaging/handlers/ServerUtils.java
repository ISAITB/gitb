/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.engine.messaging.handlers;

import com.gitb.core.ActorConfiguration;
import com.gitb.engine.messaging.handlers.server.tcp.TCPMessagingServer;
import com.gitb.types.DataType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;

import java.io.IOException;
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

        for (Map.Entry<String, DataType> pair : headerMap.entrySet()) {
            String headerName = pair.getKey();
            if (headerName.equalsIgnoreCase(header)) {
                StringType headerType = ((StringType) pair.getValue());
                return headerType.getValue();
            }
        }

        return null;
    }
}
