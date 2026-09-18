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

package com.gitb.engine.validation.handlers.schematron;

import com.gitb.utils.XMLUtils;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SchematronLocationResolverTest {

    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <inv:Invoice xmlns:inv="urn:test:invoice" xmlns:cac="urn:test:cac" xmlns:cbc="urn:test:cbc">
                <cbc:ID>INV-001</cbc:ID>
                <cac:Line id="L1">
                    <cbc:Quantity>1</cbc:Quantity>
                </cac:Line>
                <cac:Line id="L2">
                    <cbc:Quantity>2</cbc:Quantity>
                </cac:Line>
                <cac:Line id="L3">
                    <cbc:Quantity>3</cbc:Quantity>
                </cac:Line>
            </inv:Invoice>
            """;

    private Document parse() throws Exception {
        return XMLUtils.readXMLWithLineNumbers(new ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Evaluate the expression using the same mechanism as the resolver's own fallback (a Saxon XPath evaluation),
     * to confirm the fast resolver returns exactly the same node.
     */
    private Node evaluateWithXPath(Document document, String expression) throws Exception {
        XPath xPath = new XPathFactoryImpl().newXPath();
        return (Node) xPath.evaluate(expression, document, XPathConstants.NODE);
    }

    @Test
    void testResolvesRootElement() throws Exception {
        Document document = parse();
        String expression = "/*:Invoice[namespace-uri()='urn:test:invoice'][1]";
        var resolved = new SchematronLocationResolver(document).resolve(expression);
        assertTrue(resolved.isPresent());
        assertSame(evaluateWithXPath(document, expression), resolved.get());
        assertSame(document.getDocumentElement(), resolved.get());
    }

    @Test
    void testResolvesNestedElementByPosition() throws Exception {
        Document document = parse();
        String expression = "/*:Invoice[namespace-uri()='urn:test:invoice'][1]/*:Line[namespace-uri()='urn:test:cac'][2]/*:Quantity[namespace-uri()='urn:test:cbc'][1]";
        var resolved = new SchematronLocationResolver(document).resolve(expression);
        assertTrue(resolved.isPresent());
        assertSame(evaluateWithXPath(document, expression), resolved.get());
        assertEquals("2", resolved.get().getTextContent());
    }

    @Test
    void testResolvesAttribute() throws Exception {
        Document document = parse();
        String expression = "/*:Invoice[namespace-uri()='urn:test:invoice'][1]/*:Line[namespace-uri()='urn:test:cac'][2]/@id";
        var resolved = new SchematronLocationResolver(document).resolve(expression);
        assertTrue(resolved.isPresent());
        assertSame(evaluateWithXPath(document, expression), resolved.get());
        assertEquals("L2", resolved.get().getNodeValue());
    }

    @Test
    void testResolvesRepeatedCallsConsistently() throws Exception {
        // Exercises the memoisation path: resolving a sibling under an already-visited ancestor.
        Document document = parse();
        var resolver = new SchematronLocationResolver(document);
        String first = "/*:Invoice[namespace-uri()='urn:test:invoice'][1]/*:Line[namespace-uri()='urn:test:cac'][1]/@id";
        String second = "/*:Invoice[namespace-uri()='urn:test:invoice'][1]/*:Line[namespace-uri()='urn:test:cac'][3]/@id";
        assertEquals("L1", resolver.resolve(first).orElseThrow().getNodeValue());
        assertEquals("L3", resolver.resolve(second).orElseThrow().getNodeValue());
    }

    @Test
    void testFallsBackForNonCanonicalExpression() throws Exception {
        Document document = parse();
        var resolver = new SchematronLocationResolver(document);
        // Raw, prefixed XPath (e.g. as used by the "pure" Schematron path) rather than the canonical,
        // XSLT-generated form the resolver understands.
        assertTrue(resolver.resolve("/inv:Invoice/cac:Line[2]").isEmpty());
    }

    @Test
    void testFallsBackForRelativeExpression() throws Exception {
        Document document = parse();
        var resolver = new SchematronLocationResolver(document);
        assertTrue(resolver.resolve("*:Invoice[namespace-uri()='urn:test:invoice'][1]").isEmpty());
    }

    @Test
    void testFallsBackForUnknownElementName() throws Exception {
        Document document = parse();
        var resolver = new SchematronLocationResolver(document);
        assertTrue(resolver.resolve("/*:Invoice[namespace-uri()='urn:test:invoice'][1]/*:DoesNotExist[namespace-uri()='urn:test:cac'][1]").isEmpty());
    }

    @Test
    void testFallsBackForPositionBeyondAvailableSiblings() throws Exception {
        Document document = parse();
        var resolver = new SchematronLocationResolver(document);
        assertTrue(resolver.resolve("/*:Invoice[namespace-uri()='urn:test:invoice'][1]/*:Line[namespace-uri()='urn:test:cac'][99]").isEmpty());
    }

    @Test
    void testFallsBackForMismatchedNamespace() throws Exception {
        Document document = parse();
        var resolver = new SchematronLocationResolver(document);
        assertTrue(resolver.resolve("/*:Invoice[namespace-uri()='urn:test:invoice'][1]/*:Line[namespace-uri()='urn:wrong'][1]").isEmpty());
    }

    @Test
    void testFallsBackForMismatchedRootPosition() throws Exception {
        Document document = parse();
        var resolver = new SchematronLocationResolver(document);
        assertTrue(resolver.resolve("/*:Invoice[namespace-uri()='urn:test:invoice'][2]").isEmpty());
    }
}
