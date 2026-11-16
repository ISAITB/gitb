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

import com.gitb.utils.UriAuthorizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.CRL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds the validation data of a signed PDF (the certificates of the signer and of the timestamp authority, with the
 * OCSP responses or CRLs proving their status) in a Document Security Store (DSS), turning a PAdES B-T signature into
 * PAdES B-LT (ETSI EN 319 142-1).
 * <p>
 * The addresses to retrieve data from (AIA and CRL distribution points) come from certificates that can be provided
 * by community administrators. Every request is therefore authorised against the global {@link UriAuthorizer} (http(s)
 * only, no internal addresses unless whitelisted by the operator), never follows redirects and is limited in time and
 * size.
 * <p>
 * The data is added all or nothing: if it cannot be completely collected an {@link IOException} is thrown and the
 * caller keeps the original document.
 */
public class ValidationInformation {

    private static final Logger logger = LoggerFactory.getLogger(ValidationInformation.class);

    private static final int MAX_RESPONSE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_CHAIN_LENGTH = 10;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final String OCSP_NO_CHECK_OID = "1.3.6.1.5.5.7.48.1.5";

    private final UriAuthorizer authorizer;
    private final HttpClient client;
    private final JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
    // Certificates to include in the DSS, by their encoding to avoid duplicates.
    private final Map<String, byte[]> certificates = new LinkedHashMap<>();
    private final List<byte[]> ocspResponses = new ArrayList<>();
    private final List<byte[]> crls = new ArrayList<>();

    private ValidationInformation(UriAuthorizer authorizer) {
        this.authorizer = authorizer;
        this.client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Signals that the validation information could not be collected. Nothing has been written to the output when this
     * is thrown, so the caller can still use the (unchanged) signed document.
     */
    public static class UnavailableException extends IOException {

        public UnavailableException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    /**
     * Add the validation data to the (last) signature of the provided PDF. The document is read from the file and the
     * result is streamed to the output, so that the document is never held in memory.
     *
     * @param signedPdf The signed PDF file, with a timestamped signature.
     * @param output The stream to write the PDF with the added DSS to (as an incremental update that leaves the
     *               signature valid).
     * @throws UnavailableException If the validation data could not be collected (nothing was written to the output).
     * @throws IOException If the document could not be read or written.
     */
    public static void addValidationInformation(Path signedPdf, OutputStream output) throws IOException {
        addValidationInformation(signedPdf, output, UriAuthorizer.global());
    }

    static void addValidationInformation(Path signedPdf, OutputStream output, UriAuthorizer authorizer) throws IOException {
        new ValidationInformation(authorizer).process(signedPdf, output);
    }

    private void process(Path signedPdf, OutputStream output) throws IOException {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(signedPdf))) {
            // Everything that can fail because of the signature or the remote services happens before writing anything.
            try {
                collect(document, signedPdf);
            } catch (Exception e) {
                throw new UnavailableException("Unable to add validation information: " + e.getMessage(), e);
            }
            document.saveIncremental(output);
        }
    }

    private void collect(PDDocument document, Path signedPdf) throws Exception {
        PDSignature signature = document.getLastSignatureDictionary();
        if (signature == null) {
            throw new IOException("The document has no signature");
        }
        // The contents are padded with zeroes after the DER structure: read the first object only.
        CMSSignedData signedData;
        try (InputStream pdf = Files.newInputStream(signedPdf);
             ASN1InputStream in = new ASN1InputStream(signature.getContents(pdf))) {
            signedData = new CMSSignedData(ContentInfo.getInstance(in.readObject()));
        }
        // Collect the certificates to complete (signer and timestamp authority) and those available to complete them.
        List<X509Certificate> pool = new ArrayList<>();
        List<X509Certificate> starts = new ArrayList<>();
        collectCertificates(signedData, pool, starts);
        List<CMSSignedData> tokens = timestampTokens(signedData);
        for (CMSSignedData token : tokens) {
            collectCertificates(token, pool, starts);
        }
        if (tokens.isEmpty()) {
            throw new IOException("The signature has no timestamp");
        }
        for (X509Certificate start : starts) {
            completeChain(start, pool);
        }
        if (!ocspResponses.isEmpty() || !crls.isEmpty() || !certificates.isEmpty()) {
            writeDss(document);
        }
    }

