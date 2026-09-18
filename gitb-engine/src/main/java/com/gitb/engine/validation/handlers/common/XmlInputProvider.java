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

package com.gitb.engine.validation.handlers.common;

import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.types.ObjectType;
import com.gitb.utils.XMLUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Lazily computes, and caches, the different representations of an {@link ObjectType}'s XML content needed across
 * XSD and Schematron validation for a single {@code verify} step: the serialised bytes (needed for the report
 * attachment and as the basis for every other representation), a streaming source (for XSLT-based Schematron and
 * XSD validation, which never need a DOM), and the line-numbered DOM (needed only to localise report findings, and
 * only for the pure Schematron path).
 * <p/>
 * A single instance is meant to be created per {@code verify} step - by {@code XmlValidator} when it is driving
 * both an XSD and one or more Schematron validations for the same input, or by {@code XsdValidator}/
 * {@code SchematronValidator} themselves otherwise - and passed down to the individual validators and report
 * handlers, instead of each of them calling {@link ObjectType#serializeByDefaultEncoding()} (a full DOM-to-text
 * transform) or {@link XMLUtils#readXMLWithLineNumbers} independently.
 * <p/>
 * Validation handler instances are singletons shared across concurrent sessions (see {@code ModuleManager}), so an
 * instance of this class must only ever be held locally (a method parameter or local variable), never as a field.
 */
public class XmlInputProvider {

    private final ObjectType xml;
    private byte[] serialisedContent;
    private String contentAsString;
    private Document lineNumberedDocument;

    /**
     * Constructor.
     *
     * @param xml The XML content.
     */
    public XmlInputProvider(ObjectType xml) {
        this.xml = xml;
    }

    /**
     * Get or compute (once) the serialised (UTF-8) bytes of the XML content.
     *
     * @return The bytes.
     */
    public byte[] getSerialisedContent() {
        if (serialisedContent == null) {
            serialisedContent = xml.serializeByDefaultEncoding();
        }
        return serialisedContent;
    }

    /**
     * Get or compute (once) the serialised XML content as a string, decoded explicitly as UTF-8 (the encoding used
     * by {@link ObjectType#serializeByDefaultEncoding()}), rather than relying on the JVM's default charset.
     *
     * @return The content.
     */
    public String getContentAsString() {
        if (contentAsString == null) {
            contentAsString = new String(getSerialisedContent(), StandardCharsets.UTF_8);
        }
        return contentAsString;
    }

    /**
     * Get or resolve (once) the line-numbered DOM for the XML content, used to localise report findings.
     *
     * @return The document.
     */
    public Document getLineNumberedDocument() {
        if (lineNumberedDocument == null) {
            try {
                lineNumberedDocument = XMLUtils.readXMLWithLineNumbers(new ByteArrayInputStream(getSerialisedContent()));
            } catch (IOException | SAXException e) {
                throw new GITBEngineInternalError("Unable to read input as XML document.", e);
            }
        }
        return lineNumberedDocument;
    }

    /**
     * Build a fresh, hardened, namespace-aware streaming source over the XML content. A new source is returned on
     * each call since the underlying stream is consumed once it is transformed.
     *
     * @return The source.
     */
    public SAXSource newSource() {
        return XMLUtils.getSecureSaxSource(new ByteArrayInputStream(getSerialisedContent()));
    }

}
