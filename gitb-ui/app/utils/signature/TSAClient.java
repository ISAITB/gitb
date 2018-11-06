package utils.signature;

import config.Configurations;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.tsp.*;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Time Stamping Authority (TSA) Client [RFC 3161].
 *
 * Adapted from the PDFBox examples: https://pdfbox.apache.org/2.0/examples.html
 */
public class TSAClient {

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
        SecureRandom random = new SecureRandom();
        int nonce = random.nextInt();

        // generate TSA request
        TimeStampRequestGenerator tsaGenerator = new TimeStampRequestGenerator();
        tsaGenerator.setCertReq(true);
        ASN1ObjectIdentifier oid = getHashObjectIdentifier(digest.getAlgorithm());
        TimeStampRequest request = tsaGenerator.generate(oid, hash, BigInteger.valueOf(nonce));

        // get TSA response
        byte[] tsaResponse = getTSAResponse(request.getEncoded());

        TimeStampResponse response;
        try
        {
            response = new TimeStampResponse(tsaResponse);
            response.validate(request);
        }
        catch (TSPException e)
        {
            throw new IOException(e);
        }

        TimeStampToken token = response.getTimeStampToken();
        if (token == null)
        {
            throw new IOException("Response does not have a time stamp token");
        }

        return token.getEncoded();
    }

    // gets response data for the given encoded TimeStampRequest data
    // throws IOException if a connection to the TSA cannot be established
    private byte[] getTSAResponse(byte[] request) throws IOException {
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(url.toString());

        if (Configurations.PROXY_SERVER_ENABLED()) {
            HostConfiguration config = client.getHostConfiguration();
            config.setProxy(Configurations.PROXY_SERVER_HOST(), Configurations.PROXY_SERVER_PORT());
            if (Configurations.PROXY_SERVER_AUTH_ENABLED()) {
                Credentials credentials = new UsernamePasswordCredentials(Configurations.PROXY_SERVER_AUTH_USERNAME(), Configurations.PROXY_SERVER_AUTH_PASSWORD());
                AuthScope authScope = new AuthScope(Configurations.PROXY_SERVER_HOST(), Configurations.PROXY_SERVER_PORT());
                client.getState().setProxyCredentials(authScope, credentials);
            }
        }

        post.setRequestEntity(new ByteArrayRequestEntity(request));
        post.setRequestHeader("Content-type", "application/timestamp-query");
        byte[] response = null;
        try {
            client.executeMethod(post);
            if (post.getStatusCode() == HttpStatus.SC_OK) {
                response = org.apache.commons.io.IOUtils.toByteArray(post.getResponseBodyAsStream());
            }
        } finally {
            post.releaseConnection();
        }
        return response;
    }

    // returns the ASN.1 OID of the given hash algorithm
    private ASN1ObjectIdentifier getHashObjectIdentifier(String algorithm) {
        switch (algorithm) {
            case "MD2":
                return new ASN1ObjectIdentifier(PKCSObjectIdentifiers.md2.getId());
            case "MD5":
                return new ASN1ObjectIdentifier(PKCSObjectIdentifiers.md5.getId());
            case "SHA-1":
                return new ASN1ObjectIdentifier(OIWObjectIdentifiers.idSHA1.getId());
            case "SHA-224":
                return new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha224.getId());
            case "SHA-256":
                return new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha256.getId());
            case "SHA-384":
                return new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha384.getId());
            case "SHA-512":
                return new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha512.getId());
            default:
                return new ASN1ObjectIdentifier(algorithm);
        }
    }
}