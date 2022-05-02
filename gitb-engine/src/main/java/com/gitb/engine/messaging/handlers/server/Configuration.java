package com.gitb.engine.messaging.handlers.server;

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

}
