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

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.asn1.cmp.PKIStatus;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.tsp.*;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * Time Stamping Authority (TSA) Client [RFC 3161].
 * <p/>
 * Adapted from the PDFBox examples: <a href="https://pdfbox.apache.org/2.0/examples.html">...</a>
 * <p/>
 * Failures are reported with messages that name the setting that is likely to need adapting. The credentials are never
 * included in messages.
 */
public class TSAClient {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_BODY_EXCERPT = 200;

    private final TsaSettings settings;
    private final MessageDigest digest;

    /**
     * @param settings the settings to use for the TSA
     * @param digest the message digest to use
     */
    public TSAClient(TsaSettings settings, MessageDigest digest) {
        this.settings = settings;
        this.digest = digest;
    }

    /**
     *
     * @param messageImprint imprint of message contents
     * @return the encoded time stamp token
     * @throws IOException if there was an error with the connection or data from the TSA server,
     *                     or if the time stamp response could not be validated
     */
    public byte[] getTimeStampToken(byte[] messageImprint) throws IOException {
        digest.reset();
        byte[] hash = digest.digest(messageImprint);
        // 32-bit cryptographic nonce
        int nonce = RANDOM.nextInt();
        // generate TSA request
        TimeStampRequestGenerator tsaGenerator = new TimeStampRequestGenerator();
        tsaGenerator.setCertReq(true);
        if (settings.policyOid() != null && !settings.policyOid().isEmpty()) {
            tsaGenerator.setReqPolicy(new ASN1ObjectIdentifier(settings.policyOid()));
        }
        ASN1ObjectIdentifier oid = getHashObjectIdentifier(digest.getAlgorithm());
        TimeStampRequest request = tsaGenerator.generate(oid, hash, BigInteger.valueOf(nonce));
        // get TSA response
        byte[] tsaResponse = getTSAResponse(request.getEncoded());
        TimeStampResponse response;
        try {
            response = new TimeStampResponse(tsaResponse);
        } catch (TSPException | IOException e) {
            throw new IOException("The response of the TSA at [" + address() + "] is not a valid timestamp response", e);
        }
        if (response.getStatus() != PKIStatus.GRANTED && response.getStatus() != PKIStatus.GRANTED_WITH_MODS) {
            throw new IOException("The TSA at [" + address() + "] rejected the timestamp request" + describeRejection(response));
        }
        try {
            response.validate(request);
        } catch (TSPException e) {
            throw new IOException("The response of the TSA at [" + address() + "] does not match the timestamp request: " + e.getMessage(), e);
        }
        TimeStampToken token = response.getTimeStampToken();
        if (token == null) {
            throw new IOException("The response of the TSA at [" + address() + "] does not have a time stamp token");
        }
        return token.getEncoded();
    }

