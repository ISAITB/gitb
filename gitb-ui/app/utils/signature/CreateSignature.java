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

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final Logger logger = LoggerFactory.getLogger(CreateSignature.class);

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
    public void signDetached(Path in, OutputStream out, String tsaUrl) throws IOException {
        signDetached(in, out, tsaUrl, false, null);
    }

    /**
     * Signs the given PDF file as PAdES: B-B, B-T if a timestamp authority is provided and B-LT if in addition
     * the validation information is to be added. Failing to timestamp or to collect the validation information is
     * logged and results in the highest level that could be reached.
     * <p>
     * The documents are always processed as files and streams, never held in memory.
     *
     * @param in input PDF file
     * @param out output PDF file
     * @param tsaUrl the URL of the timestamp authority (optional)
     * @param addValidationInformation whether to add the validation information (requires a timestamp).
     * @param tempFile a temporary file (that the caller is responsible to delete) to hold the signed document while
     *                 the validation information is added. Required if the validation information is to be added.
     * @throws IOException if the input file could not be read
     */
    public void signDetached(Path in, OutputStream out, String tsaUrl, boolean addValidationInformation, Path tempFile) throws IOException {
        if (!addValidationInformation) {
            try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBufferedFile(in))) {
                signDetached(doc, out, tsaUrl);
            }
            return;
        }
        if (tempFile == null) {
            throw new IllegalArgumentException("A temporary file is needed to add validation information");
        }
        // The validation information is added to the signed document, which is first written to the temporary file.
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBufferedFile(in));
             OutputStream signed = Files.newOutputStream(tempFile)) {
            signDetached(doc, signed, tsaUrl);
        }
        if (!isTimestamped()) {
            logger.warn("Validation information requires a timestamped signature. The signature will be of PAdES level B-B.");
            Files.copy(tempFile, out);
            return;
        }
        try {
            ValidationInformation.addValidationInformation(tempFile, out);
        } catch (ValidationInformation.UnavailableException e) {
            // Nothing was written to the output: use the signed document as is.
            logger.warn("Could not add validation information to the signature. The signature will be of PAdES level B-T.", e);
            Files.copy(tempFile, out);
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
        // PAdES: CAdES-based signature (ETSI EN 319 142-1).
        signature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);
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