    @SuppressWarnings("unchecked")
    private void collectCertificates(CMSSignedData data, List<X509Certificate> pool, List<X509Certificate> starts) throws Exception {
        for (X509CertificateHolder holder : data.getCertificates().getMatches(null)) {
            pool.add(converter.getCertificate(holder));
        }
        for (SignerInformation signer : data.getSignerInfos().getSigners()) {
            Collection<X509CertificateHolder> matches = data.getCertificates().getMatches((org.bouncycastle.util.Selector<X509CertificateHolder>) (org.bouncycastle.util.Selector<?>) signer.getSID());
            for (X509CertificateHolder holder : matches) {
                starts.add(converter.getCertificate(holder));
            }
        }
    }

    private List<CMSSignedData> timestampTokens(CMSSignedData signedData) throws Exception {
        List<CMSSignedData> tokens = new ArrayList<>();
        for (SignerInformation signer : signedData.getSignerInfos().getSigners()) {
            if (signer.getUnsignedAttributes() != null) {
                Attribute attribute = signer.getUnsignedAttributes().get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
                if (attribute != null) {
                    ASN1Encodable value = attribute.getAttrValues().getObjectAt(0);
                    tokens.add(new CMSSignedData(ContentInfo.getInstance(value)));
                }
            }
        }
        return tokens;
    }

    /**
     * Walk up the chain of the certificate, adding every certificate and the revocation data of those that have an issuer.
     */
    private void completeChain(X509Certificate start, List<X509Certificate> pool) throws Exception {
        X509Certificate current = start;
        for (int i = 0; i < MAX_CHAIN_LENGTH; i++) {
            addCertificate(current);
            if (isSelfSigned(current)) {
                return;
            }
            X509Certificate issuer = findIssuer(current, pool);
            if (issuer == null) {
                throw new IOException("Could not determine the issuer of [" + current.getSubjectX500Principal() + "]");
            }
            addCertificate(issuer);
            if (current == start) {
                // Revocation data is required for the signer and timestamp authority certificates.
                addRevocationData(current, issuer);
            } else {
                // For CA certificates it is added if available (e.g. some do not provide any OCSP or CRL address).
                try {
                    addRevocationData(current, issuer);
                } catch (IOException e) {
                    logger.debug("No revocation data added for CA certificate: {}", e.getMessage());
                }
            }
            current = issuer;
        }
        throw new IOException("Certificate chain of [" + start.getSubjectX500Principal() + "] is too long");
    }

    private boolean isSelfSigned(X509Certificate certificate) {
        if (certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal())) {
            try {
                certificate.verify(certificate.getPublicKey());
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private X509Certificate findIssuer(X509Certificate certificate, List<X509Certificate> pool) throws Exception {
        for (X509Certificate candidate : pool) {
            if (candidate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal()) && verifiedBy(certificate, candidate)) {
                return candidate;
            }
        }
        // Retrieve from the authority information access extension.
        for (URI uri : accessLocations(certificate, AccessDescription.id_ad_caIssuers)) {
            try {
                X509Certificate candidate = (X509Certificate) CertificateFactory.getInstance("X.509")
                        .generateCertificate(new ByteArrayInputStream(http(uri, null, null, null)));
                if (candidate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal()) && verifiedBy(certificate, candidate)) {
                    pool.add(candidate);
                    return candidate;
                }
            } catch (Exception e) {
                logger.debug("Could not retrieve issuer certificate from [{}]: {}", uri, e.getMessage());
            }
        }
        return null;
    }

