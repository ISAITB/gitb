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

import org.apache.pdfbox.io.IOUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.Attributes;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class wraps the TSAClient and the work that has to be done with it. Like Adding Signed
 * TimeStamps to a signature, or creating a CMS timestamp attribute (with a signed timestamp)
 *
 * Adapted from the PDFBox examples: https://pdfbox.apache.org/2.0/examples.html
 */
public class ValidationTimeStamp {

    private static final Logger logger = LoggerFactory.getLogger(ValidationTimeStamp.class);

    private TSAClient tsaClient;

    /**
     * @param settings The settings of the TSA where TS-Requests will be done.
     * @throws NoSuchAlgorithmException
     * @throws MalformedURLException
     */
    public ValidationTimeStamp(TsaSettings settings) throws NoSuchAlgorithmException, MalformedURLException {
        if (settings != null)
        {
            // Fail early for an invalid address.
            URI.create(settings.url()).toURL();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            this.tsaClient = new TSAClient(settings, digest);
        }
    }

    /**
     * Check whether a timestamp token includes the certificate of the TSA that issued it. Without it a validator cannot
     * validate the timestamp unless it can retrieve the certificate otherwise, and long-term validation data cannot be
     * added for it.
     *
     * @param token The encoded timestamp token.
     * @return The check result.
     * @throws IOException If the token cannot be read.
     */
    public static boolean containsSignerCertificate(byte[] token) throws IOException {
        try {
            TimeStampToken timeStampToken = new TimeStampToken(new CMSSignedData(token));
            return !timeStampToken.getCertificates().getMatches(timeStampToken.getSID()).isEmpty();
        } catch (CMSException | TSPException e) {
            throw new IOException("Unable to read the timestamp token", e);
        }
    }

    /**
     * Creates a signed timestamp token by the given input stream.
     *
     * @param content InputStream of the content to sign
     * @return the byte[] of the timestamp token
     * @throws IOException
     */
    public byte[] getTimeStampToken(InputStream content) throws IOException {
        return tsaClient.getTimeStampToken(IOUtils.toByteArray(content));
    }

    /**
     * Extend cms signed data with TimeStamp first or to all signers
     *
     * @param signedData Generated CMS signed data
     * @return CMSSignedData Extended CMS signed data
     * @throws IOException
     */
    public CMSSignedData addSignedTimeStamp(CMSSignedData signedData) throws TimestampingException {
        SignerInformationStore signerStore = signedData.getSignerInfos();
        List<SignerInformation> newSigners = new ArrayList<>();
        try {
            for (SignerInformation signer : (Collection<SignerInformation>) signerStore.getSigners()) {
                // This adds a timestamp to every signer (into his unsigned attributes) in the signature.
                newSigners.add(signTimeStamp(signer));
            }
        } catch (Exception e) {
            throw new TimestampingException("Error generating timestamp: " + e.getMessage(), e);
        }

        // Because new SignerInformation is created, new SignerInfoStore has to be created
        // and also be replaced in signedData. Which creates a new signedData object.
        return CMSSignedData.replaceSigners(signedData, new SignerInformationStore(newSigners));
    }

    /**
     * Extend CMS Signer Information with the TimeStampToken into the unsigned Attributes.
     *
     * @param signer information about signer
     * @return information about SignerInformation
     * @throws IOException
     */
    private SignerInformation signTimeStamp(SignerInformation signer) throws IOException {
        AttributeTable unsignedAttributes = signer.getUnsignedAttributes();

        ASN1EncodableVector vector = new ASN1EncodableVector();
        if (unsignedAttributes != null)
        {
            vector = unsignedAttributes.toASN1EncodableVector();
        }

        byte[] token = tsaClient.getTimeStampToken(signer.getSignature());
        if (!containsSignerCertificate(token)) {
            logger.warn("The timestamp token provided by the TSA does not include the TSA's certificate. Validators that cannot retrieve it otherwise may not be able to verify the timestamp, and no long-term validation data can be added for it.");
        }
        ASN1ObjectIdentifier oid = PKCSObjectIdentifiers.id_aa_signatureTimeStampToken;
        ASN1Encodable signatureTimeStamp = new Attribute(oid,
                new DERSet(ASN1Primitive.fromByteArray(token)));

        vector.add(signatureTimeStamp);
        Attributes signedAttributes = new Attributes(vector);

        // There is no other way changing the unsigned attributes of the signer information.
        // result is never null, new SignerInformation always returned,
        // see source code of replaceUnsignedAttributes
        return SignerInformation.replaceUnsignedAttributes(signer, new AttributeTable(signedAttributes));
    }
}