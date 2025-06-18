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

import org.apache.pdfbox.io.IOUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.cms.CMSTypedData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wraps a InputStream into a CMSProcessable object for bouncy castle. It's a memory saving
 * alternative to the {@link org.bouncycastle.cms.CMSProcessableByteArray CMSProcessableByteArray}
 * class.
 *
 * Adapted from the PDFBox examples: https://pdfbox.apache.org/2.0/examples.html
 */
class CMSProcessableInputStream implements CMSTypedData
{
    private InputStream in;
    private final ASN1ObjectIdentifier contentType;

    CMSProcessableInputStream(InputStream is)
    {
        this(new ASN1ObjectIdentifier(CMSObjectIdentifiers.data.getId()), is);
    }

    CMSProcessableInputStream(ASN1ObjectIdentifier type, InputStream is) {
        contentType = type;
        in = is;
    }

    @Override
    public Object getContent() {
        return in;
    }

    @Override
    public void write(OutputStream out) throws IOException {
        // read the content only one time
        IOUtils.copy(in, out);
        in.close();
    }

    @Override
    public ASN1ObjectIdentifier getContentType() {
        return contentType;
    }
}