    // gets response data for the given encoded TimeStampRequest data
    // throws IOException if a connection to the TSA cannot be established or the TSA does not respond correctly
    private byte[] getTSAResponse(byte[] request) throws IOException {
        // Credentials are never forwarded to another address, so redirects are only followed if there are none.
        HttpClient.Redirect redirectPolicy = settings.hasCredentials() ? HttpClient.Redirect.NEVER : HttpClient.Redirect.NORMAL;
        try (var client = HttpClient.newBuilder().connectTimeout(settings.timeout()).followRedirects(redirectPolicy).build()) {
            HttpRequest.Builder httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(settings.url()))
                    .timeout(settings.timeout())
                    .header("Content-type", "application/timestamp-query")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(request));
            if (settings.hasCredentials()) {
                String credentials = settings.username() + ":" + (settings.password() == null ? "" : settings.password());
                httpRequest.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
            }
            HttpResponse<byte[]> response = client.send(httpRequest.build(), HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            if (status == HTTP_OK) {
                if (response.body() == null || response.body().length == 0) {
                    throw new IOException("The TSA at [" + address() + "] returned an empty response");
                }
                return response.body();
            }
            throw new IOException("The TSA at [" + address() + "] returned HTTP status " + status + describeHttpFailure(status, response.body()));
        } catch (HttpTimeoutException e) {
            throw new IOException("The TSA at [" + address() + "] did not respond within " + settings.timeout().toSeconds() + " seconds (see TSA_SERVER_TIMEOUT)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while calling the TSA at [" + address() + "]", e);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Error while calling the TSA at [" + address() + "]: " + e.getMessage(), e);
        }
    }

    // The TSA address, without any user information.
    private String address() {
        try {
            URI uri = URI.create(settings.url());
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null).toString();
        } catch (Exception e) {
            return "invalid address";
        }
    }

    private String describeHttpFailure(int status, byte[] body) {
        StringBuilder description = new StringBuilder();
        if (status == HTTP_UNAUTHORIZED || status == HTTP_FORBIDDEN) {
            description.append(" (the TSA requires authentication or rejected the credentials, see TSA_SERVER_AUTH_ENABLED, TSA_SERVER_USERNAME and TSA_SERVER_PASSWORD)");
        }
        if (body != null && body.length > 0) {
            String text = new String(body, 0, Math.min(body.length, MAX_BODY_EXCERPT), StandardCharsets.UTF_8);
            // Only show a readable text (and never control characters).
            if (text.chars().allMatch(c -> c == '\n' || c == '\r' || c == '\t' || (c >= 32 && c != 0xFFFD))) {
                description.append(": ").append(text.strip().replaceAll("\\s+", " "));
            }
        }
        return description.toString();
    }

    private String describeRejection(TimeStampResponse response) {
        StringBuilder description = new StringBuilder();
        String statusString = response.getStatusString();
        if (statusString != null && !statusString.isBlank()) {
            description.append(" (").append(statusString.strip()).append(")");
        }
        PKIFailureInfo failure = response.getFailInfo();
        if (failure != null) {
            List<String> reasons = new ArrayList<>();
            int value = failure.intValue();
            if ((value & PKIFailureInfo.badAlg) != 0) reasons.add("unsupported hash algorithm");
            if ((value & PKIFailureInfo.badRequest) != 0) reasons.add("invalid or unauthorised request");
            if ((value & PKIFailureInfo.badDataFormat) != 0) reasons.add("invalid data format");
            if ((value & PKIFailureInfo.timeNotAvailable) != 0) reasons.add("time not available");
            if ((value & PKIFailureInfo.unacceptedPolicy) != 0) reasons.add("policy not accepted, see TSA_SERVER_POLICY_OID");
            if ((value & PKIFailureInfo.unacceptedExtension) != 0) reasons.add("extension not accepted");
            if ((value & PKIFailureInfo.addInfoNotAvailable) != 0) reasons.add("additional information not available");
            if ((value & PKIFailureInfo.systemFailure) != 0) reasons.add("TSA system failure");
            if (!reasons.isEmpty()) {
                description.append(": ").append(String.join(", ", reasons));
            }
        }
        return description.toString();
    }

    // returns the ASN.1 OID of the given hash algorithm
    private ASN1ObjectIdentifier getHashObjectIdentifier(String algorithm) {
        return switch (algorithm) {
            case "MD2" -> new ASN1ObjectIdentifier(PKCSObjectIdentifiers.md2.getId());
            case "MD5" -> new ASN1ObjectIdentifier(PKCSObjectIdentifiers.md5.getId());
            case "SHA-1" -> new ASN1ObjectIdentifier(OIWObjectIdentifiers.idSHA1.getId());
            case "SHA-224" -> new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha224.getId());
            case "SHA-256" -> new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha256.getId());
            case "SHA-384" -> new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha384.getId());
            case "SHA-512" -> new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha512.getId());
            default -> new ASN1ObjectIdentifier(algorithm);
        };
    }
}
