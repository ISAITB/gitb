package com.gitb.utils;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.Configuration;
import com.gitb.utils.map.Tuple;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by serbay.
 */
public class ActorUtils {

	private static final Pattern ACTOR_ID_ENDPOINT_NAME_PATTERN = Pattern.compile("([a-zA-Z0-9_]+)(?:.([a-zA-Z0-9_]+))?");

	public static String extractActorId(String actorIdEndpointName) {
		Matcher matcher = ACTOR_ID_ENDPOINT_NAME_PATTERN.matcher(actorIdEndpointName);

		if(!matcher.matches()) {
			return null;
		}

		return matcher.group(1);
	}

	public static String extractEndpointName(String actorIdEndpointName) {
		Matcher matcher = ACTOR_ID_ENDPOINT_NAME_PATTERN.matcher(actorIdEndpointName);

		if(!matcher.matches() || matcher.groupCount() < 2) {
			return null;
		}

		return matcher.group(2);
	}

	public static String extractActorId(Tuple<String> tuple) {
		return tuple.getContents()[0];
	}

	public static String extractEndpointName(Tuple<String> tuple) {
		return tuple.getContents()[1];
	}

	public static Tuple<String> toTuple(String actorId, String endpointName) {
		return new Tuple<>(new String[] {actorId, endpointName});
	}

	public static ActorConfiguration getActorConfiguration(Collection<ActorConfiguration> actorConfigurations, String name, String endpoint) {
		for(ActorConfiguration actorConfiguration : actorConfigurations) {
			if((endpoint == null && actorConfiguration.getActor().equals(name))
                    || (actorConfiguration.getEndpoint() == null && actorConfiguration.getActor().equals(name))
				    || (endpoint != null && actorConfiguration.getEndpoint() != null && actorConfiguration.getActor().equals(name) && actorConfiguration.getEndpoint().equals(endpoint))) {
				return actorConfiguration;
			}
		}

		return null;
	}

	public static ActorConfiguration copyActorConfiguration(String name, String endpoint, ActorConfiguration sutHandlerConfiguration) {
		ActorConfiguration sutHandlerConfigurationCopy = new ActorConfiguration();
		sutHandlerConfigurationCopy.setActor(name);
		sutHandlerConfigurationCopy.setEndpoint(endpoint);
		for(Configuration configuration : sutHandlerConfiguration.getConfig()) {
			sutHandlerConfigurationCopy.getConfig().add(ConfigurationUtils.constructConfiguration(configuration.getName(), configuration.getValue()));
		}
		return sutHandlerConfigurationCopy;
	}
}
