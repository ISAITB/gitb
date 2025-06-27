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

package com.gitb.utils;

import jakarta.xml.bind.*;
import net.sf.saxon.jaxp.SaxonTransformerFactory;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.namespace.QName;
import javax.xml.parsers.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Created by senan on 9/22/14.
 */
public class XMLUtils {

    public static final String LINE_NUMBER_KEY_NAME = "lineNumber";

    /**
     * Converts an XML content into a corresponding Object model.
     * A common interface (StreamSource) is used to support different XML sources, i.e. String, File, etc.
     * For a String s, new StreamSource(new StringReader(s)) can be used.
     * For a File f, new StreamSource(f) can be used.
     * @param clazz Class type of the Object
     * @param source An XML source
     * @param <T> Type of the object
     * @return Object which is converted from an XML file
     */
    public static <T> T unmarshal(Class<T> clazz, StreamSource source) throws JAXBException {
	    return unmarshal(clazz, source, null, null);
    }

    /**
     * Converts an XML content into a corresponding Object model following schema validation.
     * A common interface (StreamSource) is used to support different XML sources, i.e. String, File, etc.
     * For a String s, new StreamSource(new StringReader(s)) can be used.
     * For a File f, new StreamSource(f) can be used.
     * @param clazz Class type of the Object
     * @param source An XML source
     * @param schemaSource The XML source to read the schema from.
     * @param resourceResolver A resource resolver to resolve relevant schemas.
     * @param <T> Type of the object
     * @return Object which is converted from an XML file
     */
    public static <T> T unmarshal(Class<T> clazz, StreamSource source, StreamSource schemaSource, LSResourceResolver resourceResolver) throws JAXBException {
        JAXBContext context       = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        if (schemaSource != null) {
            try {
                var schemaFactory = getSecureSchemaFactory();
                if (resourceResolver != null) {
                    schemaFactory.setResourceResolver(resourceResolver);
                }
                var schema = schemaFactory.newSchema(schemaSource);
                unmarshaller.setSchema(schema);
            } catch (SAXException e) {
                throw new IllegalStateException("Provided schema could not be parsed", e);
            }
        }
        /*
         Use a factory that disables XML External Entity (XXE) attacks.
         This cannot be done by defining a bean since the XMLInputFactory
         is not thread safe.
         */
        XMLStreamReader xsr;
        try {
            xsr = getSecureXMLInputFactory().createXMLStreamReader(source);
        } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
        }
        JAXBElement<T> element    =  unmarshaller.unmarshal(xsr, clazz);
        return (T) element.getValue();
    }

    /**
     * Converts a JAXB object into a corresponding XML string. The objects "must" be wrapped
     * into a JAXBElement by ObjectFactory's before using it.
     * @param element Object to be converted
     * @param <T> Type of the object
     * @return XML String
     */
    public static <T> String marshalToString(JAXBElement<T> element) throws JAXBException {
        Class<?> clazz        = element.getValue().getClass();
        JAXBContext context   = JAXBContext.newInstance( clazz.getPackage().getName() );
        StringWriter writer   = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(element, writer);
        return writer.toString();
    }

    /**
     * Validate XML content using an XML Schema securely.
     * <p/>
     * Findings will be reported through the provided error handler which is also returned by this method.
     *
     * @param contentToValidate The input to validate.
     * @param schemaToValidateWith The schema to use.
     * @throws XMLStreamException If the input cannot be parsed as XML.
     * @throws SAXException If the input is invalid (not thrown for regular errors if a custom errorHandler is provided).
     */
    public static void validateAgainstSchema(InputStream contentToValidate, InputStream schemaToValidateWith) throws XMLStreamException, SAXException {
        validateAgainstSchema(new StreamSource(contentToValidate), new StreamSource(schemaToValidateWith), null, null);
    }

    /**
     * Validate XML content using an XML Schema securely.
     * <p/>
     * Findings will be reported through the provided error handler which is also returned by this method.
     *
     * @param contentToValidate The input to validate.
     * @param schemaToValidateWith The schema to use.
     * @param errorHandler The error handler to configure (optional).
     * @param resourceResolver The resource resolver to configure (optional).
     * @throws XMLStreamException If the input cannot be parsed as XML.
     * @throws SAXException If the input is invalid (not thrown for regular errors if a custom errorHandler is provided).
     */
    public static void validateAgainstSchema(Source contentToValidate, Source schemaToValidateWith, ErrorHandler errorHandler, LSResourceResolver resourceResolver) throws XMLStreamException, SAXException {
        /*
         * The security configuration for the Xerces parser involves:
         * - Setting the FEATURE_SECURE_PROCESSING to true.
         * - Using a secured underlying parser (see getSecureXMLInputFactory()) that completely disables DTD processing.
         * Xerces does not directly support the JAXP 1.5 features to disable XXE (ACCESS_EXTERNAL_DTD, ACCESS_EXTERNAL_SCHEMA)
         * but we ensure secure processing by means of the secured underlying parser.
         */
        XMLSchemaFactory factory = new XMLSchemaFactory();
        if (errorHandler != null) factory.setErrorHandler(errorHandler);
        if (resourceResolver != null) factory.setResourceResolver(resourceResolver);
        Schema schema;
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            schema = factory.newSchema(schemaToValidateWith);
        } catch (SAXException e) {
            throw new IllegalStateException("Unable to configure schema", e);
        }
        Validator validator = schema.newValidator();
        if (errorHandler != null) validator.setErrorHandler(errorHandler);
        if (resourceResolver != null) validator.setResourceResolver(resourceResolver);
        try {
            // If no custom error handler is set, the default implementation will throw an exception upon detected errors.
            validator.validate(new StAXSource(getSecureXMLInputFactory().createXMLStreamReader(contentToValidate)));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read input stream", e);
        }
    }

    /**
     * Converts a JAXB object into a corresponding XML structure
     * and writes it to given output stream. The objects "must" be wrapped
     * into a JAXBElement by ObjectFactory's before using it.
     * @param element Object to be converted
     * @param out Output stream that the XML structure will be written to
     * @param <T> Type of the object
     * @throws JAXBException
     */
    public static <T> void marshalToStream(JAXBElement<T> element, OutputStream out) throws JAXBException {
        Class<?> clazz        = element.getValue().getClass();
        JAXBContext context   = JAXBContext.newInstance( clazz.getPackage().getName() );
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(element, out);
    }

    /**
     * Converts a JAXB object into a corresponding XML Node
     * and returns it. The objects "must" be wrapped
     * into a JAXBElement by ObjectFactory's before using it.
     * @param element Object to be converted
     * @throws JAXBException
     * @return DOM Element
     */
    public static Node marshalToNode(JAXBElement element) throws JAXBException {
        DOMResult result = new DOMResult();
        Class<?> clazz   = element.getValue().getClass();
        JAXBContext context   = JAXBContext.newInstance( clazz.getPackage().getName() );
        Marshaller marshaller = context.createMarshaller();
        marshaller.marshal(element, result);
        return ((Document)result.getNode()).getDocumentElement();
    }

    /**
     * Wraps an arbitrary element of some type T into a JAXBElement<T>
     * without the need of ObjectFactory
     * @param namespace Namespace of the element
     * @param tag Name of the element
     * @param object Object to be wrapped
     * @param <T> Type of the object
     * @return JAXBElement<T>
     */
    public static <T> JAXBElement<T> wrap( String namespace, String tag, T object ){
        QName qname = new QName( namespace, tag );
        Class<?> clazz = object.getClass();
        JAXBElement<T> jxElement = new JAXBElement( qname, clazz, object );
        return jxElement;
    }

    public static SchemaFactory getSecureSchemaFactory() throws SAXNotSupportedException, SAXNotRecognizedException {
        /*
         * Xerces does not support the JAXP ACCESS_EXTERNAL_DTD and ACCESS_EXTERNAL_STYLESHEET features.
         * The correct approach for secure schema validation using Xerces is to ensure that the parser used to parse
         * the XML content is secured.
         *
         * Wherever this method is used, the implementation must additionally foresee that the XML content is
         * loaded after calling getSecureXMLInputFactory() to obtain the parser.
         */
        return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    }

    public static SAXParserFactory getSecureSAXParserFactory() throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
        // Use Xerces implementation for its advanced security features.
        SAXParserFactoryImpl saxParserFactory = new SAXParserFactoryImpl();
        saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        saxParserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        saxParserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        saxParserFactory.setXIncludeAware(false);
        return saxParserFactory;
    }

    public static DocumentBuilderFactory getSecureDocumentBuilderFactory() throws ParserConfigurationException {
        // Use Xerces implementation for its advanced security features.
        DocumentBuilderFactoryImpl docBuilderFactory = new DocumentBuilderFactoryImpl();
        docBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        docBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        docBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        docBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        docBuilderFactory.setXIncludeAware(false);
        docBuilderFactory.setExpandEntityReferences(false);
        return docBuilderFactory;
    }

    /**
     * Parses an XML document from a provided InputStream and sets a "lineNumber" attribute for each node
     * @param is InputStream of XML document to be parsed
     * @return DOM Document whose nodes have its line number
     * @throws IOException
     * @throws SAXException
     */
    public static Document readXMLWithLineNumbers(InputStream is) throws IOException, SAXException {
        final Document doc;
        SAXParser parser;
        try {
            final SAXParserFactory factory = getSecureSAXParserFactory();
            parser = factory.newSAXParser();
            final DocumentBuilder docBuilder = getSecureDocumentBuilderFactory().newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
        }

        final Stack<org.w3c.dom.Element> elementStack = new Stack<>();
        final StringBuilder textBuffer = new StringBuilder();
        final DefaultHandler handler = new DefaultHandler() {
            private Locator locator;

            @Override
            public void setDocumentLocator(final Locator locator) {
                this.locator = locator; // Save the locator, so that it can be used later for line tracking when traversing nodes.
            }

            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                addTextIfNeeded();
                final org.w3c.dom.Element el = doc.createElement(qName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    el.setAttribute(attributes.getQName(i), attributes.getValue(i));
                }
                el.setUserData(LINE_NUMBER_KEY_NAME, String.valueOf(this.locator.getLineNumber()), null);
                elementStack.push(el);
            }

            @Override
            public void endElement(final String uri, final String localName, final String qName) {
                addTextIfNeeded();
                final org.w3c.dom.Element closedEl = elementStack.pop();
                if (elementStack.isEmpty()) { // Is this the root element?
                    doc.appendChild(closedEl);
                } else {
                    final org.w3c.dom.Element parentEl = elementStack.peek();
                    parentEl.appendChild(closedEl);
                }
            }

            @Override
            public void characters(final char ch[], final int start, final int length) throws SAXException {
                textBuffer.append(ch, start, length);
            }

            // Outputs text accumulated under the current node
            private void addTextIfNeeded() {
                if (textBuffer.length() > 0) {
                    final org.w3c.dom.Element el = elementStack.peek();
                    final Node textNode = doc.createTextNode(textBuffer.toString());
                    el.appendChild(textNode);
                    textBuffer.delete(0, textBuffer.length());
                }
            }
        };
        parser.parse(is, handler);

        return doc;
    }

    /**
     * Generates an XML Signature from given XML document by using Java XML Digital Signature API (JSR 105)
     * and envelopes this signature into the same document under a specific parent node.
     * @param parentNode XML DOM Node under which XML Signature is generated
     * @param certificate Public certificate to get information about keystore owner
     * @param privateKey Private key to sign XML document
     * @return XML DOM Document with a generated XML signature
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws javax.xml.crypto.MarshalException
     * @throws XMLSignatureException
     */
    public static Node sign(Node parentNode, X509Certificate certificate, PrivateKey privateKey) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, javax.xml.crypto.MarshalException, XMLSignatureException {
        // First, create the DOM XMLSignatureFactory that will be used to generate the XMLSignature
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // Create a Reference to the enveloped document (in this case we are
        // signing the whole document, so a URI of "" signifies that) and
        // also specify the SHA1 digest algorithm and the ENVELOPED Transform.
        DigestMethod digestMethod = fac.newDigestMethod(DigestMethod.SHA1, null);
        Transform transform = fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);
        Reference reference = fac.newReference("", digestMethod, Collections.singletonList(transform), null, null);

        // Create the SignedInfo
        SignatureMethod signatureMethod = fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null);
        CanonicalizationMethod canonicalizationMethod = fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null);
        SignedInfo si = fac.newSignedInfo(canonicalizationMethod, signatureMethod, Collections.singletonList(reference));

        // Create the KeyInfo containing the X509Data.
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        List x509Content = new ArrayList();
        x509Content.add(certificate.getSubjectX500Principal().getName());
        x509Content.add(certificate);
        X509Data xd = kif.newX509Data(x509Content);
        KeyInfo ki  = kif.newKeyInfo(Collections.singletonList(xd));

        // Create a DOMSignContext and specify the RSA PrivateKey and
        // location of the resulting XMLSignature's parent element.
        DOMSignContext dsc = new DOMSignContext(privateKey, parentNode);

        // Create the XMLSignature, but don't sign it yet.
        XMLSignature signature = fac.newXMLSignature(si, ki);

        // Marshal, generate, and sign the enveloped signature.
        signature.sign(dsc);

        return parentNode;
    }

    /**
     * Generates an XML Signature from given XML document by using Java XML Digital Signature API (JSR 105)
     * and envelopes this signature into the same document
     * @param doc XML DOM Document from which XML Signature is generated
     * @param certificate Public certificate to get information about keystore owner
     * @param privateKey Private key to sign XML document
     * @return XML DOM Document with a generated XML signature
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws javax.xml.crypto.MarshalException
     * @throws XMLSignatureException
     */
    public static Document sign(Document doc, X509Certificate certificate, PrivateKey privateKey) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, javax.xml.crypto.MarshalException, XMLSignatureException {
        sign(doc.getDocumentElement(), certificate, privateKey);
        return doc;
    }

    public static TransformerFactory getSecureTransformerFactory() {
        var factory = new SaxonTransformerFactory();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return factory;
    }

    public static XMLInputFactory getSecureXMLInputFactory() {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return xmlInputFactory;
    }

    public static byte[] convertDocumentToByteArray(Document document) throws TransformerException {
        TransformerFactory transformerFactory = getSecureTransformerFactory();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(bos);
        transformer.transform(source, result);
        return bos.toByteArray();
    }

    /**
     * Searches a child node with its node name in a parent node and returns it
     * @param parent Node to be searched
     * @param name Node name of the desired node
     * @return Node if found, null otherwise
     */
    public static Node getChildNodeByName(Node parent, String name) {
        if(parent != null) {
            for(int i=0; i<parent.getChildNodes().getLength(); i++){
                Node node = parent.getChildNodes().item(i);
                if(node.getNodeName().equals(name))
                    return node;
            }
        }
        return null;
    }

    public static Path prettyPrintXmlFile(Path xmlFile) {
        var tempReportPath = xmlFile.resolveSibling("temp."+xmlFile.getFileName().toString());
        try (var xmlStream = Files.newInputStream(xmlFile)) {
            var transformer = getSecureTransformerFactory().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(
                new StAXSource(XMLUtils.getSecureXMLInputFactory().createXMLStreamReader(xmlStream)),
                new StreamResult(tempReportPath.toFile())
            );
        } catch (Exception e) {
            throw new IllegalStateException("Unable to apply pretty-print transformation", e);
        }
        try {
            Files.move(tempReportPath, xmlFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to complete pretty-printing", e);
        }
        return xmlFile;
    }
}
