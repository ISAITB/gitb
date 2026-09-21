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

import com.gitb.tr.BAR;
import com.gitb.tr.TAR;
import com.gitb.utils.XMLUtils;
import com.helger.schematron.svrl.SVRLMarshaller;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused tests for {@link SchematronReportHandler}, covering the Schematron performance fix for
 * <a href="https://github.com/ISAITB/xml-validator/issues/4">xml-validator#4</a> as ported into the engine:
 * lazy resolution of the line-numbered document, and the fallback line number for a location that resolves to an
 * attribute node.
 * <p/>
 * The attribute-location case is exercised directly against a hand-built SVRL document (rather than through the
 * real Schematron engines) because it turns out neither is a reliable way to trigger it in this ph-schematron
 * version: the ISO-XSLT2 skeleton's "schematron-select-full-path" mode has no template for attribute nodes (an
 * attribute-context rule's location degrades to the attribute's raw text value, not a path), and the pure engine
 * throws {@code IllegalStateException: Failed to find Node with name '...' at parent} from
 * {@code com.helger.xml.XMLHelper._getPathToNode} for an attribute-context rule against the line-numbered DOM (a
 * separate, pre-existing limitation of that DOM/Saxon combination, unrelated to this fix). The pure engine's
 * {@code PSXPathValidationHandlerSVRL._getPathToNode} does still correctly emit a genuine attribute-ending path
 * (e.g. {@code /inv:Invoice/cac:Line[2]/@id}) when it does not hit that limitation, which is the input shape this
 * test constructs directly.
 */
class SchematronReportHandlerTest {

    private static final String XML = """
            <inv:Invoice xmlns:inv="urn:test:invoice" xmlns:cac="urn:test:cac" xmlns:cbc="urn:test:cbc">
                <cbc:ID>INV-001</cbc:ID>
                <cac:Line id="L1">
                    <cbc:Quantity>1</cbc:Quantity>
                </cac:Line>
                <cac:Line id="L2">
                    <cbc:Quantity>2</cbc:Quantity>
                </cac:Line>
            </inv:Invoice>
            """;

    private Document lineNumberedDocument() throws Exception {
        return XMLUtils.readXMLWithLineNumbers(new ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8)));
    }

    private com.helger.schematron.svrl.jaxb.SchematronOutputType svrl(String svrlXml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document svrlDoc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(svrlXml.getBytes(StandardCharsets.UTF_8)));
        return new SVRLMarshaller(false).read(svrlDoc);
    }

    @Test
    void testAttributeLocationFallsBackToOwningElementLine() throws Exception {
        // A genuine attribute-ending location, as produced by the pure engine's own _getPathToNode when it does
        // not hit the limitation described in the class Javadoc - a plain, prefixed XPath (not the canonical
        // XSLT-generated form), so the fast resolver correctly does not match it and this exercises the Saxon
        // XPath fallback in SchematronReportHandler.getLocationInfo().
        String svrlXml = """
                <svrl:schematron-output xmlns:svrl="http://purl.oclc.org/dsdl/svrl">
                  <svrl:failed-assert location="/inv:Invoice/cac:Line[2]/@id" test="string-length(.) &gt; 3">
                    <svrl:text>Line id must be more than 3 characters</svrl:text>
                  </svrl:failed-assert>
                </svrl:schematron-output>
                """;
        Document document = lineNumberedDocument();
        var handler = new SchematronReportHandler(XML, null, () -> document, svrl(svrlXml), true, false, false);
        TAR report = handler.createReport();
        assertEquals(1, report.getReports().getInfoOrWarningOrError().size());
        BAR bar = (BAR) report.getReports().getInfoOrWarningOrError().getFirst().getValue();
        // Line 6 is "cac:Line id=\"L2\"" - the owning element of the matched @id attribute. Before the fix this
        // returned the literal string "null" (the line-number user data is only ever set on elements), producing
        // "xml:null:0".
        assertEquals("xml:6:0", bar.getLocation());
    }

    @Test
    void testDocumentIsNotResolvedWhenThereAreNoFindings() throws Exception {
        String svrlXml = """
                <svrl:schematron-output xmlns:svrl="http://purl.oclc.org/dsdl/svrl"/>
                """;
        AtomicInteger callCount = new AtomicInteger();
        var handler = new SchematronReportHandler(XML, null, () -> {
            callCount.incrementAndGet();
            throw new AssertionError("Document should not be resolved when there are no findings");
        }, svrl(svrlXml), false, false, false);
        TAR report = handler.createReport();
        assertTrue(report.getReports().getInfoOrWarningOrError().isEmpty());
        assertEquals(0, callCount.get());
    }

    @Test
    void testDocumentIsResolvedOnlyOnceForMultipleFindings() throws Exception {
        String svrlXml = """
                <svrl:schematron-output xmlns:svrl="http://purl.oclc.org/dsdl/svrl">
                  <svrl:failed-assert location="/*:Invoice[namespace-uri()='urn:test:invoice'][1]/*:Line[namespace-uri()='urn:test:cac'][1]" test="a">
                    <svrl:text>First</svrl:text>
                  </svrl:failed-assert>
                  <svrl:failed-assert location="/*:Invoice[namespace-uri()='urn:test:invoice'][1]/*:Line[namespace-uri()='urn:test:cac'][2]" test="b">
                    <svrl:text>Second</svrl:text>
                  </svrl:failed-assert>
                </svrl:schematron-output>
                """;
        AtomicInteger callCount = new AtomicInteger();
        Document document = lineNumberedDocument();
        var handler = new SchematronReportHandler(XML, null, () -> {
            callCount.incrementAndGet();
            return document;
        }, svrl(svrlXml), false, false, false);
        TAR report = handler.createReport();
        assertEquals(2, report.getReports().getInfoOrWarningOrError().size());
        assertEquals(1, callCount.get());
        var bars = report.getReports().getInfoOrWarningOrError().stream().map(e -> (BAR) e.getValue()).toList();
        assertEquals("xml:3:0", bars.get(0).getLocation());
        assertEquals("xml:6:0", bars.get(1).getLocation());
    }
}
