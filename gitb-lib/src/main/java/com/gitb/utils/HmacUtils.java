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

package com.gitb.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HmacUtils {

    private static final Logger LOG = LoggerFactory.getLogger(HmacUtils.class);

    /**
     * The HMAC token header name.
     */
    public static final String HMAC_HEADER_TOKEN = "X-Authorization";
    /**
     * The timestamp header name.
     */
    public static final String HMAC_HEADER_TIMESTAMP = "X-Timestamp";

    private static String hmacSecretKey;
    private static Long hmacMaxValidityWindow;

    private static boolean isConfigured() {
        return hmacSecretKey != null && hmacMaxValidityWindow != null;
    }

    public static void configure(String hmacSecretKey, Long hmacMaxValidityWindow) {
        HmacUtils.hmacSecretKey = hmacSecretKey;
        HmacUtils.hmacMaxValidityWindow = hmacMaxValidityWindow;
    }

    public static String getKey() {
        return HmacUtils.hmacSecretKey;
    }

    public static String getHashedKey() {
        return DigestUtils.sha3_512Hex(HmacUtils.hmacSecretKey);
    }

    private static TokenData getTokenDataInternal(String textToSign, String timestamp) {
        Mac mac = org.apache.commons.codec.digest.HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_256, hmacSecretKey.getBytes(StandardCharsets.UTF_8));
        String content = DigestUtils.md5Hex(textToSign).toUpperCase() + '\n' + timestamp;
        mac.update(content.getBytes(StandardCharsets.UTF_8));
        return new TokenData(Base64.getEncoder().encodeToString(mac.doFinal()), timestamp);
    }

    public static TokenData getTokenData(String textToSign) {
        if (!isConfigured()) {
            throw new IllegalStateException("HMAC configuration not present");
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        return getTokenDataInternal(textToSign, timestamp);
    }

    public static boolean isTokenValid(String receivedTokenValue, String expectedText, String expectedTimestamp) {
        if (!isConfigured()) {
            throw new IllegalStateException("HMAC configuration not present");
        }
        TokenData expectedTokenData = getTokenDataInternal(expectedText, expectedTimestamp);
        if (expectedTokenData.getTokenValue().equals(receivedTokenValue)) {
            // Token matches.
            long now = System.currentTimeMillis();
            if (now - Long.parseLong(expectedTimestamp) <= hmacMaxValidityWindow) {
                return true;
            } else {
                LOG.warn("Expired HMAC token received and rejected.");
            }
        } else {
            LOG.warn("Invalid HMAC token received and rejected.");
        }
        return false;
    }

    public static class TokenData {

        private final String tokenValue;
        private final String tokenTimestamp;

        private TokenData(String tokenValue, String tokenTimestamp) {
            this.tokenValue = tokenValue;
            this.tokenTimestamp = tokenTimestamp;
        }

        public String getTokenValue() {
            return tokenValue;
        }

        public String getTokenTimestamp() {
            return tokenTimestamp;
        }
    }

}
