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

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.ess.ESSCertIDv2;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.IssuerSerial;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;

/**
 * Base class used for PDF signature creation.
 *
 * Adapted from the PDFBox examples: https://pdfbox.apache.org/2.0/examples.html
 */
public abstract class CreateSignatureBase implements SignatureInterface {

    private static final Logger logger = LoggerFactory.getLogger(CreateSignatureBase.class);

    private PrivateKey privateKey;
    private Certificate[] certificateChain;
    private TsaSettings tsa;
    private volatile boolean timestamped = false;

    /**
     * Initialize the signature creator with a keystore (pkcs12) and pin that should be used for the
     * signature.
     *
     * @param keystore is a pkcs12 keystore.
     * @param pin      is the pin for the keystore / private key
     * @throws KeyStoreException         if the keystore has not been initialized (loaded)
     * @throws NoSuchAlgorithmException  if the algorithm for recovering the key cannot be found
     * @throws UnrecoverableKeyException if the given password is wrong
     * @throws CertificateException      if the certificate is not valid as signing time
     * @throws IOException               if no certificate could be found
     */
    public CreateSignatureBase(KeyStore keystore, char[] pin) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, IOException, CertificateException {
        // grabs the first alias from the keystore and get the private key.
        Enumeration<String> aliases = keystore.aliases();
        String alias;
        Certificate cert = null;
        while (aliases.hasMoreElements()) {
            alias = aliases.nextElement();
            setPrivateKey((PrivateKey) keystore.getKey(alias, pin));
            Certificate[] certChain = keystore.getCertificateChain(alias);
            if (certChain == null) {
                continue;
            }
            setCertificateChain(certChain);
            cert = certChain[0];
            if (cert instanceof X509Certificate) {
                SigUtils.checkCertificateValidity((X509Certificate) cert);
                SigUtils.checkCertificateUsage((X509Certificate) cert);
                SigUtils.checkCertificateExtendedUsage((X509Certificate) cert);
            }
            break;
        }
        if (cert == null) {
            throw new IOException("Could not find certificate");
        }
    }

    private final void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    private final void setCertificateChain(final Certificate[] certificateChain) {
        this.certificateChain = certificateChain;
    }

    /**
     * @param tsa The settings of the timestamp authority to use (null for no timestamps).
     */
    public void setTsa(TsaSettings tsa) {
        this.tsa = tsa;
    }

    /**
     * SignatureInterface implementation.
     * <p>
     * This method will be called from inside the pdfbox and create the PKCS #7 signature.
     * The given InputStream contains the bytes that are given by the byte range.
     * <p>
     * This method is for internal use only.
     * <p>
     * Use your favorite cryptographic library to implement PKCS #7 signature creation.
     *
     * @throws IOException If an IO error occurs.
     */
    @Override
    public byte[] sign(InputStream content) throws IOException {
        // cannot be done private (interface)
        try {
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            X509Certificate cert = (X509Certificate) certificateChain[0];
            ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm(privateKey)).build(privateKey);
            // PAdES (ETSI EN 319 142-1 / CAdES baseline): signing-certificate-v2 is required and the signing-time
            // signed attribute must not be present (the time is given by the signature dictionary and the timestamp).
            JcaSignerInfoGeneratorBuilder signerInfoBuilder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build());
            signerInfoBuilder.setSignedAttributeGenerator(new PadesSignedAttributeGenerator(cert));
            gen.addSignerInfoGenerator(signerInfoBuilder.build(contentSigner, cert));
            gen.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));
            CMSProcessableInputStream msg = new CMSProcessableInputStream(content);
            CMSSignedData signedData = gen.generate(msg, false);
            timestamped = false;
            if (tsa != null) {
                ValidationTimeStamp validation = new ValidationTimeStamp(tsa);
                try {
                    signedData = validation.addSignedTimeStamp(signedData);
                    timestamped = true;
                } catch (TimestampingException e) {
                    logger.warn("Could not add signed timestamp. The signature will not be timestamped.", e);
                }
            }
            return signedData.getEncoded();
        } catch (GeneralSecurityException | CMSException | OperatorCreationException e) {
            throw new IOException(e);
        }
    }

    /**
     * @return Whether the last created signature includes a signature timestamp (PAdES B-T).
     */
    public boolean isTimestamped() {
        return timestamped;
    }

    private static String signatureAlgorithm(PrivateKey key) throws NoSuchAlgorithmException {
        String algorithm = key.getAlgorithm();
        if ("RSA".equalsIgnoreCase(algorithm)) {
            return "SHA256WithRSA";
        } else if ("EC".equalsIgnoreCase(algorithm) || "ECDSA".equalsIgnoreCase(algorithm)) {
            return "SHA256WithECDSA";
        }
        throw new NoSuchAlgorithmException("Unsupported signing key algorithm [" + algorithm + "]");
    }

    /**
     * Signed attributes for PAdES: the default ones (content type, message digest) without the signing time, plus the
     * ESS signing-certificate-v2 attribute that binds the signature to the signing certificate.
     */
    private static class PadesSignedAttributeGenerator extends DefaultSignedAttributeTableGenerator {

        private final Attribute signingCertificate;

        PadesSignedAttributeGenerator(X509Certificate certificate) throws GeneralSecurityException {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
            X509CertificateHolder holder;
            try {
                holder = new JcaX509CertificateHolder(certificate);
            } catch (CertificateEncodingException e) {
                throw new GeneralSecurityException(e);
            }
            IssuerSerial issuerSerial = new IssuerSerial(new GeneralNames(new GeneralName(holder.getIssuer())), holder.getSerialNumber());
            // The default hash algorithm of ESSCertIDv2 is SHA-256.
            signingCertificate = new Attribute(PKCSObjectIdentifiers.id_aa_signingCertificateV2, new DERSet(new SigningCertificateV2(new ESSCertIDv2(hash, issuerSerial))));
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public AttributeTable getAttributes(Map parameters) {
            AttributeTable table = super.getAttributes(parameters);
            table = table.remove(CMSAttributes.signingTime);
            return table.add(signingCertificate.getAttrType(), signingCertificate.getAttrValues().getObjectAt(0));
        }
    }

}