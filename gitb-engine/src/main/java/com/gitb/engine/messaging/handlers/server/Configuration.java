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
