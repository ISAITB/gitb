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

package com.gitb.tbs;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.TestEngineConfiguration;
import com.gitb.engine.utils.rdf.RdfContentRestrictions;
import com.gitb.utils.UriAuthorizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.gitb.tbs.TdlTestHelper.configureOnly;
import static com.gitb.tbs.TdlTestHelper.run;
import static com.gitb.tbs.TdlTestHelper.stringInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the content processed by the engine (expressions, stylesheets, schemas, shapes, models, templates and
 * inputs) cannot be used to read local files, reach internal addresses (unless whitelisted) or otherwise access
 * resources that should not be reachable.
 * <p>
 * Resources served by the WireMock server below "/ext/" are on an internal address (localhost). They can only be
 * looked up if whitelisted. The requests made to them are counted to verify that blocked lookups never reach the server.
 */
class ResourceAccessRestrictionsTest extends BaseIntegrationTest {

    private static final String SECRET = "TOP-SECRET-CONTENT";

    @TempDir
    Path tempDir;

    @BeforeAll
    static void stubAll() {
        for (String id : new String[]{
                "sec-expr-unparsed-text", "sec-expr-doc", "sec-expr-env", "sec-expr-collection", "sec-xpath-processor",
                "sec-xslt", "sec-template", "sec-json", "sec-xsd", "sec-schematron", "sec-shacl", "sec-rdf-convert",
                "sec-rdf-ask", "sec-uri-input", "sec-xsd-import-artifact", "sec-xsd-hint"
        }) {
            stubTdl(id, "tdl/sec/" + id + ".xml");
        }
    }

    @BeforeEach
    void noInternalAddressesWhitelisted() {
        UriAuthorizer.setGlobal(new UriAuthorizer(List.of()));
    }

    @AfterEach
    void restoreAuthorizer() {
        UriAuthorizer.setGlobal(null);
        TestEngineConfiguration.RDF_CONTENT_RESTRICTIONS = RdfContentRestrictions.NONE;
    }

    // Helpers - start.

    private void whitelistExternalResources() {
        UriAuthorizer.setGlobal(new UriAuthorizer(List.of(base() + "/ext/")));
    }

    private String base() {
        return "http://localhost:" + wireMockPort();
    }

    /**
     * Serve a resource on the (internal) WireMock server under a unique path.
     *
     * @return The path.
     */
    private String serve(String contentType, String body) {
        String path = "/ext/" + UUID.randomUUID();
        stubFor(get(urlPathEqualTo(path)).willReturn(aResponse().withHeader("Content-Type", contentType).withBody(body)));
        return path;
    }

    private String serveRedirect(String target) {
        String path = "/ext/" + UUID.randomUUID();
        stubFor(get(urlPathEqualTo(path)).willReturn(aResponse().withStatus(302).withHeader("Location", target)));
        return path;
    }

    /**
     * Serve a resource that is not under the whitelisted path, to be reached only via a redirect.
     */
    private String serveOutsideWhitelist(String contentType, String body) {
        String path = "/internal/" + UUID.randomUUID();
        stubFor(get(urlPathEqualTo(path)).willReturn(aResponse().withHeader("Content-Type", contentType).withBody(body)));
        return path;
    }

    private String secretFileUri() throws Exception {
        return Files.writeString(tempDir.resolve("secret.txt"), SECRET).toUri().toString();
    }

    private String secretFileUri(String name, String content) throws Exception {
        return Files.writeString(tempDir.resolve(name), content).toUri().toString();
    }

    private static AnyContent typedInput(String name, String type, String value) {
        AnyContent input = stringInput(name, value);
        input.setType(type);
        return input;
    }

