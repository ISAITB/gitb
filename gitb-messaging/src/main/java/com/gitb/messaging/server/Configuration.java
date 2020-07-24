package com.gitb.messaging.server;

import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 * Created by serbay on 9/24/14.
 */
public class Configuration {
	private final int start;
	private final int end;
	private final String ipAddress;
	private final String actorName;
    private final String keystoreLocation;
    private final String keystorePassword;
    private final String defaultAlias;

	public Configuration(int start, int end, String ipAddress, String actorName, String keystoreLocation, String keystorePassword, String defaultAlias) {
		this.start = start;
		this.end = end;
		this.ipAddress = ipAddress;
		this.actorName = actorName;
        this.keystoreLocation = keystoreLocation;
        this.keystorePassword = keystorePassword;
        this.defaultAlias = defaultAlias;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public String getActorName() {
		return actorName;
	}

    public String getKeystoreLocation() {
        return keystoreLocation;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getDefaultAlias() {
        return defaultAlias;
    }

	public static Configuration defaultConfiguration() {
		try {
			CompositeConfiguration config = new CompositeConfiguration();
			config.addConfiguration(new SystemConfiguration());
			config.addConfiguration(new EnvironmentConfiguration());
			config.addConfiguration(new Configurations().properties("messaging-module.properties"));

			return new Configuration(
				config.getInt("gitb.messaging.start-port", 8080),
				config.getInt("gitb.messaging.end-port", 9000),
				config.getString("gitb.messaging.server-ip-address"),
				config.getString("gitb.messaging.actor-name"),
                config.getString("gitb.messaging.keystore.location"),
                config.getString("gitb.messaging.keystore.password"),
                config.getString("gitb.messaging.default.alias")
			);
		} catch (ConfigurationException e) {
			throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Module properties could not be read"), e);
		}
	}
}
