/*
 * Copyright (C) 2026 European Union
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

package utils.signature;

import java.time.Duration;

/**
 * The settings to use to obtain trusted timestamps from a Time Stamping Authority (TSA) [RFC 3161].
 *
 * @param url The address of the TSA.
 * @param username The user name for HTTP basic authentication (null if no authentication is needed).
 * @param password The password for HTTP basic authentication (null if no authentication is needed).
 * @param policyOid The policy OID to request (null to let the TSA apply its default policy).
 * @param timeout The maximum time to wait for the TSA (for connecting and for its response).
 */
public record TsaSettings(String url, String username, String password, String policyOid, Duration timeout) {

    /**
     * The default time to wait for a TSA.
     */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    public TsaSettings {
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT;
        }
    }

    /**
     * @return Whether HTTP basic authentication is to be used.
     */
    public boolean hasCredentials() {
        return username != null && !username.isEmpty();
    }

    /**
     * Never include the password.
     */
    @Override
    public String toString() {
        return "TsaSettings[url=" + url + ", authentication=" + hasCredentials() + ", policyOid=" + policyOid + ", timeout=" + timeout + "]";
    }

}