    private boolean verifiedBy(X509Certificate certificate, X509Certificate issuer) {
        try {
            certificate.verify(issuer.getPublicKey());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void addCertificate(X509Certificate certificate) throws Exception {
        certificates.putIfAbsent(java.util.HexFormat.of().formatHex(certificate.getEncoded()), certificate.getEncoded());
    }

    private void addRevocationData(X509Certificate certificate, X509Certificate issuer) throws Exception {
        if (certificate.getExtensionValue(OCSP_NO_CHECK_OID) != null) {
            return;
        }
        Exception ocspFailure = null;
        for (URI uri : accessLocations(certificate, AccessDescription.id_ad_ocsp)) {
            try {
                ocspResponses.add(fetchOcsp(uri, certificate, issuer));
                return;
            } catch (Exception e) {
                ocspFailure = e;
                logger.debug("No usable OCSP response from [{}]: {}", uri, e.getMessage());
            }
        }
        Exception crlFailure = null;
        for (URI uri : crlLocations(certificate)) {
            try {
                crls.add(fetchCrl(uri, certificate, issuer));
                return;
            } catch (Exception e) {
                crlFailure = e;
                logger.debug("No usable CRL from [{}]: {}", uri, e.getMessage());
            }
        }
        String reason = ocspFailure != null ? ocspFailure.getMessage() : (crlFailure != null ? crlFailure.getMessage() : "no OCSP or CRL address in the certificate");
        throw new IOException("No revocation data for [" + certificate.getSubjectX500Principal() + "]: " + reason);
    }

    private byte[] fetchOcsp(URI uri, X509Certificate certificate, X509Certificate issuer) throws Exception {
        DigestCalculator digestCalculator = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
        CertificateID id = new CertificateID(digestCalculator, new JcaX509CertificateHolder(issuer), certificate.getSerialNumber());
        OCSPReq request = new OCSPReqBuilder().addRequest(id).build();
        byte[] responseBytes = http(uri, request.getEncoded(), "application/ocsp-request", "application/ocsp-response");
        OCSPResp response = new OCSPResp(responseBytes);
        if (response.getStatus() != OCSPResp.SUCCESSFUL) {
            throw new IOException("OCSP response status " + response.getStatus());
        }
        BasicOCSPResp basic = (BasicOCSPResp) response.getResponseObject();
        // The response must be signed by the issuer or by a certificate provided in the response.
        List<X509CertificateHolder> signers = new ArrayList<>(List.of(basic.getCerts()));
        signers.add(new JcaX509CertificateHolder(issuer));
        X509CertificateHolder validSigner = null;
        for (X509CertificateHolder signer : signers) {
            try {
                if (basic.isSignatureValid(new JcaContentVerifierProviderBuilder().build(signer))) {
                    validSigner = signer;
                    break;
                }
            } catch (Exception e) {
                // Try the next one.
            }
        }
        if (validSigner == null) {
            throw new IOException("The signature of the OCSP response could not be verified");
        }
        for (SingleResp single : basic.getResponses()) {
            if (single.getCertID().equals(id)) {
                if (single.getCertStatus() != null) {
                    throw new IOException("The certificate [" + certificate.getSubjectX500Principal() + "] is not reported as good by the OCSP responder");
                }
                // Keep the responder's certificate so that the response can be validated offline.
                for (X509CertificateHolder holder : basic.getCerts()) {
                    addCertificate(converter.getCertificate(holder));
                }
                return responseBytes;
            }
        }
        throw new IOException("The OCSP response does not refer to the certificate");
    }

    private byte[] fetchCrl(URI uri, X509Certificate certificate, X509Certificate issuer) throws Exception {
        byte[] bytes = http(uri, null, null, "application/pkix-crl");
        CRL crl = CertificateFactory.getInstance("X.509").generateCRL(new ByteArrayInputStream(bytes));
        if (!(crl instanceof X509CRL x509Crl)) {
            throw new IOException("Not an X.509 CRL");
        }
        x509Crl.verify(issuer.getPublicKey());
        Date now = new Date();
        if (x509Crl.getNextUpdate() != null && now.after(x509Crl.getNextUpdate())) {
            throw new IOException("The CRL is expired");
        }
        if (x509Crl.isRevoked(certificate)) {
            throw new IOException("The certificate is revoked");
        }
        return bytes;
    }

    private List<URI> accessLocations(X509Certificate certificate, org.bouncycastle.asn1.ASN1ObjectIdentifier method) throws Exception {
        List<URI> uris = new ArrayList<>();
        Extensions extensions = new JcaX509CertificateHolder(certificate).getExtensions();
        AuthorityInformationAccess access = extensions == null ? null : AuthorityInformationAccess.fromExtensions(extensions);
        if (access != null) {
            for (AccessDescription description : access.getAccessDescriptions()) {
                if (method.equals(description.getAccessMethod())) {
                    addHttpUri(uris, description.getAccessLocation());
                }
            }
        }
        return uris;
    }

    private List<URI> crlLocations(X509Certificate certificate) throws Exception {
        List<URI> uris = new ArrayList<>();
        Extensions extensions = new JcaX509CertificateHolder(certificate).getExtensions();
        CRLDistPoint points = extensions == null ? null : CRLDistPoint.fromExtensions(extensions);
        if (points != null) {
            for (DistributionPoint point : points.getDistributionPoints()) {
                DistributionPointName name = point.getDistributionPoint();
                if (name != null && name.getType() == DistributionPointName.FULL_NAME) {
                    for (GeneralName generalName : GeneralNames.getInstance(name.getName()).getNames()) {
                        addHttpUri(uris, generalName);
                    }
                }
            }
        }
        return uris;
    }

    private void addHttpUri(List<URI> uris, GeneralName name) {
        if (name.getTagNo() == GeneralName.uniformResourceIdentifier) {
            try {
                URI uri = URI.create(org.bouncycastle.asn1.ASN1IA5String.getInstance(name.getName()).getString());
                // Only http(s) is ever used (e.g. LDAP addresses are skipped).
                if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                    uris.add(uri);
                }
            } catch (IllegalArgumentException e) {
                // Ignore invalid addresses.
            }
        }
    }

    /**
     * Make an authorised request (POST if a body is provided) without following redirects.
     */
    private byte[] http(URI uri, byte[] body, String contentType, String accept) throws IOException {
        authorizer.check(uri);
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri).timeout(REQUEST_TIMEOUT);
        if (accept != null) {
            builder.header("Accept", accept);
        }
        if (body == null) {
            builder.GET();
        } else {
            builder.header("Content-Type", contentType).POST(HttpRequest.BodyPublishers.ofByteArray(body));
        }
        try {
            HttpResponse<InputStream> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream stream = response.body()) {
                if (response.statusCode() != 200) {
                    throw new IOException("The server at [" + uri + "] returned status " + response.statusCode());
                }
                byte[] data = stream.readNBytes(MAX_RESPONSE_SIZE + 1);
                if (data.length > MAX_RESPONSE_SIZE) {
                    throw new IOException("The response from [" + uri + "] is too large");
                }
                return data;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while contacting [" + uri + "]", e);
        }
    }

