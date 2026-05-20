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
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.tsp.*;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.SecureRandom;

import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Time Stamping Authority (TSA) Client [RFC 3161].
 * <p/>
 * Adapted from the PDFBox examples: <a href="https://pdfbox.apache.org/2.0/examples.html">...</a>
 */
public class TSAClient {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final URL url;
    private final String username;
    private final String password;
    private final MessageDigest digest;

    /**
     *
     * @param url the URL of the TSA service
     * @param username user name of TSA
     * @param password password of TSA
     * @param digest the message digest to use
     */
    public TSAClient(URL url, String username, String password, MessageDigest digest) {
        this.url = url;
        this.username = username;
        this.password = password;
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
        ASN1ObjectIdentifier oid = getHashObjectIdentifier(digest.getAlgorithm());
        TimeStampRequest request = tsaGenerator.generate(oid, hash, BigInteger.valueOf(nonce));
        // get TSA response
        byte[] tsaResponse = getTSAResponse(request.getEncoded());
        TimeStampResponse response;
        try {
            response = new TimeStampResponse(tsaResponse);
            response.validate(request);
        } catch (TSPException e) {
            throw new IOException(e);
        }
        TimeStampToken token = response.getTimeStampToken();
        if (token == null) {
            throw new IOException("Response does not have a time stamp token");
        }
        return token.getEncoded();
    }

    // gets response data for the given encoded TimeStampRequest data
    // throws IOException if a connection to the TSA cannot be established
    private byte[] getTSAResponse(byte[] request) throws IOException {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(url.toURI())
                    .header("Content-type", "application/timestamp-query")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(request))
                    .build();
            try (var client = HttpClient.newHttpClient()) {
                HttpResponse<byte[]> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == HTTP_OK) {
                    return response.body();
                }
            }
            return null;
        } catch (Exception e) {
            throw new IOException("Error while calling TSA server", e);
        }
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