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

package com.gitb.engine.messaging.handlers.server.exceptions;

import java.net.InetAddress;

/**
 * Created by serbay on 9/24/14.
 */
public class ExistingSessionException extends Exception {
	private final InetAddress address;
	private final String sessionId;

	public ExistingSessionException(InetAddress address) {
		this.address = address;
		this.sessionId = null;
	}

	public ExistingSessionException(InetAddress address, String sessionId) {
		this.address = address;
		this.sessionId = sessionId;
	}

	@Override
	public String toString() {
		return "ExistingSessionException{" +
			"address=" + address +
			", sessionId='" + sessionId + '\'' +
			'}';
	}
}