    private void writeDss(PDDocument document) throws IOException {
        COSDictionary catalog = document.getDocumentCatalog().getCOSObject();
        COSDictionary dss = new COSDictionary();
        dss.setNeedToBeUpdated(true);
        dss.setItem(COSName.getPDFName("Certs"), toStreams(document, certificates.values()));
        if (!ocspResponses.isEmpty()) {
            dss.setItem(COSName.getPDFName("OCSPs"), toStreams(document, ocspResponses));
        }
        if (!crls.isEmpty()) {
            dss.setItem(COSName.getPDFName("CRLs"), toStreams(document, crls));
        }
        catalog.setItem(COSName.getPDFName("DSS"), dss);
        // Developer extension identifying the ETSI extensions (for documents below PDF 2.0).
        COSDictionary extensions = catalog.getCOSDictionary(COSName.getPDFName("Extensions"));
        if (extensions == null) {
            extensions = new COSDictionary();
            catalog.setItem(COSName.getPDFName("Extensions"), extensions);
        }
        if (extensions.getDictionaryObject(COSName.getPDFName("ESIC")) == null) {
            COSDictionary esic = new COSDictionary();
            esic.setItem(COSName.TYPE, COSName.getPDFName("DeveloperExtensions"));
            esic.setName(COSName.getPDFName("BaseVersion"), "1.7");
            esic.setInt(COSName.getPDFName("ExtensionLevel"), 1);
            esic.setNeedToBeUpdated(true);
            extensions.setItem(COSName.getPDFName("ESIC"), esic);
        }
        extensions.setNeedToBeUpdated(true);
        catalog.setNeedToBeUpdated(true);
    }

    private COSArray toStreams(PDDocument document, Collection<byte[]> items) throws IOException {
        COSArray array = new COSArray();
        for (byte[] item : items) {
            COSStream stream = document.getDocument().createCOSStream();
            try (OutputStream out = stream.createOutputStream(COSName.FLATE_DECODE)) {
                out.write(item);
            }
            stream.setNeedToBeUpdated(true);
            array.add(stream);
        }
        array.setNeedToBeUpdated(true);
        return array;
    }

}