    private static List<AnyContent> inputs(String... namesAndValues) {
        List<AnyContent> inputs = new ArrayList<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            inputs.add(stringInput(namesAndValues[i], namesAndValues[i + 1]));
        }
        return inputs;
    }

    private static String logs(TestRunResult result) {
        return String.join("\n", result.logMessages());
    }

    /**
     * Assert that the session failed because of the expected reason, without exposing any secret.
     */
    private static void assertBlocked(TestRunResult result, String expectedReason) {
        result.assertFailed();
        String logs = logs(result);
        assertTrue(logs.contains(expectedReason), "Expected [" + expectedReason + "] in the logs:\n" + logs);
        assertFalse(logs.contains(SECRET), "The secret must not be exposed:\n" + logs);
    }

    // Helpers - end.

    // TDL expressions - start.

    @Test
    void expressionCannotReadLocalFile() throws Exception {
        assertBlocked(run("sec-expr-unparsed-text", null, inputs("url", secretFileUri(), "expected", SECRET)), "only http and https");
    }

    @Test
    void expressionCannotReadInternalAddress() throws Exception {
        String path = serve("text/plain", SECRET);
        assertBlocked(run("sec-expr-unparsed-text", null, inputs("url", base() + path, "expected", SECRET)), "internal or reserved address");
        assertEquals(0, requestCount(path));
    }

    @Test
    void expressionCanReadWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String path = serve("text/plain", "hello");
        run("sec-expr-unparsed-text", null, inputs("url", base() + path, "expected", "hello")).assertSuccess();
        assertEquals(1, requestCount(path));
    }

    @Test
    void expressionCannotFollowRedirectToNonWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String target = serveOutsideWhitelist("text/plain", SECRET);
        String path = serveRedirect(target);
        assertBlocked(run("sec-expr-unparsed-text", null, inputs("url", base() + path, "expected", SECRET)), "internal or reserved address");
        assertEquals(0, requestCount(target));
    }

    @Test
    void expressionCannotLoadDocumentFromLocalFile() throws Exception {
        assertBlocked(run("sec-expr-doc", null, inputs("url", secretFileUri("secret.xml", "<root>" + SECRET + "</root>"))), "only http and https");
    }

    @Test
    void expressionCanLoadDocumentFromWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String path = serve("application/xml", "<root/>");
        run("sec-expr-doc", null, inputs("url", base() + path)).assertSuccess();
    }

    @Test
    void expressionCannotLoadDocumentWithDoctype() throws Exception {
        whitelistExternalResources();
        String path = serve("application/xml", "<!DOCTYPE root [<!ENTITY x SYSTEM \"" + secretFileUri() + "\">]><root>&x;</root>");
        assertBlocked(run("sec-expr-doc", null, inputs("url", base() + path)), "DOCTYPE");
    }

    @Test
    void expressionCannotReadEnvironmentVariables() throws Exception {
        // The HMAC key is part of the environment of the test JVM (see the surefire configuration).
        run("sec-expr-env").assertSuccess();
    }

    @Test
    void expressionCannotListCollections() throws Exception {
        assertBlocked(run("sec-expr-collection", null, inputs("url", tempDir.toUri().toString() + "?select=*.txt")), "Collections are not supported");
    }

    @Test
    void xpathProcessorExpressionCannotReadLocalFile() throws Exception {
        assertBlocked(run("sec-xpath-processor", null, inputs("xpath", "unparsed-text('" + secretFileUri() + "')", "expected", SECRET)), "only http and https");
    }

    @Test
    void xpathProcessorStillEvaluatesExpressions() throws Exception {
        run("sec-xpath-processor", null, inputs("xpath", "/root/name", "expected", "value")).assertSuccess();
    }

    // TDL expressions - end.

    // XSLT and templates - start.

    private static String stylesheet(String templateBody) {
        return "<xsl:stylesheet version=\"3.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"><xsl:output method=\"text\"/><xsl:template match=\"/\">" + templateBody + "</xsl:template></xsl:stylesheet>";
    }

    @Test
    void xsltStillTransforms() throws Exception {
        run("sec-xslt", null, inputs("xslt", stylesheet("<xsl:value-of select=\"/root/item\"/>"), "expected", "hello")).assertSuccess();
    }

    @Test
    void xsltCannotReadLocalFile() throws Exception {
        assertBlocked(run("sec-xslt", null, inputs("xslt", stylesheet("<xsl:value-of select=\"unparsed-text('" + secretFileUri() + "')\"/>"), "expected", SECRET)), "only http and https");
    }

    @Test
    void xsltCannotReadInternalAddress() throws Exception {
        String path = serve("text/plain", SECRET);
        assertBlocked(run("sec-xslt", null, inputs("xslt", stylesheet("<xsl:value-of select=\"unparsed-text('" + base() + path + "')\"/>"), "expected", SECRET)), "internal or reserved address");
        assertEquals(0, requestCount(path));
    }

    @Test
    void xsltCanReadWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String path = serve("text/plain", "remote");
        run("sec-xslt", null, inputs("xslt", stylesheet("<xsl:value-of select=\"unparsed-text('" + base() + path + "')\"/>"), "expected", "remote")).assertSuccess();
    }

    @Test
    void xsltCannotWriteFiles() throws Exception {
        Path target = tempDir.resolve("written.txt");
        var result = run("sec-xslt", null, inputs("xslt", stylesheet("<xsl:result-document href=\"" + target.toUri() + "\"><xsl:text>x</xsl:text></xsl:result-document>"), "expected", ""));
        result.assertFailed();
        assertFalse(Files.exists(target));
    }

    @Test
    void xsltCannotReadEnvironmentVariables() throws Exception {
        run("sec-xslt", null, inputs("xslt", stylesheet("<xsl:value-of select=\"environment-variable('HMAC_KEY')\"/>"), "expected", "")).assertSuccess();
    }

    @Test
    void templateStillProcesses() throws Exception {
        run("sec-template", null, inputs("template", "<#assign a = 1>value ${a}", "expected", "value 1")).assertSuccess();
    }

    @Test
    void templateCannotInstantiateArbitraryClasses() throws Exception {
        Path marker = tempDir.resolve("executed.txt");
        String command = System.getProperty("os.name").toLowerCase().contains("win")
                ? "cmd /c echo x > \"" + marker + "\""
                : "touch " + marker;
        TestRunResult result = run("sec-template", null, inputs(
                "template", "<#assign ex=\"freemarker.template.utility.Execute\"?new()>${ex(\"" + command.replace("\\", "\\\\").replace("\"", "\\\"") + "\")}",
                "expected", "irrelevant"));
        result.assertFailed();
        assertFalse(Files.exists(marker), "The command must not be executed");
    }

    // XSLT and templates - end.

    // JSON schema - start.

    private static String schemaReferring(String ref) {
        return "{\"type\":\"object\",\"properties\":{\"name\":{\"$ref\":\"" + ref + "\"}}}";
    }

    @Test
    void jsonSchemaCannotReferToClasspathResources() throws Exception {
        assertBlocked(run("sec-json", null, inputs("schema", schemaReferring("classpath:engine-module.properties"))), "is not allowed to be loaded");
        assertBlocked(run("sec-json", null, inputs("schema", schemaReferring("resource:engine-module.properties"))), "is not allowed to be loaded");
    }

    @Test
    void jsonSchemaCannotReferToLocalFiles() throws Exception {
        String uri = secretFileUri("schema.json", "{\"type\":\"string\"}");
        assertBlocked(run("sec-json", null, inputs("schema", schemaReferring(uri))), "only http and https");
    }

    @Test
    void jsonSchemaCannotReferToInternalAddress() throws Exception {
        String path = serve("application/json", "{\"type\":\"string\"}");
        assertBlocked(run("sec-json", null, inputs("schema", schemaReferring(base() + path))), "internal or reserved address");
        assertEquals(0, requestCount(path));
    }

    @Test
    void jsonSchemaCanReferToWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String path = serve("application/json", "{\"type\":\"string\"}");
        run("sec-json", null, inputs("schema", schemaReferring(base() + path))).assertSuccess();
        assertEquals(1, requestCount(path));
    }

    @Test
    void jsonSchemaCannotRedirectToNonWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String target = serveOutsideWhitelist("application/json", "{\"type\":\"string\"}");
        String path = serveRedirect(base() + target);
        assertBlocked(run("sec-json", null, inputs("schema", schemaReferring(base() + path))), "internal or reserved address");
        assertEquals(0, requestCount(target));
    }

    // JSON schema - end.

    // XML schema and Schematron - start.

    private static String xsdImporting(String schemaLocation) {
        return "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"urn:test:root\" targetNamespace=\"urn:test:root\" elementFormDefault=\"qualified\">"
                + "<xs:include schemaLocation=\"" + schemaLocation + "\"/>"
                + "</xs:schema>";
    }

    private static final String XS_NS = "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"";

    /**
     * An XSD that includes the referenced schema and uses the type it is expected to define.
     */
    private static String xsdUsingIncluded(String schemaLocation) {
        return "<xs:schema " + XS_NS + " xmlns=\"urn:test:root\" targetNamespace=\"urn:test:root\" elementFormDefault=\"qualified\">"
                + "<xs:include schemaLocation=\"" + schemaLocation + "\"/>"
                + "<xs:element name=\"root\"><xs:complexType><xs:sequence><xs:element name=\"name\" type=\"NameType\"/></xs:sequence></xs:complexType></xs:element>"
                + "</xs:schema>";
    }

    private static final String XSD_DEFINING_NAME_TYPE = "<xs:schema " + XS_NS + " xmlns=\"urn:test:root\" targetNamespace=\"urn:test:root\" elementFormDefault=\"qualified\">"
            + "<xs:simpleType name=\"NameType\"><xs:restriction base=\"xs:string\"/></xs:simpleType></xs:schema>";

    private String serveAt(String path, String contentType, String body) {
        stubFor(get(urlPathEqualTo(path)).willReturn(aResponse().withHeader("Content-Type", contentType).withBody(body)));
        return path;
    }

    @Test
    void xsdStillValidates() throws Exception {
        run("sec-xsd", null, inputs("xsd", "<xs:schema " + XS_NS + " xmlns=\"urn:test:root\" targetNamespace=\"urn:test:root\" elementFormDefault=\"qualified\">"
                + "<xs:element name=\"root\"><xs:complexType><xs:sequence><xs:element name=\"name\" type=\"xs:string\"/></xs:sequence></xs:complexType></xs:element></xs:schema>")).assertSuccess();
    }

    @Test
    void xsdStillIncludesTestSuiteArtifacts() throws Exception {
        stubTestResource("/resources/sec-xsd-import-artifact/.*main\\.xsd", "tdl/sec/xsd-import/main.xsd");
        stubTestResource("/resources/sec-xsd-import-artifact/.*types\\.xsd", "tdl/sec/xsd-import/types.xsd");
        run("sec-xsd-import-artifact").assertSuccess();
    }

    @Test
    void xsdWithInlineContentCannotIncludeRelativeResources() throws Exception {
        // There is no test suite from which a relative reference could be resolved.
        assertBlocked(run("sec-xsd", null, inputs("xsd", xsdImporting("types.xsd"))), "not part of the test suite");
    }

    @Test
    void xsdCanIncludeRemoteResourceFromWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        // The included schema is remote and itself includes (relatively) another remote schema that defines the type used.
        String dir = "/ext/" + UUID.randomUUID();
        serveAt(dir + "/a.xsd", "application/xml", "<xs:schema " + XS_NS + " xmlns=\"urn:test:root\" targetNamespace=\"urn:test:root\"><xs:include schemaLocation=\"b.xsd\"/></xs:schema>");
        serveAt(dir + "/b.xsd", "application/xml", XSD_DEFINING_NAME_TYPE);
        run("sec-xsd", null, inputs("xsd", xsdUsingIncluded(base() + dir + "/a.xsd"))).assertSuccess();
        assertEquals(1, requestCount(dir + "/a.xsd"));
        assertEquals(1, requestCount(dir + "/b.xsd"));
    }

    @Test
    void xsdCanIncludeRemoteResourceThatRedirectsWithinWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String target = serveAt("/ext/" + UUID.randomUUID() + ".xsd", "application/xml", XSD_DEFINING_NAME_TYPE);
        String redirect = serveRedirect(base() + target);
        run("sec-xsd", null, inputs("xsd", xsdUsingIncluded(base() + redirect))).assertSuccess();
        assertEquals(1, requestCount(target));
    }

    @Test
    void xsdCannotIncludeRemoteResourceAtInternalAddress() throws Exception {
        String path = serve("application/xml", XSD_DEFINING_NAME_TYPE);
        assertBlocked(run("sec-xsd", null, inputs("xsd", xsdUsingIncluded(base() + path))), "internal or reserved address");
        assertEquals(0, requestCount(path));
    }

    @Test
    void xsdCannotIncludeRemoteResourceThatRedirectsToNonWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String target = serveOutsideWhitelist("application/xml", XSD_DEFINING_NAME_TYPE);
        String redirect = serveRedirect(base() + target);
        assertBlocked(run("sec-xsd", null, inputs("xsd", xsdUsingIncluded(base() + redirect))), "internal or reserved address");
        assertEquals(0, requestCount(target));
    }

    @Test
    void xsdCannotIncludeLocalFile() throws Exception {
        assertBlocked(run("sec-xsd", null, inputs("xsd", xsdUsingIncluded(secretFileUri("other.xsd", XSD_DEFINING_NAME_TYPE)))), "was blocked");
    }

    @Test
    void xsdCannotIncludeOtherSchemes() throws Exception {
        assertBlocked(run("sec-xsd", null, inputs("xsd", xsdUsingIncluded("jar:file:///x.jar!/other.xsd"))), "was blocked");
        assertBlocked(run("sec-xsd", null, inputs("xsd", xsdUsingIncluded("classpath:engine-module.properties"))), "was blocked");
    }

    @Test
    void xsdSchemaLocationHintsInValidatedContentAreNotFollowed() throws Exception {
        whitelistExternalResources();
        // Even if the address could be looked up, the content being validated cannot cause schemas to be loaded.
        String path = serve("application/xml", "<xs:schema " + XS_NS + " targetNamespace=\"urn:test:other\"><xs:element name=\"x\" type=\"xs:string\"/></xs:schema>");
        TestRunResult result = run("sec-xsd-hint", null, inputs("hint", "urn:test:other " + base() + path));
        // The content cannot be validated without the schema for the other namespace (and the validation itself ran without errors).
        result.assertFailed();
        assertFalse(logs(result).contains("Unexpected error"), logs(result));
        assertEquals(0, requestCount(path));
    }

    @Test
    void schematronStillValidates() throws Exception {
        String pure = "<sch:schema xmlns:sch=\"http://purl.oclc.org/dsdl/schematron\"><sch:pattern><sch:rule context=\"/invoice\"><sch:assert test=\"total\">Total</sch:assert></sch:rule></sch:pattern></sch:schema>";
        run("sec-schematron", null, inputs("schematron", pure, "schematronType", "sch")).assertSuccess();
        String xslt = "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:svrl=\"http://purl.oclc.org/dsdl/svrl\">"
                + "<xsl:template match=\"/\"><svrl:schematron-output/></xsl:template></xsl:stylesheet>";
        run("sec-schematron", null, inputs("schematron", xslt, "schematronType", "xslt")).assertSuccess();
    }

    @Test
    void schematronPureCanReadWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String path = serve("application/xml", "<secret/>");
        String sch = "<sch:schema xmlns:sch=\"http://purl.oclc.org/dsdl/schematron\" queryBinding=\"xslt2\"><sch:pattern><sch:rule context=\"/invoice\">"
                + "<sch:assert test=\"doc('" + base() + path + "')/secret\">Read</sch:assert></sch:rule></sch:pattern></sch:schema>";
        run("sec-schematron", null, inputs("schematron", sch, "schematronType", "sch")).assertSuccess();
        assertEquals(1, requestCount(path));
    }

    @Test
    void schematronPureCannotReadLocalFiles() throws Exception {
        String uri = secretFileUri("secret.xml", "<secret>" + SECRET + "</secret>");
        String sch = "<sch:schema xmlns:sch=\"http://purl.oclc.org/dsdl/schematron\" queryBinding=\"xslt2\"><sch:pattern><sch:rule context=\"/invoice\">"
                + "<sch:assert test=\"doc('" + uri + "')/secret\">Read</sch:assert></sch:rule></sch:pattern></sch:schema>";
        TestRunResult result = run("sec-schematron", null, inputs("schematron", sch, "schematronType", "sch"));
        result.assertFailed();
        assertFalse(logs(result).contains(SECRET));
    }

    @Test
    void schematronPureCannotReadInternalAddress() throws Exception {
        String path = serve("application/xml", "<secret/>");
        String sch = "<sch:schema xmlns:sch=\"http://purl.oclc.org/dsdl/schematron\" queryBinding=\"xslt2\"><sch:pattern><sch:rule context=\"/invoice\">"
                + "<sch:assert test=\"doc('" + base() + path + "')/secret\">Read</sch:assert></sch:rule></sch:pattern></sch:schema>";
        run("sec-schematron", null, inputs("schematron", sch, "schematronType", "sch")).assertFailed();
        assertEquals(0, requestCount(path));
    }

    @Test
    void schematronXsltCannotListCollections() throws Exception {
        secretFileUri(); // Ensures there is a matching file in the collection.
        // A failed assertion is only reported if the collection is empty, so success would mean that the collection was listed.
        String xslt = "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:svrl=\"http://purl.oclc.org/dsdl/svrl\">"
                + "<xsl:template match=\"/\"><svrl:schematron-output>"
                + "<xsl:if test=\"count(collection('" + tempDir.toUri() + "?select=*.txt')) = 0\"><svrl:failed-assert test=\"x\" location=\"/invoice\"><svrl:text>none</svrl:text></svrl:failed-assert></xsl:if>"
                + "</svrl:schematron-output></xsl:template></xsl:stylesheet>";
        TestRunResult result = run("sec-schematron", null, inputs("schematron", xslt, "schematronType", "xslt"));
        result.assertFailed();
    }

    @Test
    void schematronXsltReportLocationCannotLoadResources() throws Exception {
        String path = serve("application/xml", "<invoice/>");
        // The location of a reported item is evaluated as an XPath expression against the validated document.
        String xslt = "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:svrl=\"http://purl.oclc.org/dsdl/svrl\">"
                + "<xsl:template match=\"/\"><svrl:schematron-output>"
                + "<svrl:failed-assert test=\"x\" location=\"doc('" + base() + path + "')/invoice\"><svrl:text>Reported</svrl:text></svrl:failed-assert>"
                + "</svrl:schematron-output></xsl:template></xsl:stylesheet>";
        run("sec-schematron", null, inputs("schematron", xslt, "schematronType", "xslt")).assertFailed();
        assertEquals(0, requestCount(path));
    }

    // XML schema and Schematron - end.

    // RDF - start.

    private static final String SHAPES_TTL = "@prefix sh: <http://www.w3.org/ns/shacl#> . @prefix ex: <http://example.org/> . "
            + "ex:PersonShape a sh:NodeShape ; sh:targetClass ex:Person ; sh:property [ sh:path ex:name ; sh:minCount 1 ] .";
    private static final String MODEL_TTL = "@prefix ex: <http://example.org/> . ex:alice a ex:Person ; ex:name \"Alice\" .";

    private static String jsonLdWithContext(String contextReference) {
        return "{\"@context\":\"" + contextReference + "\",\"@id\":\"http://example.org/alice\",\"@type\":\"Person\",\"name\":\"Alice\"}";
    }

    private static final String CONTEXT_BODY = "{\"@context\":{\"@vocab\":\"http://example.org/\"}}";

    private List<AnyContent> shaclInputs(String model, String modelType, String shapes, String shapesType, boolean loadImports) {
        List<AnyContent> inputs = inputs("model", model, "modelType", modelType, "shapes", shapes, "shapesType", shapesType);
        inputs.add(typedInput("loadImports", "boolean", String.valueOf(loadImports)));
        return inputs;
    }

    @Test
    void shaclStillValidates() throws Exception {
        run("sec-shacl", null, shaclInputs(MODEL_TTL, "text/turtle", SHAPES_TTL, "text/turtle", false)).assertSuccess();
    }

    @Test
    void shaclJsonLdContextCannotBeLoadedFromLocalFile() throws Exception {
        String uri = secretFileUri("context.jsonld", CONTEXT_BODY);
        assertBlocked(run("sec-shacl", null, shaclInputs(jsonLdWithContext(uri), "application/ld+json", SHAPES_TTL, "text/turtle", false)), "only http and https");
    }

    @Test
    void shaclJsonLdContextCannotBeLoadedFromInternalAddress() throws Exception {
        String path = serve("application/ld+json", CONTEXT_BODY);
        assertBlocked(run("sec-shacl", null, shaclInputs(jsonLdWithContext(base() + path), "application/ld+json", SHAPES_TTL, "text/turtle", false)), "internal or reserved address");
        assertEquals(0, requestCount(path));
    }

    @Test
    void shaclJsonLdContextCanBeLoadedFromWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String path = serve("application/ld+json", CONTEXT_BODY);
        run("sec-shacl", null, shaclInputs(jsonLdWithContext(base() + path), "application/ld+json", SHAPES_TTL, "text/turtle", false)).assertSuccess();
        assertEquals(1, requestCount(path));
    }

    @Test
    void shaclJsonLdContextCannotRedirectToNonWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String target = serveOutsideWhitelist("application/ld+json", CONTEXT_BODY);
        String path = serveRedirect(base() + target);
        assertBlocked(run("sec-shacl", null, shaclInputs(jsonLdWithContext(base() + path), "application/ld+json", SHAPES_TTL, "text/turtle", false)), "internal or reserved address");
        assertEquals(0, requestCount(target));
    }

    @Test
    void shaclShapesJsonLdContextCannotBeLoadedFromLocalFile() throws Exception {
        String uri = secretFileUri("shapes-context.jsonld", CONTEXT_BODY);
        String shapes = "{\"@context\":\"" + uri + "\",\"@id\":\"http://example.org/Shape\",\"@type\":\"http://www.w3.org/ns/shacl#NodeShape\"}";
        assertBlocked(run("sec-shacl", null, shaclInputs(MODEL_TTL, "text/turtle", shapes, "application/ld+json", false)), "only http and https");
    }

    @Test
    void shaclImportsCannotBeLoadedFromLocalFile() throws Exception {
        String uri = secretFileUri("ontology.ttl", "@prefix ex: <http://example.org/> . ex:imported ex:p ex:o .");
        String model = "@prefix owl: <http://www.w3.org/2002/07/owl#> . <http://example.org/onto> a owl:Ontology ; owl:imports <" + uri + "> .";
        assertBlocked(run("sec-shacl", null, shaclInputs(model, "text/turtle", SHAPES_TTL, "text/turtle", true)), "only http and https");
    }

    @Test
    void shaclImportsCannotBeLoadedFromInternalAddress() throws Exception {
        String path = serve("text/turtle", "@prefix ex: <http://example.org/> . ex:imported ex:p ex:o .");
        String model = "@prefix owl: <http://www.w3.org/2002/07/owl#> . <http://example.org/onto> a owl:Ontology ; owl:imports <" + base() + path + "> .";
        assertBlocked(run("sec-shacl", null, shaclInputs(model, "text/turtle", SHAPES_TTL, "text/turtle", true)), "internal or reserved address");
        assertEquals(0, requestCount(path));
    }

    @Test
    void shaclImportsAreLoadedFromWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        // Imports that cannot be read are ignored, so we make the validation outcome depend on the imported content (a person without a name).
        String path = serve("text/turtle", "@prefix ex: <http://example.org/> . ex:bob a ex:Person .");
        String model = "@prefix owl: <http://www.w3.org/2002/07/owl#> . <http://example.org/onto> a owl:Ontology ; owl:imports <" + base() + path + "> .";
        TestRunResult result = run("sec-shacl", null, shaclInputs(model, "text/turtle", SHAPES_TTL, "text/turtle", true));
        result.assertFailed();
        assertEquals(1, requestCount(path));
        // Without loading imports the (unused) import has no effect.
        String otherPath = serve("text/turtle", "@prefix ex: <http://example.org/> . ex:bob a ex:Person .");
        String otherModel = "@prefix owl: <http://www.w3.org/2002/07/owl#> . <http://example.org/onto> a owl:Ontology ; owl:imports <" + base() + otherPath + "> .";
        run("sec-shacl", null, shaclInputs(otherModel, "text/turtle", SHAPES_TTL, "text/turtle", false)).assertSuccess();
        assertEquals(0, requestCount(otherPath));
    }

    private static String shapesWithService(String endpoint) {
        return "@prefix sh: <http://www.w3.org/ns/shacl#> . @prefix ex: <http://example.org/> . "
                + "ex:PersonShape a sh:NodeShape ; sh:targetClass ex:Person ; sh:sparql [ a sh:SPARQLConstraint ; sh:message \"Failed\" ; "
                + "sh:select \"\"\"SELECT $this WHERE { SERVICE <" + endpoint + "> { ?s ?p ?o } }\"\"\" ] .";
    }

    @Test
    void shaclSparqlConstraintsStillWork() throws Exception {
        String shapes = "@prefix sh: <http://www.w3.org/ns/shacl#> . @prefix ex: <http://example.org/> . "
                + "ex:PersonShape a sh:NodeShape ; sh:targetClass ex:Person ; sh:sparql [ a sh:SPARQLConstraint ; sh:message \"Failed\" ; sh:select \"\"\"SELECT $this WHERE { FILTER(false) }\"\"\" ] .";
        run("sec-shacl", null, shaclInputs(MODEL_TTL, "text/turtle", shapes, "text/turtle", false)).assertSuccess();
    }

    @Test
    void shaclSparqlConstraintCanCallWhitelistedService() throws Exception {
        whitelistExternalResources();
        String path = serve("application/sparql-results+json", "{\"head\":{\"vars\":[\"s\",\"p\",\"o\"]},\"results\":{\"bindings\":[]}}");
        run("sec-shacl", null, shaclInputs(MODEL_TTL, "text/turtle", shapesWithService(base() + path), "text/turtle", false));
        assertTrue(requestCount(path) >= 1, "The whitelisted service should have been called");
    }

    @Test
    void shaclSparqlConstraintCannotCallInternalService() throws Exception {
        String path = serve("application/sparql-results+json", "{\"head\":{\"vars\":[\"s\",\"p\",\"o\"]},\"results\":{\"bindings\":[]}}");
        TestRunResult result = run("sec-shacl", null, shaclInputs(MODEL_TTL, "text/turtle", shapesWithService(base() + path), "text/turtle", false));
        result.assertFailed();
        assertEquals(0, requestCount(path), "The service must not be called:\n" + logs(result));
    }

    @Test
    void rdfProcessorStillConverts() throws Exception {
        run("sec-rdf-convert", null, inputs("model", MODEL_TTL, "modelType", "text/turtle")).assertSuccess();
    }

    @Test
    void rdfProcessorJsonLdContextCannotBeLoadedFromLocalFile() throws Exception {
        String uri = secretFileUri("context.jsonld", CONTEXT_BODY);
        assertBlocked(run("sec-rdf-convert", null, inputs("model", jsonLdWithContext(uri), "modelType", "application/ld+json")), "only http and https");
    }

    @Test
    void rdfProcessorJsonLdContextCannotBeLoadedFromInternalAddress() throws Exception {
        String path = serve("application/ld+json", CONTEXT_BODY);
        assertBlocked(run("sec-rdf-convert", null, inputs("model", jsonLdWithContext(base() + path), "modelType", "application/ld+json")), "internal or reserved address");
        assertEquals(0, requestCount(path));
    }

    @Test
    void rdfProcessorJsonLdContextCanBeLoadedFromWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String path = serve("application/ld+json", CONTEXT_BODY);
        run("sec-rdf-convert", null, inputs("model", jsonLdWithContext(base() + path), "modelType", "application/ld+json")).assertSuccess();
        assertEquals(1, requestCount(path));
    }

    @Test
    void rdfProcessorQueryCannotCallInternalService() throws Exception {
        String path = serve("application/sparql-results+json", "{\"head\":{\"vars\":[\"s\"]},\"results\":{\"bindings\":[]}}");
        TestRunResult result = run("sec-rdf-ask", null, inputs("query", "ASK { SERVICE <" + base() + path + "> { ?s ?p ?o } }"));
        result.assertFailed();
        assertEquals(0, requestCount(path), "The service must not be called:\n" + logs(result));
    }

    @Test
    void rdfProcessorQueryStillWorks() throws Exception {
        run("sec-rdf-ask", null, inputs("query", "ASK { ?s ?p ?o }")).assertSuccess();
    }

    private static final String MODEL_JSONLD = "{\"@context\":{\"ex\":\"http://example.org/\"},\"@id\":\"ex:alice\",\"@type\":\"ex:Person\"}";

    @Test
    void rdfProcessorRejectsBlacklistedInputContentType() throws Exception {
        TestEngineConfiguration.RDF_CONTENT_RESTRICTIONS = RdfContentRestrictions.of(null, "application/ld+json");
        assertBlocked(run("sec-rdf-convert", null, inputs("model", MODEL_JSONLD, "modelType", "application/ld+json")), "is not allowed by the test engine's configuration");
        // Equivalent content types are covered.
        assertBlocked(run("sec-rdf-convert", null, inputs("model", MODEL_JSONLD, "modelType", "application/json")), "is not allowed by the test engine's configuration");
        // Positive control.
        run("sec-rdf-convert", null, inputs("model", MODEL_TTL, "modelType", "text/turtle")).assertSuccess();
    }

    @Test
    void rdfProcessorRejectsContentTypeOutsideWhitelist() throws Exception {
        TestEngineConfiguration.RDF_CONTENT_RESTRICTIONS = RdfContentRestrictions.of("text/turtle", null);
        assertBlocked(run("sec-rdf-convert", null, inputs("model", MODEL_JSONLD, "modelType", "application/ld+json")), "is not allowed by the test engine's configuration");
        run("sec-rdf-convert", null, inputs("model", MODEL_TTL, "modelType", "text/turtle")).assertSuccess();
    }

    @Test
    void rdfProcessorOutputContentTypeIsNotRestricted() throws Exception {
        // The (fixed) output content type of the test case is text/turtle, which is blacklisted for parsing.
        TestEngineConfiguration.RDF_CONTENT_RESTRICTIONS = RdfContentRestrictions.of(null, "text/turtle");
        run("sec-rdf-convert", null, inputs("model", MODEL_JSONLD, "modelType", "application/ld+json")).assertSuccess();
    }

    @Test
    void rdfProcessorQueryRejectsBlacklistedModelContentType() throws Exception {
        TestEngineConfiguration.RDF_CONTENT_RESTRICTIONS = RdfContentRestrictions.of(null, "text/turtle");
        assertBlocked(run("sec-rdf-ask", null, inputs("query", "ASK { ?s ?p ?o }")), "is not allowed by the test engine's configuration");
    }

    @Test
    void shaclRejectsBlacklistedModelContentType() throws Exception {
        TestEngineConfiguration.RDF_CONTENT_RESTRICTIONS = RdfContentRestrictions.of(null, "application/ld+json");
        assertBlocked(run("sec-shacl", null, shaclInputs(MODEL_JSONLD, "application/ld+json", SHAPES_TTL, "text/turtle", false)), "is not allowed by the test engine's configuration");
        run("sec-shacl", null, shaclInputs(MODEL_TTL, "text/turtle", SHAPES_TTL, "text/turtle", false)).assertSuccess();
    }

    @Test
    void shaclRejectsBlacklistedShapesContentType() throws Exception {
        TestEngineConfiguration.RDF_CONTENT_RESTRICTIONS = RdfContentRestrictions.of(null, "application/rdf+xml");
        String shapes = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:sh=\"http://www.w3.org/ns/shacl#\"><sh:NodeShape rdf:about=\"http://example.org/Shape\"/></rdf:RDF>";
        assertBlocked(run("sec-shacl", null, shaclInputs(MODEL_TTL, "text/turtle", shapes, "application/rdf+xml", false)), "is not allowed by the test engine's configuration");
    }

    @Test
    void shaclRejectsModelContentTypeWhenThereAreNoShapes() throws Exception {
        TestEngineConfiguration.RDF_CONTENT_RESTRICTIONS = RdfContentRestrictions.of("text/turtle", null);
        assertBlocked(run("sec-shacl", null, shaclInputs(MODEL_JSONLD, "application/ld+json", "", "text/turtle", false)), "is not allowed by the test engine's configuration");
    }

    @Test
    void shaclImportWithContentTypeOutsideWhitelistIsBlocked() throws Exception {
        whitelistExternalResources();
        TestEngineConfiguration.RDF_CONTENT_RESTRICTIONS = RdfContentRestrictions.of("text/turtle", null);
        String path = serve("application/rdf+xml", "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/>");
        String model = "@prefix owl: <http://www.w3.org/2002/07/owl#> . <http://example.org/onto> a owl:Ontology ; owl:imports <" + base() + path + "> .";
        assertBlocked(run("sec-shacl", null, shaclInputs(model, "text/turtle", SHAPES_TTL, "text/turtle", true)), "is not allowed by the test engine's configuration");
    }

    @Test
    void shaclImportWithWhitelistedContentTypeIsLoaded() throws Exception {
        whitelistExternalResources();
        TestEngineConfiguration.RDF_CONTENT_RESTRICTIONS = RdfContentRestrictions.of("text/turtle", null);
        String path = serve("text/turtle", "@prefix ex: <http://example.org/> . ex:bob a ex:Person .");
        String model = "@prefix owl: <http://www.w3.org/2002/07/owl#> . <http://example.org/onto> a owl:Ontology ; owl:imports <" + base() + path + "> .";
        // The imported person has no name, which makes the validation fail.
        run("sec-shacl", null, shaclInputs(model, "text/turtle", SHAPES_TTL, "text/turtle", true)).assertFailed();
        assertEquals(1, requestCount(path));
    }

    // RDF - end.

    // Inputs provided by reference - start.

    private static AnyContent uriInput(String name, String uri) {
        AnyContent input = stringInput(name, uri);
        input.setEmbeddingMethod(ValueEmbeddingEnumeration.URI);
        return input;
    }

    @Test
    void inputProvidedByReferenceCannotReachInternalAddress() throws Exception {
        String path = serve("text/plain", "retrieved");
        var result = configureOnly("sec-uri-input", List.of(uriInput("data", base() + path)));
        assertNotNull(result.getErrorCode(), "The configuration should have failed");
        assertTrue(String.valueOf(result.getErrorDescription()).contains("internal or reserved address"), result.getErrorDescription());
        assertEquals(0, requestCount(path));
    }

    @Test
    void inputProvidedByReferenceCanReachWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String path = serve("text/plain", "retrieved");
        run("sec-uri-input", null, List.of(uriInput("data", base() + path))).assertSuccess();
        assertEquals(1, requestCount(path));
    }

    @Test
    void inputProvidedByReferenceCannotRedirectToNonWhitelistedAddress() throws Exception {
        whitelistExternalResources();
        String target = serveOutsideWhitelist("text/plain", "retrieved");
        String path = serveRedirect(base() + target);
        var result = configureOnly("sec-uri-input", List.of(uriInput("data", base() + path)));
        assertNotNull(result.getErrorCode(), "The configuration should have failed");
        assertEquals(0, requestCount(target));
    }

    @Test
    void inputProvidedByReferenceCannotReadLocalFile() throws Exception {
        var result = configureOnly("sec-uri-input", List.of(uriInput("data", secretFileUri())));
        assertNotNull(result.getErrorCode(), "The configuration should have failed");
        assertTrue(String.valueOf(result.getErrorDescription()).contains("only http and https"), result.getErrorDescription());
        assertFalse(String.valueOf(result.getErrorDescription()).contains(SECRET));
    }

    // Inputs provided by reference - end.

}
