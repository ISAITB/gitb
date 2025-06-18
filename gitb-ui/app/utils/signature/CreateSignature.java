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

package utils.signature;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Calendar;

/**
 * Class to create s aignature.
 *
 * Adapted from the PDFBox examples: https://pdfbox.apache.org/2.0/examples.html
 */
public class CreateSignature extends CreateSignatureBase {

    /**
     * Initialize the signature creator with a keystore and certficate password.
     *
     * @param keystore the pkcs12 keystore containing the signing certificate
     * @param pin the password for recovering the key
     * @throws KeyStoreException if the keystore has not been initialized (loaded)
     * @throws NoSuchAlgorithmException if the algorithm for recovering the key cannot be found
     * @throws UnrecoverableKeyException if the given password is wrong
     * @throws CertificateException if the certificate is not valid as signing time
     * @throws IOException if no certificate could be found
     */
    public CreateSignature(KeyStore keystore, char[] pin) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, IOException {
        super(keystore, pin);
    }

    /**
     * Signs the given PDF file.
     * @param in input PDF file
     * @param out output PDF file
     * @throws IOException if the input file could not be read
     */
    public void signDetached(InputStream in, OutputStream out, String tsaUrl) throws IOException {
        // sign
        try (PDDocument doc = PDDocument.load(in)) {
            signDetached(doc, out, tsaUrl);
        }
    }

    private void signDetached(PDDocument document, OutputStream output, String tsaUrl) throws IOException {
        setTsaUrl(tsaUrl);
        int accessPermissions = SigUtils.getMDPPermission(document);
        if (accessPermissions == 1) {
            throw new IllegalStateException("No changes to the document are permitted due to DocMDP transform parameters dictionary");
        }
        // create signature dictionary
        PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        signature.setName("Test Bed");
        signature.setLocation("European Union");
        signature.setReason("Conformance testing");

        // the signing date, needed for valid signature
        signature.setSignDate(Calendar.getInstance());

        // Optional: certify
        if (accessPermissions == 0) {
            SigUtils.setMDPPermission(document, signature, 2);
        }

        SignatureOptions signatureOptions = new SignatureOptions();
        // Size can vary, but should be enough for purpose.
        signatureOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);
        // register signature dictionary and sign interface
        document.addSignature(signature, this, signatureOptions);
        // write incremental (only for signing purpose)
        document.saveIncremental(output);
    }

}