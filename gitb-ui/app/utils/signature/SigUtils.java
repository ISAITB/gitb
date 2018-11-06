package utils.signature;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.x509.KeyPurposeId;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.*;
import java.util.Enumeration;
import java.util.List;

/**
 * Utility class for the signature / timestamp examples.
 *
 * Adapted from the PDFBox examples: https://pdfbox.apache.org/2.0/examples.html
 */
public class SigUtils {

    static {
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
    }

    private SigUtils() {
    }

    /**
     * Get the access permissions granted for this document in the DocMDP transform parameters
     * dictionary. Details are described in the table "Entries in the DocMDP transform parameters
     * dictionary" in the PDF specification.
     *
     * @param doc document.
     * @return the permission value. 0 means no DocMDP transform parameters dictionary exists. Other
     * return values are 1, 2 or 3. 2 is also returned if the DocMDP transform parameters dictionary
     * is found but did not contain a /P entry, or if the value is outside the valid range.
     */
    public static int getMDPPermission(PDDocument doc) {
        COSBase base = doc.getDocumentCatalog().getCOSObject().getDictionaryObject(COSName.PERMS);
        if (base instanceof COSDictionary)
        {
            COSDictionary permsDict = (COSDictionary) base;
            base = permsDict.getDictionaryObject(COSName.DOCMDP);
            if (base instanceof COSDictionary)
            {
                COSDictionary signatureDict = (COSDictionary) base;
                base = signatureDict.getDictionaryObject("Reference");
                if (base instanceof COSArray)
                {
                    COSArray refArray = (COSArray) base;
                    for (int i = 0; i < refArray.size(); ++i)
                    {
                        base = refArray.getObject(i);
                        if (base instanceof COSDictionary)
                        {
                            COSDictionary sigRefDict = (COSDictionary) base;
                            if (COSName.DOCMDP.equals(sigRefDict.getDictionaryObject("TransformMethod")))
                            {
                                base = sigRefDict.getDictionaryObject("TransformParams");
                                if (base instanceof COSDictionary)
                                {
                                    COSDictionary transformDict = (COSDictionary) base;
                                    int accessPermissions = transformDict.getInt(COSName.P, 2);
                                    if (accessPermissions < 1 || accessPermissions > 3)
                                    {
                                        accessPermissions = 2;
                                    }
                                    return accessPermissions;
                                }
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Set the access permissions granted for this document in the DocMDP transform parameters
     * dictionary. Details are described in the table "Entries in the DocMDP transform parameters
     * dictionary" in the PDF specification.
     *
     * @param doc The document.
     * @param signature The signature object.
     * @param accessPermissions The permission value (1, 2 or 3).
     */
    static public void setMDPPermission(PDDocument doc, PDSignature signature, int accessPermissions)
    {
        COSDictionary sigDict = signature.getCOSObject();

        // DocMDP specific stuff
        COSDictionary transformParameters = new COSDictionary();
        transformParameters.setItem(COSName.TYPE, COSName.getPDFName("TransformParams"));
        transformParameters.setInt(COSName.P, accessPermissions);
        transformParameters.setName(COSName.V, "1.2");
        transformParameters.setNeedToBeUpdated(true);

        COSDictionary referenceDict = new COSDictionary();
        referenceDict.setItem(COSName.TYPE, COSName.getPDFName("SigRef"));
        referenceDict.setItem("TransformMethod", COSName.DOCMDP);
        referenceDict.setItem("DigestMethod", COSName.getPDFName("SHA1"));
        referenceDict.setItem("TransformParams", transformParameters);
        referenceDict.setNeedToBeUpdated(true);

        COSArray referenceArray = new COSArray();
        referenceArray.add(referenceDict);
        sigDict.setItem("Reference", referenceArray);
        referenceArray.setNeedToBeUpdated(true);

        // Catalog
        COSDictionary catalogDict = doc.getDocumentCatalog().getCOSObject();
        COSDictionary permsDict = new COSDictionary();
        catalogDict.setItem(COSName.PERMS, permsDict);
        permsDict.setItem(COSName.DOCMDP, signature);
        catalogDict.setNeedToBeUpdated(true);
        permsDict.setNeedToBeUpdated(true);
    }

    /**
     * Log if the certificate is not valid for signature usage. Doing this
     * anyway results in Adobe Reader failing to validate the PDF.
     *
     * @param x509Certificate
     * @throws java.security.cert.CertificateParsingException
     */
    public static boolean checkCertificateUsage(X509Certificate x509Certificate) throws CertificateParsingException {
        // Check whether signer certificate is "valid for usage"
        // https://stackoverflow.com/a/52765021/535646
        // https://www.adobe.com/devnet-docs/acrobatetk/tools/DigSig/changes.html#id1
        boolean[] keyUsage = x509Certificate.getKeyUsage();
        if (keyUsage != null && !keyUsage[0] && !keyUsage[1]) {
            // (unclear what "signTransaction" is)
            // https://tools.ietf.org/html/rfc5280#section-4.2.1.3
            return false;
        }
        return true;
    }

    public static boolean checkCertificateExtendedUsage(X509Certificate x509Certificate) throws CertificateParsingException {
        List<String> extendedKeyUsage = x509Certificate.getExtendedKeyUsage();
        if (extendedKeyUsage != null &&
                !extendedKeyUsage.contains(KeyPurposeId.id_kp_emailProtection.toString()) &&
                !extendedKeyUsage.contains(KeyPurposeId.id_kp_codeSigning.toString()) &&
                !extendedKeyUsage.contains(KeyPurposeId.anyExtendedKeyUsage.toString()) &&
                !extendedKeyUsage.contains("1.2.840.113583.1.1.5") &&
                // not mentioned in Adobe document, but tolerated in practice
                !extendedKeyUsage.contains("1.3.6.1.4.1.311.10.3.12")) {
            return false;
        }
        return true;
    }

    public static void checkCertificateValidity(X509Certificate x509Certificate) throws CertificateExpiredException, CertificateNotYetValidException {
        x509Certificate.checkValidity();
    }

    public static Certificate checkKeystore(KeyStore keystore, char[] pin) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        Enumeration<String> aliases = keystore.aliases();
        String alias;
        while (aliases.hasMoreElements()) {
            alias = aliases.nextElement();
            keystore.getKey(alias, pin);
            Certificate[] certChain = keystore.getCertificateChain(alias);
            if (certChain == null) {
                continue;
            }
            return certChain[0];
        }
        return null;
    }

    public static KeyStore loadKeystore(byte[] keystoreBytes, String keystoreType, char[] keystorePassword) {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(keystoreBytes)) {
            KeyStore keystore = KeyStore.getInstance(keystoreType);
            keystore.load(bin, keystorePassword);
            return keystore;
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
            throw new IllegalStateException("Unable to load keystore", e);
        }
    }

}