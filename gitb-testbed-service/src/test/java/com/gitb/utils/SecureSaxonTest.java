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

package com.gitb.utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.InputSource;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class SecureSaxonTest {

    private static WireMockServer server;
    private static String base;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void start() {
        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        base = "http://localhost:" + server.port();
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    @BeforeEach
    void reset() {
        server.resetAll();
        UriAuthorizer.setGlobal(new UriAuthorizer(List.of()));
    }

    @AfterEach
    void restore() {
        UriAuthorizer.setGlobal(null);
    }

    private static String messages(Throwable t) {
        var builder = new StringBuilder();
        for (Throwable current = t; current != null; current = current.getCause()) {
            builder.append(current.getMessage()).append(" | ");
        }
        return builder.toString();
    }

    private static void assertBlocked(org.junit.jupiter.api.function.Executable executable, String expectedFragment) {
        var e = assertThrows(Exception.class, executable);
        assertTrue(messages(e).contains(expectedFragment), "Expected [" + expectedFragment + "] in: " + messages(e));
        assertFalse(messages(e).contains("TOP-SECRET"));
    }

    private Object xpath(String expression) throws XPathExpressionException {
        return SecureSaxon.newXPathFactory().newXPath().evaluate(expression, new InputSource(new StringReader("<a/>")), XPathConstants.STRING);
    }

    private String transform(String xslt) throws Exception {
        var transformer = SecureSaxon.newTransformerFactory().newTransformer(new StreamSource(new StringReader(xslt)));
        var out = new StringWriter();
        transformer.transform(new StreamSource(new StringReader("<a/>")), new StreamResult(out));
        return out.toString();
    }

    @Test
    void localFilesCannotBeRead() throws Exception {
        Path secret = Files.writeString(tempDir.resolve("secret.txt"), "TOP-SECRET");
        Path secretXml = Files.writeString(tempDir.resolve("secret.xml"), "<secret>TOP-SECRET</secret>");
        String uri = secret.toUri().toString();
        String notAllowed = "only http and https";
        assertBlocked(() -> xpath("unparsed-text('" + uri + "')"), notAllowed);
        assertBlocked(() -> xpath("string-join(unparsed-text-lines('" + uri + "'))"), notAllowed);
        assertBlocked(() -> xpath("string(doc('" + secretXml.toUri() + "'))"), notAllowed);
        assertBlocked(() -> xpath("json-doc('" + uri + "')"), notAllowed);
        assertBlocked(() -> xpath("count(collection('" + tempDir.toUri() + "?select=*.txt'))"), "Collections are not supported");
        assertBlocked(() -> xpath("count(uri-collection('" + tempDir.toUri() + "'))"), "Collections are not supported");
        assertBlocked(() -> xpath("unparsed-text('jar:" + uri + "!/x')"), notAllowed);
    }

    @Test
    void environmentVariablesAreHidden() throws Exception {
        // PATH is always defined in the environment.
        assertEquals("", xpath("environment-variable('PATH')"));
        assertEquals("0", xpath("count(available-environment-variables())"));
    }

    @Test
    void internalHttpAddressesCannotBeRead() {
        server.stubFor(get("/doc.xml").willReturn(aResponse().withBody("<root/>")));
        assertBlocked(() -> xpath("name(doc('" + base + "/doc.xml')/*)"), "internal or reserved address");
        assertBlocked(() -> xpath("unparsed-text('" + base + "/doc.xml')"), "internal or reserved address");
        server.verify(0, getRequestedFor(urlEqualTo("/doc.xml")));
    }

    @Test
    void whitelistedHttpAddressesCanBeRead() throws Exception {
        UriAuthorizer.setGlobal(new UriAuthorizer(List.of(base + "/")));
        server.stubFor(get("/doc.xml").willReturn(aResponse().withHeader("Content-Type", "application/xml").withBody("<root/>")));
        server.stubFor(get("/doc.txt").willReturn(aResponse().withHeader("Content-Type", "text/plain").withBody("hello")));
        server.stubFor(get("/doc.json").willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{\"a\":\"b\"}")));
        assertEquals("root", xpath("name(doc('" + base + "/doc.xml')/*)"));
        assertEquals("hello", xpath("unparsed-text('" + base + "/doc.txt')"));
        assertEquals("b", xpath("json-doc('" + base + "/doc.json')?a"));
    }

    @Test
    void documentsWithDoctypeAreRejected() {
        UriAuthorizer.setGlobal(new UriAuthorizer(List.of(base + "/")));
        server.stubFor(get("/xxe.xml").willReturn(aResponse().withHeader("Content-Type", "application/xml")
                .withBody("<!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><r>&x;</r>")));
        assertBlocked(() -> xpath("string(doc('" + base + "/xxe.xml'))"), "DOCTYPE");
    }

    @Test
    void stylesheetsCannotReadFilesOrWriteResultDocuments() throws Exception {
        Path secret = Files.writeString(tempDir.resolve("secret.txt"), "TOP-SECRET");
        assertBlocked(() -> transform("""
                <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                  <xsl:template match="/"><xsl:value-of select="unparsed-text('%s')"/></xsl:template>
                </xsl:stylesheet>""".formatted(secret.toUri())), "only http and https");
        Path target = tempDir.resolve("written.xml");
        String writingStylesheet = """
                <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                  <xsl:template match="/"><xsl:result-document href="%s"><x/></xsl:result-document></xsl:template>
                </xsl:stylesheet>""".formatted(target.toUri());
        // Disabled together with extension functions (so stylesheets cannot write documents).
        var compilationErrors = new StringBuilder();
        var factory = SecureSaxon.newTransformerFactory();
        factory.setErrorListener(new javax.xml.transform.ErrorListener() {
            @Override
            public void warning(javax.xml.transform.TransformerException e) {
                compilationErrors.append(e.getMessage());
            }

            @Override
            public void error(javax.xml.transform.TransformerException e) {
                compilationErrors.append(e.getMessage());
            }

            @Override
            public void fatalError(javax.xml.transform.TransformerException e) {
                compilationErrors.append(e.getMessage());
            }
        });
        assertThrows(Exception.class, () -> factory.newTransformer(new StreamSource(new StringReader(writingStylesheet))));
        assertTrue(compilationErrors.toString().contains("xsl:result-document is disabled"), compilationErrors.toString());
        assertFalse(Files.exists(target));
        assertEquals("", transform("""
                <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                  <xsl:output method="text"/>
                  <xsl:template match="/"><xsl:value-of select="environment-variable('PATH')"/></xsl:template>
                </xsl:stylesheet>"""));
    }

    @Test
    void ordinaryExpressionsAndStylesheetsStillWork() throws Exception {
        assertEquals("3", xpath("string(1 + 2)"));
        assertTrue(transform("""
                <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                  <xsl:template match="/"><b/></xsl:template>
                </xsl:stylesheet>""").contains("<b/>"));
    }

}
