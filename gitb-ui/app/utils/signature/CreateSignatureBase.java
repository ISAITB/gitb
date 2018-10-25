package utils.signature;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Base class used for PDF signature creation.
 *
 * Adapted from the PDFBox examples: https://pdfbox.apache.org/2.0/examples.html
 */
public abstract class CreateSignatureBase implements SignatureInterface {

    private static final Logger logger = LoggerFactory.getLogger(CreateSignatureBase.class);

    private PrivateKey privateKey;
    private Certificate[] certificateChain;
    private String tsaUrl;

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

    public void setTsaUrl(String tsaUrl) {
        this.tsaUrl = tsaUrl;
    }

    /**
     * SignatureInterface implementation.
     * <p>
     * This method will be called from inside of the pdfbox and create the PKCS #7 signature.
     * The given InputStream contains the bytes that are given by the byte range.
     * <p>
     * This method is for internal use only.
     * <p>
     * Use your favorite cryptographic library to implement PKCS #7 signature creation.
     *
     * @throws IOException
     */
    @Override
    public byte[] sign(InputStream content) throws IOException {
        // cannot be done private (interface)
        try {
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            X509Certificate cert = (X509Certificate) certificateChain[0];
            ContentSigner sha1Signer = new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey);
            gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build()).build(sha1Signer, cert));
            gen.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));
            CMSProcessableInputStream msg = new CMSProcessableInputStream(content);
            CMSSignedData signedData = gen.generate(msg, false);
            if (tsaUrl != null && tsaUrl.length() > 0) {
                ValidationTimeStamp validation = new ValidationTimeStamp(tsaUrl);
                try {
                    signedData = validation.addSignedTimeStamp(signedData);
                } catch (TimestampingException e) {
                    logger.warn("Could not add signed timestamp. Reverting to using system clock.", e);
                }
            }
            return signedData.getEncoded();
        } catch (GeneralSecurityException | CMSException | OperatorCreationException e) {
            throw new IOException(e);
        }
    }

}